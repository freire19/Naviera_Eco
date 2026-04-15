package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private static final SecureRandom RANDOM = new SecureRandom();

    public AdminService(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    public List<Map<String, Object>> listarEmpresas() {
        return jdbc.queryForList("SELECT * FROM empresas ORDER BY nome");
    }

    public Map<String, Object> buscarEmpresa(Long id) {
        var list = jdbc.queryForList("SELECT * FROM empresas WHERE id = ?", id);
        if (list.isEmpty()) throw ApiException.notFound("Empresa nao encontrada");
        return list.get(0);
    }

    /**
     * Gera codigo de ativacao unico no formato NAV-XXXX (4 hex).
     * Tenta ate 10 vezes; fallback com 6 hex se colidir muito.
     */
    private String gerarCodigoAtivacaoUnico() {
        for (int tentativa = 0; tentativa < 10; tentativa++) {
            String codigo = "NAV-" + String.format("%04X", RANDOM.nextInt(0xFFFF));
            List<Map<String, Object>> existente = jdbc.queryForList(
                "SELECT 1 FROM empresas WHERE codigo_ativacao = ?", codigo);
            if (existente.isEmpty()) return codigo;
        }
        // Fallback com mais entropia se 4 hex colidir muito
        return "NAV-" + String.format("%06X", RANDOM.nextInt(0xFFFFFF));
    }

    /**
     * Cria empresa + primeiro usuario administrador + codigo de ativacao.
     * Se operador_nome/operador_email nao forem informados, gera empresa sem usuario
     * (para retrocompatibilidade, mas o BFF sempre envia).
     */
    @Transactional
    public Map<String, Object> criarEmpresa(Map<String, Object> dados) {
        String nome = (String) dados.get("nome");
        String slug = (String) dados.get("slug");
        if (nome == null || nome.isBlank()) throw ApiException.badRequest("Nome e obrigatorio");

        String codigo = gerarCodigoAtivacaoUnico();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                INSERT INTO empresas (nome, cnpj, ie, endereco, cep, telefone, email, path_logo, plano, ativo, slug, cor_primaria, codigo_ativacao)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?, ?)""",
                new String[]{"id"});
            ps.setObject(1, nome);
            ps.setObject(2, dados.get("cnpj"));
            ps.setObject(3, dados.get("ie"));
            ps.setObject(4, dados.get("endereco"));
            ps.setObject(5, dados.get("cep"));
            ps.setObject(6, dados.get("telefone"));
            ps.setObject(7, dados.get("email"));
            ps.setObject(8, dados.get("path_logo"));
            ps.setObject(9, dados.getOrDefault("plano", "basico"));
            ps.setObject(10, slug != null ? slug.toLowerCase() : null);
            ps.setObject(11, dados.getOrDefault("cor_primaria", "#059669"));
            ps.setObject(12, codigo);
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) throw new RuntimeException("Falha ao obter ID da empresa criada");
        Long id = keyHolder.getKey().longValue();

        // Criar primeiro usuario se dados do operador foram informados
        String opNome = (String) dados.get("operador_nome");
        String opEmail = (String) dados.get("operador_email");
        String senhaTemp = null;

        if (opNome != null && !opNome.isBlank() && opEmail != null && !opEmail.isBlank()) {
            byte[] bytes = new byte[4];
            RANDOM.nextBytes(bytes);
            senhaTemp = String.format("%02x%02x%02x%02x", bytes[0] & 0xFF, bytes[1] & 0xFF, bytes[2] & 0xFF, bytes[3] & 0xFF);

            jdbc.update("""
                INSERT INTO usuarios (nome, email, senha, funcao, permissao, empresa_id, deve_trocar_senha)
                VALUES (?, ?, ?, 'Administrador', 'ADMIN', ?, TRUE)""",
                opNome, opEmail.toLowerCase(), encoder.encode(senhaTemp), id);
        }

        var result = new java.util.HashMap<String, Object>();
        result.put("id", id);
        result.put("nome", nome);
        result.put("slug", slug);
        result.put("codigo_ativacao", codigo);
        if (senhaTemp != null) {
            result.put("operador", Map.of(
                "nome", opNome,
                "email", opEmail.toLowerCase(),
                "senha_temporaria", senhaTemp
            ));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> atualizarEmpresa(Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE empresas SET nome = COALESCE(?, nome), cnpj = COALESCE(?, cnpj),
                ie = COALESCE(?, ie), endereco = COALESCE(?, endereco), cep = COALESCE(?, cep),
                telefone = COALESCE(?, telefone), email = COALESCE(?, email),
                path_logo = COALESCE(?, path_logo), plano = COALESCE(?, plano),
                slug = COALESCE(?, slug), cor_primaria = COALESCE(?, cor_primaria)
            WHERE id = ?""",
            dados.get("nome"), dados.get("cnpj"), dados.get("ie"), dados.get("endereco"),
            dados.get("cep"), dados.get("telefone"), dados.get("email"),
            dados.get("path_logo"), dados.get("plano"),
            dados.get("slug"), dados.get("cor_primaria"), id);
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
