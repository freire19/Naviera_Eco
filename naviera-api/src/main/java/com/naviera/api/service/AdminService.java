package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final JdbcTemplate jdbc;

    public AdminService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listarEmpresas() {
        return jdbc.queryForList("SELECT * FROM empresas ORDER BY nome");
    }

    public Map<String, Object> buscarEmpresa(Long id) {
        var list = jdbc.queryForList("SELECT * FROM empresas WHERE id = ?", id);
        if (list.isEmpty()) throw ApiException.notFound("Empresa nao encontrada");
        return list.get(0);
    }

    @Transactional
    public Map<String, Object> criarEmpresa(Map<String, Object> dados) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                INSERT INTO empresas (nome, cnpj, ie, endereco, cep, telefone, email, path_logo, plano, ativo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)""",
                new String[]{"id"});
            ps.setObject(1, dados.get("nome"));
            ps.setObject(2, dados.get("cnpj"));
            ps.setObject(3, dados.get("ie"));
            ps.setObject(4, dados.get("endereco"));
            ps.setObject(5, dados.get("cep"));
            ps.setObject(6, dados.get("telefone"));
            ps.setObject(7, dados.get("email"));
            ps.setObject(8, dados.get("path_logo"));
            ps.setObject(9, dados.getOrDefault("plano", "basico"));
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        return Map.of("mensagem", "Empresa criada", "id", id);
    }

    @Transactional
    public Map<String, Object> atualizarEmpresa(Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE empresas SET nome = ?, cnpj = ?, ie = ?, endereco = ?, cep = ?,
                telefone = ?, email = ?, path_logo = ?, plano = ?
            WHERE id = ?""",
            dados.get("nome"), dados.get("cnpj"), dados.get("ie"), dados.get("endereco"),
            dados.get("cep"), dados.get("telefone"), dados.get("email"),
            dados.get("path_logo"), dados.get("plano"), id);
        if (rows == 0) throw ApiException.notFound("Empresa nao encontrada");
        return Map.of("mensagem", "Empresa atualizada");
    }

    @Transactional
    public Map<String, Object> ativarEmpresa(Long id, boolean ativo) {
        int rows = jdbc.update("UPDATE empresas SET ativo = ? WHERE id = ?", ativo, id);
        if (rows == 0) throw ApiException.notFound("Empresa nao encontrada");
        return Map.of("mensagem", ativo ? "Empresa ativada" : "Empresa desativada");
    }
}
