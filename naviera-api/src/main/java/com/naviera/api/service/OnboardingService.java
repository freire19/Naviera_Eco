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
public class OnboardingService {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private static final SecureRandom RANDOM = new SecureRandom();

    public OnboardingService(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    /**
     * Gera codigo de ativacao no formato NAV-XXXXXXXX (8 hex = ~4 bilhoes de possibilidades).
     * Resistente a brute-force — probabilidade de colisao negligenciavel.
     */
    private String gerarCodigoAtivacao() {
        for (int tentativa = 0; tentativa < 10; tentativa++) {
            String codigo = "NAV-" + String.format("%08X", RANDOM.nextInt());
            List<Map<String, Object>> existente = jdbc.queryForList(
                "SELECT 1 FROM empresas WHERE codigo_ativacao = ?", codigo);
            if (existente.isEmpty()) return codigo;
        }
        // Fallback improvavel: usar 10 hex se 8 hex colidir em todas as tentativas
        return "NAV-" + String.format("%010X", (long)(RANDOM.nextDouble() * 0xFFFFFFFFFFL));
    }

    /**
     * Gera slug a partir do nome da empresa.
     * Ex: "Deus de Aliança Navegações" → "deus-de-alianca"
     */
    private String gerarSlug(String nome) {
        String base = nome.toLowerCase()
            .replaceAll("[àáâãä]", "a")
            .replaceAll("[èéêë]", "e")
            .replaceAll("[ìíîï]", "i")
            .replaceAll("[òóôõö]", "o")
            .replaceAll("[ùúûü]", "u")
            .replaceAll("[ç]", "c")
            .replaceAll("[^a-z0-9\\s]", "")
            .trim()
            .replaceAll("\\s+", "-");

        // Limitar a 3 palavras para manter curto
        String[] partes = base.split("-");
        if (partes.length > 3) {
            base = partes[0] + "-" + partes[1] + "-" + partes[2];
        }

        // Garantir unicidade
        String slug = base;
        int contador = 1;
        while (!jdbc.queryForList("SELECT 1 FROM empresas WHERE slug = ?", slug).isEmpty()) {
            slug = base + "-" + contador++;
        }
        return slug;
    }

    /**
     * Registro self-service: operador se cadastra pelo site.
     * Cria empresa + primeiro usuario + codigo de ativacao.
     */
    @Transactional
    public Map<String, Object> registrarEmpresa(Map<String, Object> dados) {
        String nomeEmpresa = str(dados, "nome_empresa");
        String cnpj = str(dados, "cnpj");
        String nomeEmbarcacao = str(dados, "nome_embarcacao");
        String telefone = str(dados, "telefone");
        String email = str(dados, "email");
        String nomeOperador = str(dados, "nome_operador");
        String senha = str(dados, "senha");

        if (nomeEmpresa == null || nomeEmpresa.isBlank())
            throw ApiException.badRequest("Nome da empresa e obrigatorio");
        if (email == null || email.isBlank())
            throw ApiException.badRequest("Email e obrigatorio");
        if (nomeOperador == null || nomeOperador.isBlank())
            throw ApiException.badRequest("Nome do operador e obrigatorio");
        if (senha == null || senha.length() < 6)
            throw ApiException.badRequest("Senha deve ter no minimo 6 caracteres");

        // Verificar email unico
        if (!jdbc.queryForList("SELECT 1 FROM usuarios WHERE LOWER(email) = LOWER(?)", email).isEmpty()) {
            throw ApiException.conflict("Email ja cadastrado");
        }

        String slug = gerarSlug(nomeEmpresa);
        String codigo = gerarCodigoAtivacao();
        String senhaHash = encoder.encode(senha);

        // 1. Criar empresa
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                INSERT INTO empresas (nome, cnpj, telefone, email, slug, codigo_ativacao, plano, ativo)
                VALUES (?, ?, ?, ?, ?, ?, 'basico', TRUE)""",
                new String[]{"id"});
            ps.setString(1, nomeEmpresa);
            ps.setObject(2, cnpj != null && !cnpj.isBlank() ? cnpj : null);
            ps.setObject(3, telefone);
            ps.setObject(4, email.toLowerCase());
            ps.setString(5, slug);
            ps.setString(6, codigo);
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) throw new RuntimeException("Falha ao obter ID da empresa criada");
        Long empresaId = keyHolder.getKey().longValue();

        // 2. Criar primeiro usuario (Administrador da empresa)
        jdbc.update("""
            INSERT INTO usuarios (nome, email, senha, funcao, permissao, empresa_id, deve_trocar_senha)
            VALUES (?, ?, ?, 'Administrador', 'ADMIN', ?, FALSE)""",
            nomeOperador, email.toLowerCase(), senhaHash, empresaId);

        // 3. Salvar nome da embarcacao se informado
        if (nomeEmbarcacao != null && !nomeEmbarcacao.isBlank()) {
            jdbc.update("""
                INSERT INTO embarcacoes (nome, empresa_id) VALUES (?, ?)
                ON CONFLICT DO NOTHING""",
                nomeEmbarcacao, empresaId);
        }

        return Map.of(
            "empresa_id", empresaId,
            "nome", nomeEmpresa,
            "slug", slug,
            "codigo_ativacao", codigo,
            "email", email.toLowerCase()
        );
    }

    /**
     * Ativacao pelo Desktop: valida codigo e retorna dados da empresa.
     * Marca ativado_em na primeira ativacao.
     */
    public Map<String, Object> ativarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw ApiException.badRequest("Codigo de ativacao e obrigatorio");
        }

        String codigoNorm = codigo.trim().toUpperCase();

        List<Map<String, Object>> result = jdbc.queryForList(
            "SELECT id, nome, slug, ativo FROM empresas WHERE codigo_ativacao = ?", codigoNorm);

        if (result.isEmpty()) {
            throw ApiException.notFound("Codigo de ativacao invalido");
        }

        Map<String, Object> empresa = result.get(0);

        if (!Boolean.TRUE.equals(empresa.get("ativo"))) {
            throw ApiException.badRequest("Empresa desativada. Entre em contato com o suporte.");
        }

        Long empresaId = ((Number) empresa.get("id")).longValue();

        // Buscar nome do operador para mostrar na tela final (sem email — reduz PII exposta)
        List<Map<String, Object>> usuarios = jdbc.queryForList(
            "SELECT nome FROM usuarios WHERE empresa_id = ? AND funcao = 'Administrador' AND (excluido IS NOT TRUE) LIMIT 1",
            empresaId);

        String operadorNome = "";
        if (!usuarios.isEmpty()) {
            operadorNome = (String) usuarios.get(0).get("nome");
        }

        // Marcar ativacao (nao impede re-ativacao — pode reinstalar)
        jdbc.update("UPDATE empresas SET ativado_em = NOW() WHERE id = ? AND ativado_em IS NULL", empresaId);

        return Map.of(
            "empresa_id", empresaId,
            "nome", empresa.get("nome"),
            "slug", empresa.get("slug"),
            "operador_nome", operadorNome
        );
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString().trim() : null;
    }
}
