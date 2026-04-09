package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.AmigoDTO;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AmigoService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;

    public AmigoService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo) {
        this.jdbc = jdbc;
        this.clienteRepo = clienteRepo;
    }

    private AmigoDTO mapRow(java.sql.ResultSet rs, boolean hasAmizade) throws java.sql.SQLException {
        return new AmigoDTO(
            hasAmizade ? rs.getLong("id") : null, rs.getLong("id_amigo"),
            rs.getString("nome"), rs.getString("cidade"), rs.getString("foto_url"),
            hasAmizade ? rs.getString("status") : null,
            hasAmizade ? rs.getString("data_solicitacao") : null
        );
    }

    public List<AmigoDTO> listar(Long clienteId) {
        String sql = """
            SELECT a.id, a.id_amigo, c.nome, c.cidade, c.foto_url, a.status,
                   a.data_solicitacao::text as data_solicitacao
            FROM amigos_app a
            JOIN clientes_app c ON a.id_amigo = c.id
            WHERE a.id_cliente = ? AND a.status = 'ACEITO'
            UNION ALL
            SELECT a.id, a.id_cliente as id_amigo, c.nome, c.cidade, c.foto_url, a.status,
                   a.data_solicitacao::text as data_solicitacao
            FROM amigos_app a
            JOIN clientes_app c ON a.id_cliente = c.id
            WHERE a.id_amigo = ? AND a.status = 'ACEITO'
            ORDER BY nome
            """;
        return jdbc.query(sql, (rs, i) -> mapRow(rs, true), clienteId, clienteId);
    }

    public List<AmigoDTO> pendentes(Long clienteId) {
        String sql = """
            SELECT a.id, a.id_cliente as id_amigo, c.nome, c.cidade, c.foto_url, a.status,
                   a.data_solicitacao::text as data_solicitacao
            FROM amigos_app a
            JOIN clientes_app c ON a.id_cliente = c.id
            WHERE a.id_amigo = ? AND a.status = 'PENDENTE'
            ORDER BY a.data_solicitacao DESC
            """;
        return jdbc.query(sql, (rs, i) -> mapRow(rs, true), clienteId);
    }

    public List<AmigoDTO> buscarPorNome(Long clienteId, String nome) {
        String sql = """
            SELECT c.id as id_amigo, c.nome, c.cidade, c.foto_url
            FROM clientes_app c
            WHERE c.id != ? AND c.ativo = true
              AND UPPER(c.nome) LIKE UPPER(?)
              AND c.id NOT IN (
                SELECT id_amigo FROM amigos_app WHERE id_cliente = ?
                UNION SELECT id_cliente FROM amigos_app WHERE id_amigo = ?
              )
            ORDER BY c.nome LIMIT 20
            """;
        return jdbc.query(sql, (rs, i) -> mapRow(rs, false), clienteId, "%" + nome + "%", clienteId, clienteId);
    }

    public List<AmigoDTO> sugestoes(Long clienteId) {
        var cliente = clienteRepo.findById(clienteId).orElse(null);
        String cidade = cliente != null && cliente.getCidade() != null ? cliente.getCidade() : "";
        String sql = """
            SELECT c.id as id_amigo, c.nome, c.cidade, c.foto_url
            FROM clientes_app c
            WHERE c.id != ? AND c.ativo = true
              AND c.id NOT IN (
                SELECT id_amigo FROM amigos_app WHERE id_cliente = ?
                UNION SELECT id_cliente FROM amigos_app WHERE id_amigo = ?
              )
            ORDER BY CASE WHEN UPPER(c.cidade) = UPPER(?) THEN 0 ELSE 1 END, c.nome
            LIMIT 10
            """;
        return jdbc.query(sql, (rs, i) -> mapRow(rs, false), clienteId, clienteId, clienteId, cidade);
    }

    @Transactional
    public void enviarConvitePorId(Long clienteId, Long amigoId) {
        if (amigoId.equals(clienteId))
            throw ApiException.badRequest("Voce nao pode adicionar a si mesmo");

        clienteRepo.findById(amigoId)
            .orElseThrow(() -> ApiException.notFound("Usuario nao encontrado"));

        Integer exists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM amigos_app WHERE (id_cliente = ? AND id_amigo = ?) OR (id_cliente = ? AND id_amigo = ?)",
            Integer.class, clienteId, amigoId, amigoId, clienteId);

        if (exists != null && exists > 0)
            throw ApiException.conflict("Ja existe uma solicitacao com este usuario");

        jdbc.update("INSERT INTO amigos_app (id_cliente, id_amigo, status) VALUES (?, ?, 'PENDENTE')",
            clienteId, amigoId);
    }

    @Transactional
    public void aceitar(Long clienteId, Long amizadeId) {
        int updated = jdbc.update(
            "UPDATE amigos_app SET status = 'ACEITO', data_aceite = NOW() WHERE id = ? AND id_amigo = ? AND status = 'PENDENTE'",
            amizadeId, clienteId);
        if (updated == 0)
            throw ApiException.notFound("Solicitacao nao encontrada ou ja processada");
    }

    @Transactional
    public void remover(Long clienteId, Long amizadeId) {
        int deleted = jdbc.update(
            "DELETE FROM amigos_app WHERE id = ? AND (id_cliente = ? OR id_amigo = ?)",
            amizadeId, clienteId, clienteId);
        if (deleted == 0)
            throw ApiException.notFound("Amizade nao encontrada");
    }
}
