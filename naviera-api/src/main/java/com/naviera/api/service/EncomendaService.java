package com.naviera.api.service;

import com.naviera.api.dto.EncomendaDTO;
import com.naviera.api.model.ClienteApp;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Lê encomendas diretamente das tabelas do sistema desktop.
 * Busca por nome do destinatário vinculado ao CPF do cliente logado.
 * Quando o campo cpf_destinatario existir no banco, usará esse campo.
 */
@Service
public class EncomendaService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;

    public EncomendaService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo) {
        this.jdbc = jdbc; this.clienteRepo = clienteRepo;
    }

    public List<EncomendaDTO> buscarPorCliente(Long clienteId) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        String sql = """
            SELECT e.id, e.numero_encomenda, e.remetente, e.destinatario, e.rota,
                   COALESCE(emb.nome, '') as embarcacao,
                   e.total_a_pagar, e.valor_pago, e.desconto,
                   e.status_pagamento, e.entregue, e.total_volumes,
                   v.data_viagem, v.data_chegada
            FROM encomendas e
            LEFT JOIN viagens v ON e.id_viagem = v.id
            LEFT JOIN embarcacoes emb ON v.id_embarcacao = emb.id
            WHERE UPPER(e.destinatario) LIKE UPPER(?)
            ORDER BY e.id DESC
            """;

        return jdbc.query(sql, (rs, i) -> new EncomendaDTO(
            rs.getLong("id"),
            rs.getString("numero_encomenda"),
            rs.getString("remetente"),
            rs.getString("destinatario"),
            rs.getString("rota"),
            rs.getString("embarcacao"),
            rs.getBigDecimal("total_a_pagar"),
            rs.getBigDecimal("valor_pago"),
            rs.getBigDecimal("total_a_pagar")
                .subtract(rs.getBigDecimal("desconto") != null ? rs.getBigDecimal("desconto") : java.math.BigDecimal.ZERO)
                .subtract(rs.getBigDecimal("valor_pago") != null ? rs.getBigDecimal("valor_pago") : java.math.BigDecimal.ZERO)
                .max(java.math.BigDecimal.ZERO),
            rs.getString("status_pagamento"),
            rs.getBoolean("entregue"),
            rs.getInt("total_volumes"),
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null,
            rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toString() : null
        ), "%" + cliente.getNome() + "%");
    }
}
