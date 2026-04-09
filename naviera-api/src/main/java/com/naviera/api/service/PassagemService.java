package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.CompraPassagemRequest;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PassagemService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;

    public PassagemService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo) {
        this.jdbc = jdbc;
        this.clienteRepo = clienteRepo;
    }

    public List<Map<String, Object>> minhasPassagens(Long clienteId) {
        var cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> ApiException.notFound("Cliente nao encontrado"));
        String sql = """
            SELECT p.id_passagem, p.numero_bilhete, p.data_emissao,
                   p.valor_a_pagar, p.status_passagem,
                   emb.nome as embarcacao, r.origem, r.destino,
                   v.data_viagem, v.data_chegada,
                   tp.nome_tipo_passagem as tipo,
                   ac.nome_acomodacao as acomodacao
            FROM passagens p
            JOIN viagens v ON p.id_viagem = v.id_viagem
            JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            LEFT JOIN rotas r ON p.id_rota = r.id
            LEFT JOIN aux_tipos_passagem tp ON p.id_tipo_passagem = tp.id_tipo_passagem
            LEFT JOIN aux_acomodacoes ac ON p.id_acomodacao = ac.id_acomodacao
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

        // Verificar viagem existe e é futura
        var viagem = jdbc.queryForList(
            "SELECT v.id_viagem, v.id_rota, v.id_embarcacao FROM viagens v WHERE v.id_viagem = ? AND v.ativa = true AND v.data_viagem >= CURRENT_DATE",
            req.idViagem());
        if (viagem.isEmpty()) throw ApiException.badRequest("Viagem nao disponivel para compra");

        Long idRota = (Long) viagem.get(0).get("id_rota");

        // Buscar tarifa
        var tarifas = jdbc.queryForList(
            "SELECT valor_transporte, valor_alimentacao, valor_desconto FROM tarifas WHERE id_rota = ? AND id_tipo_passagem = ?",
            idRota, req.idTipoPassagem());
        if (tarifas.isEmpty()) throw ApiException.badRequest("Tarifa nao encontrada para este tipo de passagem");

        var tarifa = tarifas.get(0);
        var transporte = (java.math.BigDecimal) tarifa.get("valor_transporte");
        var alimentacao = (java.math.BigDecimal) tarifa.get("valor_alimentacao");
        var desconto = (java.math.BigDecimal) tarifa.get("valor_desconto");
        var total = transporte.add(alimentacao).subtract(desconto);

        // Criar ou buscar passageiro
        var passageiros = jdbc.queryForList(
            "SELECT id_passageiro FROM passageiros WHERE numero_documento = ?", cliente.getDocumento());
        Long idPassageiro;
        if (passageiros.isEmpty()) {
            jdbc.update("INSERT INTO passageiros (nome_passageiro, numero_documento) VALUES (?, ?)",
                cliente.getNome(), cliente.getDocumento());
            idPassageiro = jdbc.queryForObject("SELECT id_passageiro FROM passageiros WHERE numero_documento = ?",
                Long.class, cliente.getDocumento());
        } else {
            idPassageiro = (Long) passageiros.get(0).get("id_passageiro");
        }

        // Gerar numero bilhete
        String numBilhete = "APP-" + String.format("%06d", System.currentTimeMillis() % 1000000);

        // Inserir passagem
        String status = "PIX".equals(req.formaPagamento()) ? "CONFIRMADA" : "PENDENTE";
        var valorPago = "PIX".equals(req.formaPagamento()) ? total : java.math.BigDecimal.ZERO;
        jdbc.update("""
            INSERT INTO passagens (numero_bilhete, id_passageiro, id_viagem, data_emissao,
                id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao,
                valor_desconto_tarifa, valor_total, valor_a_pagar, valor_pago,
                status_passagem, observacoes)
            VALUES (?, ?, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Compra via App Naviera')
            """,
            numBilhete, idPassageiro, req.idViagem(), idRota, req.idTipoPassagem(),
            transporte, alimentacao, desconto, total, total, valorPago, status);

        return Map.of(
            "numeroBilhete", numBilhete,
            "valorTotal", total,
            "status", "PIX".equals(req.formaPagamento()) ? "CONFIRMADA" : "PENDENTE",
            "mensagem", "Passagem reservada com sucesso!"
        );
    }
}
