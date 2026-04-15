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

    // #DB144: empresaId parameter prevents cross-tenant LIKE scan
    public List<FreteDTO> buscarPorRemetente(Long clienteId, Integer empresaId) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        String sql = """
            SELECT f.id_frete, f.numero_frete, f.remetente_nome_temp as nome_remetente,
                   f.destinatario_nome_temp as nome_destinatario,
                   f.rota_temp as nome_rota, COALESCE(emb.nome, '') as embarcacao,
                   f.valor_frete_calculado as valor_nominal, f.valor_pago, f.valor_devedor,
                   f.status_frete as status, f.data_saida_viagem as data_viagem
            FROM fretes f
            LEFT JOIN viagens v ON f.id_viagem = v.id_viagem
            LEFT JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            WHERE UPPER(f.remetente_nome_temp) LIKE UPPER(?) AND f.empresa_id = ?
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
            0,
            null,
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null
        ), "%" + cliente.getNome() + "%", empresaId);
    }
}
