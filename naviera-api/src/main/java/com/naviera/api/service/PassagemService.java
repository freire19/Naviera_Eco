package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.CompraPassagemRequest;
import com.naviera.api.psp.AsaasProperties;
import com.naviera.api.psp.CobrancaRequest;
import com.naviera.api.psp.PspCobranca;
import com.naviera.api.psp.PspCobrancaService;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PassagemService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;
    private final PspCobrancaService pspService;
    private final AsaasProperties pspProps;
    private final TransactionTemplate tx;

    public PassagemService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo,
                           PspCobrancaService pspService, AsaasProperties pspProps,
                           TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.clienteRepo = clienteRepo;
        this.pspService = pspService;
        this.pspProps = pspProps;
        this.tx = tx;
    }

    // DS4-024: cross-tenant intencional — CPF pode ter passagens em multiplas empresas
    // TODO DM069: Replace List<Map<String, Object>> with typed DTO (e.g. PassagemResumoDTO)
    //   Fields: idPassagem, numeroBilhete, dataEmissao, valorAPagar, valorTotal, statusPassagem,
    //   nomePassageiro, numeroDocumento, embarcacao, origem, destino, dataViagem, tipo, acomodacao, horarioSaida
    public List<Map<String, Object>> minhasPassagens(Long clienteId) {
        var cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> ApiException.notFound("Cliente nao encontrado"));
        String sql = """
            SELECT p.id_passagem, p.numero_bilhete, p.data_emissao,
                   p.valor_a_pagar, p.valor_total, p.status_passagem,
                   p.id_viagem,
                   pas.nome_passageiro, pas.numero_documento,
                   emb.nome as embarcacao, r.origem, r.destino,
                   v.data_viagem, v.data_chegada,
                   tp.nome_tipo_passagem as tipo,
                   COALESCE(ac.nome_acomodacao, tp.nome_tipo_passagem) as acomodacao,
                   COALESCE(hs.descricao_horario_saida, '12:00') as horario_saida
            FROM passagens p
            JOIN viagens v ON p.id_viagem = v.id_viagem
            JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            LEFT JOIN rotas r ON p.id_rota = r.id
            LEFT JOIN aux_tipos_passagem tp ON p.id_tipo_passagem = tp.id_tipo_passagem
            LEFT JOIN aux_acomodacoes ac ON p.id_acomodacao = ac.id_acomodacao
            LEFT JOIN aux_horarios_saida hs ON v.id_horario_saida = hs.id_horario_saida
            JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
            WHERE pas.numero_documento = ?
            ORDER BY v.data_viagem DESC
            """;
        return jdbc.queryForList(sql, cliente.getDocumento());
    }

    // #205: chamada HTTP ao PSP nunca dentro de @Transactional — trava conexao DB durante a requisicao.
    //   Fluxo: TX1 (valida + INSERT passagem) -> HTTP PSP sem tx -> TX2 (UPDATE dados PSP).
    //   PspCobrancaService.criar e idempotente; retry do cliente reaproveita a cobranca existente.
    // TODO DM069: Return typed DTO (e.g. CompraPassagemResponse) instead of Map<String, Object>
    public Map<String, Object> comprar(Long clienteId, CompraPassagemRequest req) {
        var cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> ApiException.notFound("Cliente nao encontrado"));

        String forma = com.naviera.api.config.MoneyUtils.validarFormaPagamento(req.formaPagamento());

        // #212: idTipoPassagem obrigatorio — sem ele nao da pra escolher tarifa.
        if (req.idTipoPassagem() == null) throw ApiException.badRequest("idTipoPassagem obrigatorio");

        Map<String, Object> resp = tx.execute(status -> {
            // #657: FOR UPDATE trava a viagem enquanto calcula/insere — evita race com desativacao.
            var viagem = jdbc.queryForList(
                "SELECT v.id_viagem, v.id_rota, v.id_embarcacao, v.empresa_id FROM viagens v WHERE v.id_viagem = ? AND v.ativa = true AND v.data_viagem >= CURRENT_DATE FOR UPDATE",
                req.idViagem());
            if (viagem.isEmpty()) throw ApiException.badRequest("Viagem nao disponivel para compra");

            Integer empresaId = ((Number) viagem.get(0).get("empresa_id")).intValue();
            Long idRota = (Long) viagem.get(0).get("id_rota");

            // #DB206: numero_bilhete gerado com advisory lock + MAX+1 por empresa
            //   (timestamp%1M cicla em 16min e colide sob concorrencia).
            jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> null, empresaId);
            Integer proximo = jdbc.queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_bilhete FROM 'APP-([0-9]+)') AS INTEGER)), 0) + 1 FROM passagens WHERE empresa_id = ? AND numero_bilhete LIKE 'APP-%'",
                Integer.class, empresaId);
            String numBilhete = "APP-" + String.format("%06d", proximo != null ? proximo : 1);

            // #711: cargas fazem parte do total (Desktop e BilheteService ja somam) — alinhar.
            var tarifas = jdbc.queryForList(
                "SELECT valor_transporte, valor_alimentacao, valor_cargas, valor_desconto FROM tarifas WHERE id_rota = ? AND id_tipo_passagem = ? AND empresa_id = ?",
                idRota, req.idTipoPassagem(), empresaId);
            if (tarifas.isEmpty()) throw ApiException.badRequest("Tarifa nao encontrada para este tipo de passagem");

            var tarifa = tarifas.get(0);
            var transporte = com.naviera.api.config.MoneyUtils.toBigDecimal(tarifa.get("valor_transporte"));
            var alimentacao = com.naviera.api.config.MoneyUtils.toBigDecimal(tarifa.get("valor_alimentacao"));
            var cargas = com.naviera.api.config.MoneyUtils.toBigDecimal(tarifa.get("valor_cargas"));
            var desconto = com.naviera.api.config.MoneyUtils.toBigDecimal(tarifa.get("valor_desconto"));
            var total = transporte.add(alimentacao).add(cargas).subtract(desconto);

            var passageiros = jdbc.queryForList(
                "SELECT id_passageiro FROM passageiros WHERE numero_documento = ? AND empresa_id = ?", cliente.getDocumento(), empresaId);
            Long idPassageiro;
            if (passageiros.isEmpty()) {
                // #DB221: usar INSERT RETURNING (atomico) em vez de INSERT + SELECT separado
                idPassageiro = jdbc.queryForObject(
                    "INSERT INTO passageiros (nome_passageiro, numero_documento, empresa_id) VALUES (?, ?, ?) RETURNING id_passageiro",
                    Long.class, cliente.getNome(), cliente.getDocumento(), empresaId);
                if (idPassageiro == null) throw ApiException.badGateway("Falha ao criar passageiro");
            } else {
                idPassageiro = ((Number) passageiros.get(0).get("id_passageiro")).longValue();
            }

            BigDecimal descontoApp = "PIX".equals(forma)
                ? total.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            BigDecimal valorAPagar = total.subtract(descontoApp);
            String statusPassagem = "BARCO".equals(forma) ? "PENDENTE" : "PENDENTE_CONFIRMACAO";

            // #DL034b: gravar valor_cargas junto (relatorios fiscais dependem da decomposicao).
            Long idPassagem = jdbc.queryForObject("""
                INSERT INTO passagens (numero_bilhete, id_passageiro, id_viagem, data_emissao,
                    id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao, valor_cargas,
                    valor_desconto_tarifa, valor_total, valor_a_pagar, valor_pago,
                    status_passagem, origem_emissao, id_cliente_app, observacoes, empresa_id,
                    forma_pagamento_app, desconto_app)
                VALUES (?, ?, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, 'APP', ?, ?, ?, ?, ?)
                RETURNING id_passagem
                """, Long.class,
                numBilhete, idPassageiro, req.idViagem(), idRota, req.idTipoPassagem(),
                transporte, alimentacao, cargas, desconto, total, valorAPagar, statusPassagem,
                clienteId, "Compra via App - " + forma, empresaId, forma, descontoApp);
            if (idPassagem == null) throw ApiException.badGateway("Falha ao criar passagem (RETURNING vazio)");

            String subcontaId = null;
            if (!"BARCO".equals(forma)) {
                subcontaId = (String) jdbc.queryForMap(
                    "SELECT psp_subconta_id FROM empresas WHERE id = ?", empresaId).get("psp_subconta_id");
                if (subcontaId == null || subcontaId.isBlank()) {
                    throw ApiException.badRequest(
                        "Empresa nao possui subconta Asaas. Pagamento online indisponivel — use 'Pagar no barco'.");
                }
            }

            Map<String, Object> r = new java.util.HashMap<>();
            r.put("idPassagem", idPassagem);
            r.put("empresaId", empresaId);
            r.put("subcontaId", subcontaId);
            r.put("numeroBilhete", numBilhete);
            r.put("valorTotal", total);
            r.put("descontoApp", descontoApp);
            r.put("valorAPagar", valorAPagar);
            r.put("formaPagamento", forma);
            r.put("status", statusPassagem);
            return r;
        });

        if ("BARCO".equals(forma)) {
            resp.remove("idPassagem");
            resp.remove("empresaId");
            resp.remove("subcontaId");
            resp.put("mensagem", "Passagem reservada! Pague diretamente no embarque.");
            return resp;
        }

        Long idPassagem = (Long) resp.remove("idPassagem");
        Integer empresaId = (Integer) resp.remove("empresaId");
        String subcontaId = (String) resp.remove("subcontaId");
        BigDecimal valorAPagar = (BigDecimal) resp.get("valorAPagar");
        String numBilhete = (String) resp.get("numeroBilhete");

        // #DB209: vencimento baseado em TZ BR, nao UTC default da JVM
        CobrancaRequest pspReq = new CobrancaRequest(
            empresaId, subcontaId, "PASSAGEM", idPassagem, clienteId, forma,
            valorAPagar, BigDecimal.ZERO, pspProps.getSplitNavieraPct(),
            "Passagem " + numBilhete,
            LocalDate.now(com.naviera.api.config.MoneyUtils.ZONE_BR).plusDays(1),
            cliente.getDocumento(), cliente.getNome(), cliente.getEmail()
        );
        PspCobranca cob = pspService.criar(pspReq);

        tx.executeWithoutResult(s -> jdbc.update(
            "UPDATE passagens SET id_transacao_psp = ?, qr_pix_payload = ? WHERE id_passagem = ?",
            cob.getPspCobrancaId(), cob.getQrCodePayload(), idPassagem));

        resp.put("pspCobrancaId", cob.getPspCobrancaId());
        resp.put("qrCodePayload", cob.getQrCodePayload());
        resp.put("qrCodeImageUrl", cob.getQrCodeImageUrl());
        resp.put("boletoUrl", cob.getBoletoUrl());
        resp.put("linhaDigitavel", cob.getLinhaDigitavel());
        resp.put("checkoutUrl", cob.getCheckoutUrl());
        resp.put("mensagem", "PIX".equals(forma)
            ? "Passagem emitida! Escaneie o QR Code ou copie o codigo para pagar."
            : "Passagem emitida! Conclua o pagamento no checkout.");
        return resp;
    }

    // TODO DM069: Return typed DTO (e.g. EmbarqueConsultaDTO) instead of Map<String, Object>
    //   Fields: idPassagem, numeroBilhete, statusPassagem, origemEmissao, valorAPagar, valorPago,
    //   dataEmissao, nomePassageiro, numeroDocumento, dataNascimento, fotoUrl, email, telefone,
    //   cidade, embarcacao, origem, destino, dataViagem, tipo, acomodacao, dataEmbarque, situacao
    /** Operador escaneia QR — retorna dados completos do passageiro para conferencia */
    public Map<String, Object> consultarParaEmbarque(Integer empresaId, String numeroBilhete) {
        String sql = """
            SELECT p.id_passagem, p.numero_bilhete, p.status_passagem, p.origem_emissao,
                   p.valor_a_pagar, p.valor_pago, p.data_emissao,
                   pas.nome_passageiro, pas.numero_documento, pas.data_nascimento,
                   c.foto_url, c.email, c.telefone, c.cidade,
                   emb.nome as embarcacao, r.origem, r.destino,
                   v.data_viagem, v.data_chegada, v.id_viagem,
                   tp.nome_tipo_passagem as tipo,
                   COALESCE(ac.nome_acomodacao, tp.nome_tipo_passagem) as acomodacao,
                   p.data_embarque,
                   CASE WHEN v.data_chegada < CURRENT_DATE THEN 'EXPIRADA'
                        WHEN p.status_passagem = 'EMBARCADO' THEN 'JA_EMBARCADO'
                        WHEN p.status_passagem = 'CANCELADA' THEN 'CANCELADA'
                        WHEN p.status_passagem = 'PENDENTE_CONFIRMACAO' THEN 'PAGAMENTO_PENDENTE'
                        ELSE 'VALIDA' END as situacao
            FROM passagens p
            JOIN viagens v ON p.id_viagem = v.id_viagem
            JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            LEFT JOIN rotas r ON p.id_rota = r.id
            LEFT JOIN aux_tipos_passagem tp ON p.id_tipo_passagem = tp.id_tipo_passagem
            LEFT JOIN aux_acomodacoes ac ON p.id_acomodacao = ac.id_acomodacao
            JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
            LEFT JOIN clientes_app c ON p.id_cliente_app = c.id
            WHERE p.numero_bilhete = ? AND p.empresa_id = ?
            """;
        var results = jdbc.queryForList(sql, numeroBilhete, empresaId);
        if (results.isEmpty()) throw ApiException.notFound("Bilhete nao encontrado");
        return results.get(0);
    }

    // TODO DM069: Return typed DTO (e.g. EmbarqueConfirmacaoDTO) instead of Map<String, Object>
    //   Fields: mensagem, passageiro, bilhete, embarque
    /** Operador confirma embarque apos conferir documento com foto */
    @Transactional
    public Map<String, Object> confirmarEmbarque(Integer empresaId, String numeroBilhete, String operador) {
        // Verificar situacao
        var dados = consultarParaEmbarque(empresaId, numeroBilhete);
        String situacao = (String) dados.get("situacao");

        if ("EXPIRADA".equals(situacao)) throw ApiException.badRequest("Passagem expirada — viagem ja encerrada");
        if ("JA_EMBARCADO".equals(situacao)) throw ApiException.conflict("Passageiro ja embarcou em " + dados.get("data_embarque"));
        if ("CANCELADA".equals(situacao)) throw ApiException.badRequest("Passagem cancelada");
        if ("PAGAMENTO_PENDENTE".equals(situacao)) throw ApiException.badRequest("Pagamento pendente de confirmacao");

        // #225: status PENDENTE = "pagar no barco" — operador DEVE coletar antes do embarque.
        if ("PENDENTE".equals(dados.get("status_passagem"))) {
            BigDecimal valorAPagar = com.naviera.api.config.MoneyUtils.toBigDecimal(dados.get("valor_a_pagar"));
            BigDecimal valorPago = com.naviera.api.config.MoneyUtils.toBigDecimal(dados.get("valor_pago"));
            if (valorPago.compareTo(valorAPagar) < 0) {
                throw ApiException.badRequest("Passagem PENDENTE — colete o pagamento antes do embarque");
            }
        }

        jdbc.update("UPDATE passagens SET status_passagem = 'EMBARCADO', data_embarque = NOW(), conferido_por = ? WHERE numero_bilhete = ? AND empresa_id = ?",
            operador, numeroBilhete, empresaId);

        return Map.of(
            "mensagem", "Embarque confirmado",
            "passageiro", dados.get("nome_passageiro"),
            "bilhete", numeroBilhete,
            "embarque", "OK"
        );
    }

    // TODO DM069: Return typed DTO (e.g. PagamentoConfirmacaoDTO) instead of Map<String, Object>
    //   Fields: mensagem, bilhete
    /** Operador confirma pagamento de passagem comprada via app */
    @Transactional
    public Map<String, Object> confirmarPagamento(Integer empresaId, String numeroBilhete) {
        int updated = jdbc.update(
            "UPDATE passagens SET status_passagem = 'CONFIRMADA' WHERE numero_bilhete = ? AND status_passagem = 'PENDENTE_CONFIRMACAO' AND empresa_id = ?",
            numeroBilhete, empresaId);
        if (updated == 0) throw ApiException.notFound("Bilhete nao encontrado ou ja confirmado");
        return Map.of("mensagem", "Pagamento confirmado", "bilhete", numeroBilhete);
    }
}
