package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.CompraPassagemRequest;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;

@Service
public class PassagemService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;

    public PassagemService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo) {
        this.jdbc = jdbc;
        this.clienteRepo = clienteRepo;
    }

    // DS4-024: cross-tenant intencional — CPF pode ter passagens em multiplas empresas
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

    @Transactional
    public Map<String, Object> comprar(Long clienteId, CompraPassagemRequest req) {
        var cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> ApiException.notFound("Cliente nao encontrado"));

        // Derivar empresaId da viagem (server-side) — nunca do request
        var viagem = jdbc.queryForList(
            "SELECT v.id_viagem, v.id_rota, v.id_embarcacao, v.empresa_id FROM viagens v WHERE v.id_viagem = ? AND v.ativa = true AND v.data_viagem >= CURRENT_DATE",
            req.idViagem());
        if (viagem.isEmpty()) throw ApiException.badRequest("Viagem nao disponivel para compra");

        Integer empresaId = ((Number) viagem.get(0).get("empresa_id")).intValue();
        Long idRota = (Long) viagem.get(0).get("id_rota");

        // Buscar tarifa, filtrando por empresa_id
        var tarifas = jdbc.queryForList(
            "SELECT valor_transporte, valor_alimentacao, valor_desconto FROM tarifas WHERE id_rota = ? AND id_tipo_passagem = ? AND empresa_id = ?",
            idRota, req.idTipoPassagem(), empresaId);
        if (tarifas.isEmpty()) throw ApiException.badRequest("Tarifa nao encontrada para este tipo de passagem");

        var tarifa = tarifas.get(0);
        var transporte = (BigDecimal) tarifa.get("valor_transporte");
        var alimentacao = (BigDecimal) tarifa.get("valor_alimentacao");
        var desconto = (BigDecimal) tarifa.get("valor_desconto");
        var total = transporte.add(alimentacao).subtract(desconto);

        // Criar ou buscar passageiro, filtrando por empresa_id
        var passageiros = jdbc.queryForList(
            "SELECT id_passageiro FROM passageiros WHERE numero_documento = ? AND empresa_id = ?", cliente.getDocumento(), empresaId);
        Long idPassageiro;
        if (passageiros.isEmpty()) {
            jdbc.update("INSERT INTO passageiros (nome_passageiro, numero_documento, empresa_id) VALUES (?, ?, ?)",
                cliente.getNome(), cliente.getDocumento(), empresaId);
            idPassageiro = jdbc.queryForObject("SELECT id_passageiro FROM passageiros WHERE numero_documento = ? AND empresa_id = ?",
                Long.class, cliente.getDocumento(), empresaId);
        } else {
            idPassageiro = ((Number) passageiros.get(0).get("id_passageiro")).longValue();
        }

        // Gerar numero bilhete
        String numBilhete = "APP-" + String.format("%06d", System.currentTimeMillis() % 1000000);

        // Status: PIX = PENDENTE_CONFIRMACAO (operador confirma depois), outros = PENDENTE
        String status = "PENDENTE_CONFIRMACAO";
        var valorPago = "PIX".equals(req.formaPagamento()) ? total : BigDecimal.ZERO;
        jdbc.update("""
            INSERT INTO passagens (numero_bilhete, id_passageiro, id_viagem, data_emissao,
                id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao,
                valor_desconto_tarifa, valor_total, valor_a_pagar, valor_pago,
                status_passagem, origem_emissao, id_cliente_app, observacoes, empresa_id)
            VALUES (?, ?, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'APP', ?, ?, ?)
            """,
            numBilhete, idPassageiro, req.idViagem(), idRota, req.idTipoPassagem(),
            transporte, alimentacao, desconto, total, total, valorPago, status,
            clienteId, "Compra via App - " + req.formaPagamento(), empresaId);

        return Map.of(
            "numeroBilhete", numBilhete,
            "valorTotal", total,
            "formaPagamento", req.formaPagamento(),
            "status", status,
            "mensagem", "Passagem emitida! Aguardando confirmacao de pagamento pelo operador."
        );
    }

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

        jdbc.update("UPDATE passagens SET status_passagem = 'EMBARCADO', data_embarque = NOW(), conferido_por = ? WHERE numero_bilhete = ? AND empresa_id = ?",
            operador, numeroBilhete, empresaId);

        return Map.of(
            "mensagem", "Embarque confirmado",
            "passageiro", dados.get("nome_passageiro"),
            "bilhete", numeroBilhete,
            "embarque", "OK"
        );
    }

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
