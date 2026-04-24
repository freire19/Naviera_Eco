package dao;

// Multi-tenant imports added automatically

import model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
// tenant filter
import static dao.DAOUtils.empresaId;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import util.AppLogger;

public class UsuarioDAO {

    private String hashSenha(String senhaTextoPlano) {
        if (senhaTextoPlano == null || senhaTextoPlano.isEmpty()) {
            return null;
        }
        return BCrypt.hashpw(senhaTextoPlano, BCrypt.gensalt(12));
    }

    private boolean verificarSenha(String senhaTextoPlano, String hashArmazenado) {
        if (senhaTextoPlano == null || hashArmazenado == null || hashArmazenado.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(senhaTextoPlano, hashArmazenado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public int gerarProximoId() {
        String sql = "SELECT nextval('usuarios_id_seq')";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO: " + e.getMessage());
            throw new RuntimeException("Falha ao gerar proximo ID de usuario", e);
        }
        throw new RuntimeException("Sequencia usuarios_id_seq nao retornou valor");
    }

    public boolean inserir(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nome, senha, email, funcao, permissao, excluido, empresa_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNomeCompleto());
            ps.setString(2, hashSenha(usuario.getSenhaPlana()));
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getFuncao());
            ps.setString(5, usuario.getPermissoes());
            ps.setBoolean(6, !usuario.isAtivo());
            ps.setInt(7, empresaId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        usuario.setId(generatedKeys.getInt("id"));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Usuario usuario) {
        boolean atualizarSenha = usuario.getSenhaPlana() != null && !usuario.getSenhaPlana().isEmpty();

        StringBuilder sqlBuilder = new StringBuilder("UPDATE usuarios SET nome=?, email=?, funcao=?, permissao=?, excluido=? ");
        if (atualizarSenha) {
            sqlBuilder.append(", senha=? ");
        }
        sqlBuilder.append("WHERE id=? AND empresa_id=?");
        String sql = sqlBuilder.toString();

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            ps.setString(paramIndex++, usuario.getNomeCompleto());
            ps.setString(paramIndex++, usuario.getEmail());
            ps.setString(paramIndex++, usuario.getFuncao());
            ps.setString(paramIndex++, usuario.getPermissoes());
            ps.setBoolean(paramIndex++, !usuario.isAtivo());

            if (atualizarSenha) {
                ps.setString(paramIndex++, hashSenha(usuario.getSenhaPlana()));
            }
            ps.setInt(paramIndex++, usuario.getId());
            ps.setInt(paramIndex++, empresaId());

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(int idUsuario) {
        String sql = "DELETE FROM usuarios WHERE id=? AND empresa_id=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, empresaId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO: " + e.getMessage());
            return false;
        }
    }

    public Usuario buscarPorId(int idUsuario) {
        String sql = "SELECT id, nome, senha, email, funcao, permissao, excluido FROM usuarios WHERE id = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, empresaId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extrairUsuarioDoResultSet(rs, true);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO: " + e.getMessage());
        }
        return null;
    }

    public Usuario buscarPorLogin(String loginOuEmail) {
        String sql = "SELECT id, nome, senha, email, funcao, permissao, excluido FROM usuarios WHERE (nome = ? OR LOWER(email) = LOWER(?)) AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginOuEmail);
            ps.setString(2, loginOuEmail);
            ps.setInt(3, empresaId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extrairUsuarioDoResultSet(rs, true);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO: " + e.getMessage());
        }
        return null;
    }

    public Usuario buscarPorUsuarioESenha(String loginOuEmail, String senhaTextoPlano) {
        String sql = "SELECT id, nome, senha, email, funcao, permissao, excluido FROM usuarios WHERE (nome = ? OR LOWER(email) = LOWER(?)) AND empresa_id = ? AND excluido IS NOT TRUE";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, loginOuEmail);
            ps.setString(2, loginOuEmail);
            ps.setInt(3, empresaId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashArmazenado = rs.getString("senha");
                    if (verificarSenha(senhaTextoPlano, hashArmazenado)) {
                        return extrairUsuarioDoResultSet(rs, false);
                    }
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO: " + e.getMessage());
        }
        return null;
    }

    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, nome, email, funcao, permissao, excluido FROM usuarios WHERE empresa_id = ? ORDER BY nome";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empresaId());
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(extrairUsuarioDoResultSet(rs, false));
            }
            }
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO: " + e.getMessage());
        }
        return lista;
    }

    public List<String> listarNomesDeUsuarios() {
        return listarLoginsAtivos();
    }

    /**
     * Retorna apenas os logins de usuarios ativos (nao excluidos) para combo de login.
     * #DS5-202: filtra por empresa_id do TenantContext — evita enumeracao cross-tenant
     * quando o desktop apontar para banco central.
     */
    public List<String> listarLoginsAtivos() {
        List<String> logins = new ArrayList<>();
        String sql = "SELECT login_usuario FROM usuarios WHERE excluido IS NOT TRUE AND "
                   + DAOUtils.TENANT_FILTER + " ORDER BY login_usuario";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DAOUtils.setEmpresa(ps, 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logins.add(rs.getString("login_usuario"));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro SQL em UsuarioDAO.listarLoginsAtivos: " + e.getMessage());
        }
        return logins;
    }

    private Usuario extrairUsuarioDoResultSet(ResultSet rs, boolean incluirSenhaHashNoObjeto) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNomeCompleto(rs.getString("nome"));
        u.setLoginUsuario(rs.getString("nome"));
        if (incluirSenhaHashNoObjeto) {
            u.setSenhaHash(rs.getString("senha"));
        } else {
            u.setSenhaHash(null);
        }
        u.setEmail(rs.getString("email"));
        u.setFuncao(rs.getString("funcao"));
        u.setPermissoes(rs.getString("permissao"));
        u.setAtivo(!rs.getBoolean("excluido"));
        u.setDeveTrocarSenha(false);
        return u;
    }

    public boolean trocarSenhaELimparFlag(int idUsuario, String novaSenhaPlana) {
        String sql = "UPDATE usuarios SET senha = ? WHERE id = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashSenha(novaSenhaPlana));
            ps.setInt(2, idUsuario);
            ps.setInt(3, empresaId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("UsuarioDAO", "Erro ao trocar senha: " + e.getMessage());
            return false;
        }
    }
}
