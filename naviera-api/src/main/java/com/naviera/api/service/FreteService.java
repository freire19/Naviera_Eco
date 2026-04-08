package com.naviera.api.service;

import com.naviera.api.dto.FreteDTO;
import com.naviera.api.model.ClienteApp;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Lê fretes das tabelas do sistema desktop.
 * Para CNPJ: busca por nome_remetente vinculado ao nome da empresa.
 */
@Service
public class FreteService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;

    public FreteService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo) {
        this.jdbc = jdbc; this.clienteRepo = clienteRepo;
    }

    public List<FreteDTO> buscarPorRemetente(Long clienteId) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        String sql = """
            SELECT f.id_frete, f.numero_frete, f.nome_remetente, f.nome_destinatario,
                   f.nome_rota, COALESCE(emb.nome, '') as embarcacao,
                   f.valor_nominal, f.valor_pago, f.valor_devedor,
                   f.status, f.total_volumes, f.data_viagem
            FROM fretes f
            LEFT JOIN viagens v ON f.id_viagem = v.id
            LEFT JOIN embarcacoes emb ON v.id_embarcacao = emb.id
            WHERE UPPER(f.nome_remetente) LIKE UPPER(?)
            ORDER BY f.id_frete DESC
            """;

        return jdbc.query(sql, (rs, i) -> new FreteDTO(
            rs.getLong("id_frete"),
            rs.getString("numero_frete"),
            rs.getString("nome_remetente"),
            rs.getString("nome_destinatario"),
            rs.getString("nome_rota"),
            rs.getString("embarcacao"),
            rs.getBigDecimal("valor_nominal"),
            rs.getBigDecimal("valor_pago"),
            rs.getBigDecimal("valor_devedor"),
            rs.getString("status"),
            rs.getInt("total_volumes"),
            null,
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null
        ), "%" + cliente.getNome() + "%");
    }
}
