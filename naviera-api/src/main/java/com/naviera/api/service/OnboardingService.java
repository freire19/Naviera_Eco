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
import java.util.Set;

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
            // #236: evita "NAV-00000000" (1 em 4B com nextInt() == 0) — substituir por 1.
            int rand = RANDOM.nextInt();
            if (rand == 0) rand = 1;
            String codigo = "NAV-" + String.format("%08X", rand);
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
    // #DS5-417: slugs reservados (subdominios da plataforma) que nao podem virar tenant.
    private static final Set<String> SLUGS_RESERVADOS = Set.of(
        "admin", "api", "app", "www", "site", "auth", "ws", "static", "public",
        "naviera", "suporte", "ajuda", "blog", "status", "cdn", "actuator", "health"
    );

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

        // #DP068: 1 query para todos os slugs ja usados (base + numerados) em vez de N queries
        //   no while. Race condition residual nao e bloqueante — INSERT subsequente em
        //   empresas.slug com UNIQUE constraint pegaria o conflito (se existir).
        List<String> existentes = jdbc.queryForList(
            "SELECT slug FROM empresas WHERE slug = ? OR slug LIKE ?",
            String.class, base, base + "-%");
        java.util.Set<String> ocupados = new java.util.HashSet<>(existentes);

        String slug = base;
        int contador = 1;
        while (SLUGS_RESERVADOS.contains(slug) || ocupados.contains(slug)) {
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
        if (nomeEmpresa.length() > 200)
            throw ApiException.badRequest("Nome da empresa muito longo");
        if (email == null || email.isBlank())
            throw ApiException.badRequest("Email e obrigatorio");
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") || email.length() > 200)
            throw ApiException.badRequest("Email invalido");
        if (nomeOperador == null || nomeOperador.isBlank())
            throw ApiException.badRequest("Nome do operador e obrigatorio");
        if (nomeOperador.length() > 200)
            throw ApiException.badRequest("Nome do operador muito longo");
        if (senha == null || senha.length() < 6 || senha.length() > 128)
            throw ApiException.badRequest("Senha deve ter entre 6 e 128 caracteres");
        // #DS5-010: CNPJ opcional, mas se vier deve ser 14 digitos validos (digit-check).
        if (cnpj != null && !cnpj.isBlank()) {
            String soDigitos = cnpj.replaceAll("\\D", "");
            if (soDigitos.length() != 14 || !cnpjValido(soDigitos))
                throw ApiException.badRequest("CNPJ invalido");
            cnpj = soDigitos;
        }

        // Verificar email unico
        if (!jdbc.queryForList("SELECT 1 FROM usuarios WHERE LOWER(email) = LOWER(?)", email).isEmpty()) {
            throw ApiException.conflict("Email ja cadastrado");
        }

        // Slug baseado no nome da embarcacao (subdominio = nome do barco)
        String slug = gerarSlug(nomeEmbarcacao != null && !nomeEmbarcacao.isBlank() ? nomeEmbarcacao : nomeEmpresa);
        String codigo = gerarCodigoAtivacao();
        String senhaHash = encoder.encode(senha);

        // 1. Criar empresa
        final String cnpjFinal = (cnpj != null && !cnpj.isBlank()) ? cnpj : null;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                INSERT INTO empresas (nome, cnpj, telefone, email, slug, codigo_ativacao, plano, ativo)
                VALUES (?, ?, ?, ?, ?, ?, 'basico', TRUE)""",
                new String[]{"id"});
            ps.setString(1, nomeEmpresa);
            ps.setObject(2, cnpjFinal);
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

    // #DS5-010: digit check de CNPJ (mod 11 com pesos 5..2 e 6..2).
    private static boolean cnpjValido(String s) {
        if (s.length() != 14 || s.chars().distinct().count() == 1) return false;
        int[] p1 = {5,4,3,2,9,8,7,6,5,4,3,2};
        int[] p2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};
        return digito(s, p1) == (s.charAt(12) - '0') && digito(s, p2) == (s.charAt(13) - '0');
    }
    private static int digito(String s, int[] p) {
        int sum = 0;
        for (int i = 0; i < p.length; i++) sum += (s.charAt(i) - '0') * p[i];
        int r = sum % 11;
        return r < 2 ? 0 : 11 - r;
    }
}
