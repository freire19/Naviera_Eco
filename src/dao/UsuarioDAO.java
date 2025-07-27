package dao;

import model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
        // BCrypt hashes usually start with $2a$, $2b$, or $2y$
        if (!hashArmazenado.startsWith("$2a$") && !hashArmazenado.startsWith("$2b$") && !hashArmazenado.startsWith("$2y$")) {
            System.err.println("AVISO: Tentativa de verificar senha com hash em formato não-BCrypt: " + hashArmazenado);
            // Poderia tentar tratar como senha em texto plano antiga se necessário, mas por segurança, falha.
            return false;
        }
        try {
            return BCrypt.checkpw(senhaTextoPlano, hashArmazenado);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro ao verificar senha com BCrypt (formato de hash inválido?): " + e.getMessage());
            return false;
        }
    }
    
    public int gerarProximoId() {
        String sql = "SELECT nextval('usuarios_id_usuario_seq')";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao gerar próximo ID para usuário: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public boolean inserir(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nome_completo, login_usuario, senha_hash, email, funcao, permissoes, ativo) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNomeCompleto());
            ps.setString(2, usuario.getLoginUsuario());
            ps.setString(3, hashSenha(usuario.getSenha()));
            ps.setString(4, usuario.getEmail());
            ps.setString(5, usuario.getFuncao());
            ps.setString(6, usuario.getPermissoes());
            ps.setBoolean(7, usuario.isAtivo());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        usuario.setId(generatedKeys.getInt("id_usuario"));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir usuário: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean atualizar(Usuario usuario) {
        boolean atualizarSenha = usuario.getSenha() != null && !usuario.getSenha().isEmpty();
        
        StringBuilder sqlBuilder = new StringBuilder("UPDATE usuarios SET nome_completo=?, login_usuario=?, email=?, funcao=?, permissoes=?, ativo=? ");
        if (atualizarSenha) {
            sqlBuilder.append(", senha_hash=? ");
        }
        sqlBuilder.append("WHERE id_usuario=?");
        String sql = sqlBuilder.toString();

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            ps.setString(paramIndex++, usuario.getNomeCompleto());
            ps.setString(paramIndex++, usuario.getLoginUsuario());
            ps.setString(paramIndex++, usuario.getEmail());
            ps.setString(paramIndex++, usuario.getFuncao());
            ps.setString(paramIndex++, usuario.getPermissoes());
            ps.setBoolean(paramIndex++, usuario.isAtivo());

            if (atualizarSenha) {
                ps.setString(paramIndex++, hashSenha(usuario.getSenha()));
            }
            ps.setInt(paramIndex++, usuario.getId());

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar usuário: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean excluir(int idUsuario) {
        String sql = "DELETE FROM usuarios WHERE id_usuario=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir usuário: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public Usuario buscarPorId(int idUsuario) {
        String sql = "SELECT id_usuario, nome_completo, login_usuario, senha_hash, email, funcao, permissoes, ativo FROM usuarios WHERE id_usuario = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extrairUsuarioDoResultSet(rs, true);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário por ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public Usuario buscarPorLogin(String loginUsuario) {
        String sql = "SELECT id_usuario, nome_completo, login_usuario, senha_hash, email, funcao, permissoes, ativo FROM usuarios WHERE login_usuario = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extrairUsuarioDoResultSet(rs, true);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário por login: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Renomeado de verificarLogin para buscarPorUsuarioESenha para alinhar com o LoginController
    public Usuario buscarPorUsuarioESenha(String loginUsuario, String senhaTextoPlano) {
        String sql = "SELECT id_usuario, nome_completo, login_usuario, senha_hash, email, funcao, permissoes, ativo FROM usuarios WHERE login_usuario = ? AND ativo = TRUE";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, loginUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashArmazenado = rs.getString("senha_hash");
                    if (verificarSenha(senhaTextoPlano, hashArmazenado)) {
                        return extrairUsuarioDoResultSet(rs, false);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário por login e senha: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, nome_completo, login_usuario, email, funcao, permissoes, ativo FROM usuarios ORDER BY nome_completo";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(extrairUsuarioDoResultSet(rs, false));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar todos os usuários: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    // Método para listar apenas os nomes de login dos usuários (para ComboBox)
    public List<String> listarNomesDeUsuarios() {
        List<String> nomesUsuarios = new ArrayList<>();
        String sql = "SELECT login_usuario FROM usuarios ORDER BY login_usuario";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                nomesUsuarios.add(rs.getString("login_usuario"));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar nomes de usuários para ComboBox: " + e.getMessage());
            e.printStackTrace();
        }
        return nomesUsuarios;
    }
    
    private Usuario extrairUsuarioDoResultSet(ResultSet rs, boolean incluirSenhaHashNoObjeto) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id_usuario"));
        u.setNomeCompleto(rs.getString("nome_completo"));
        u.setLoginUsuario(rs.getString("login_usuario"));
        if (incluirSenhaHashNoObjeto) {
            u.setSenha(rs.getString("senha_hash"));
        } else {
            u.setSenha(null);
        }
        u.setEmail(rs.getString("email"));
        u.setFuncao(rs.getString("funcao"));
        u.setPermissoes(rs.getString("permissoes"));
        u.setAtivo(rs.getBoolean("ativo"));
        return u;
    }
}