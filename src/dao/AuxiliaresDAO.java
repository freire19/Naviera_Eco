package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AuxiliaresDAO {

    // --- Métodos Auxiliares Genéricos (para obter IDs de tabelas auxiliares) ---

    /**
     * Método auxiliar genérico para obter o ID de uma tabela auxiliar pelo nome.
     * @param tabela Nome da tabela auxiliar (ex: "aux_horarios_saida").
     * @param colunaNome Nome da coluna que contém a descrição (ex: "descricao_horario_saida").
     * @param colunaId Nome da coluna que contém o ID (ex: "id_horario_saida").
     * @param valorNome O valor da descrição a ser buscado (ex: "08:00 AM").
     * @return O ID correspondente (Integer) ou null se não encontrado ou valorNome for nulo/vazio.
     * @throws SQLException Se ocorrer um erro de SQL durante a operação de banco de dados.
     */
    public Integer obterIdAuxiliar(String tabela, String colunaNome, String colunaId, String valorNome) throws SQLException {
        if (valorNome == null || valorNome.trim().isEmpty() || "N/A".equalsIgnoreCase(valorNome)) {
            return null;
        }
        String sql = "SELECT " + colunaId + " FROM " + tabela + " WHERE " + colunaNome + " ILIKE ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, valorNome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(colunaId);
                }
            }
        }
        return null;
    }

    /**
     * Método auxiliar para obter o ID de uma rota pela origem e destino.
     * @param origem A string da origem da rota.
     * @param destino A string do destino da rota.
     * @return O ID da rota (Integer) ou null se não encontrada ou origem for nula/vazia.
     * @throws SQLException Se ocorrer um erro de SQL durante a operação de banco de dados.
     */
    public Integer obterIdRotaPelaOrigemDestino(String origem, String destino) throws SQLException {
        if (origem == null || origem.trim().isEmpty()) {
            return null;
        }
        String sql;
        if (destino == null || destino.trim().isEmpty()) {
            sql = "SELECT id FROM rotas WHERE origem ILIKE ? AND (destino IS NULL OR destino = '')";
        } else {
            sql = "SELECT id FROM rotas WHERE origem ILIKE ? AND destino ILIKE ?";
        }

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, origem);
            if (destino != null && !destino.trim().isEmpty()) {
                stmt.setString(2, destino);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }
    
    // Método auxiliar para buscar nomes de auxiliares pelo ID.
    public String buscarNomeAuxiliarPorId(String tabela, String colunaNome, String colunaId, Integer id) throws SQLException {
        if (id == null || id == 0) {
            return null;
        }
        String sql = "SELECT " + colunaNome + " FROM " + tabela + " WHERE " + colunaId + " = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(colunaNome);
                }
            }
        }
        return null;
    }


    // --- Métodos para Tipo de Documento (tabela: aux_tipos_documento) ---
    public boolean inserirTipoDoc(String nome) throws SQLException {
        String sql = "INSERT INTO aux_tipos_documento (nome_tipo_doc) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<String> listarTipoDoc() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_tipo_doc FROM aux_tipos_documento ORDER BY nome_tipo_doc";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_tipo_doc"));
            }
        }
        return lista;
    }

    public boolean atualizarTipoDoc(int id, String novoNome) throws SQLException {
        String sql = "UPDATE aux_tipos_documento SET nome_tipo_doc=? WHERE id_tipo_doc=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean excluirTipoDoc(int id) throws SQLException {
        String sql = "DELETE FROM aux_tipos_documento WHERE id_tipo_doc=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public Integer buscarIdTipoDocPorNome(String nome) throws SQLException { // Retorno Integer
        Integer ret = null;
        String sql = "SELECT id_tipo_doc FROM aux_tipos_documento WHERE nome_tipo_doc ILIKE ?";
        if (nome == null || nome.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = rs.getInt("id_tipo_doc");
                }
            }
        }
        return ret;
    }

    // --- Métodos para Sexo (tabela: aux_sexo) ---
    public boolean inserirSexo(String nome) throws SQLException {
        String sql = "INSERT INTO aux_sexo (nome_sexo) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<String> listarSexo() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_sexo FROM aux_sexo ORDER BY nome_sexo";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_sexo"));
            }
        }
        return lista;
    }

    public boolean atualizarSexo(int id, String novoNome) throws SQLException {
        String sql = "UPDATE aux_sexo SET nome_sexo=? WHERE id_sexo=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean excluirSexo(int id) throws SQLException {
        String sql = "DELETE FROM aux_sexo WHERE id_sexo=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public Integer buscarIdSexoPorNome(String nome) throws SQLException { // Retorno Integer
        Integer ret = null;
        String sql = "SELECT id_sexo FROM aux_sexo WHERE nome_sexo ILIKE ?";
        if (nome == null || nome.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = rs.getInt("id_sexo");
                }
            }
        }
        return ret;
    }

    // --- Métodos para Nacionalidade (tabela: aux_nacionalidades) ---
    public boolean inserirNacionalidade(String nome) throws SQLException {
        String sql = "INSERT INTO aux_nacionalidades (nome_nacionalidade) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<String> listarNacionalidade() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_nacionalidade FROM aux_nacionalidades ORDER BY nome_nacionalidade";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_nacionalidade"));
            }
        }
        return lista;
    }

    public boolean atualizarNacionalidade(int id, String novoNome) throws SQLException {
        String sql = "UPDATE aux_nacionalidades SET nome_nacionalidade=? WHERE id_nacionalidade=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean excluirNacionalidade(int id) throws SQLException {
        String sql = "DELETE FROM aux_nacionalidades WHERE id_nacionalidade=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public Integer buscarIdNacionalidadePorNome(String nome) throws SQLException { // Retorno Integer
        Integer ret = null;
        String sql = "SELECT id_nacionalidade FROM aux_nacionalidades WHERE nome_nacionalidade ILIKE ?";
        if (nome == null || nome.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = rs.getInt("id_nacionalidade");
                }
            }
        }
        return ret;
    }

    // --- Métodos para Tipos de Passagem (tabela: aux_tipos_passagem) ---
    public boolean inserirTipoPassagem(String nome) throws SQLException {
        String sql = "INSERT INTO aux_tipos_passagem (nome_tipo_passagem) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<String> listarPassagemAux() throws SQLException { // Usado no CadastroTarifaController
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_tipo_passagem FROM aux_tipos_passagem ORDER BY nome_tipo_passagem";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_tipo_passagem"));
            }
        }
        return lista;
    }

    public boolean atualizarTipoPassagem(int id, String novoNome) throws SQLException {
        String sql = "UPDATE aux_tipos_passagem SET nome_tipo_passagem=? WHERE id_tipo_passagem=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean excluirTipoPassagem(int id) throws SQLException {
        String sql = "DELETE FROM aux_tipos_passagem WHERE id_tipo_passagem=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public Integer buscarIdTipoPassagemPorNome(String nomeTipoPassagem) throws SQLException { // Retorno Integer
        Integer ret = null;
        String sql = "SELECT id_tipo_passagem FROM aux_tipos_passagem WHERE nome_tipo_passagem ILIKE ?";
        if (nomeTipoPassagem == null || nomeTipoPassagem.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomeTipoPassagem.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = rs.getInt("id_tipo_passagem");
                }
            }
        }
        return ret;
    }

    // --- Métodos para Agente Auxiliar (tabela: aux_agentes) ---
    public boolean inserirAgenteAux(String nome) throws SQLException {
        String sql = "INSERT INTO aux_agentes (nome_agente) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<String> listarAgenteAux() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_agente FROM aux_agentes ORDER BY nome_agente";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_agente"));
            }
        }
        return lista;
    }

    public boolean atualizarAgenteAux(int id, String novoNome) throws SQLException {
        String sql = "UPDATE aux_agentes SET nome_agente=? WHERE id_agente=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean excluirAgenteAux(int id) throws SQLException {
        String sql = "DELETE FROM aux_agentes WHERE id_agente=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public Integer buscarIdAgenteAuxPorNome(String nome) throws SQLException { // Retorno Integer
        Integer ret = null;
        String sql = "SELECT id_agente FROM aux_agentes WHERE nome_agente ILIKE ?";
        if (nome == null || nome.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = rs.getInt("id_agente");
                }
            }
        }
        return ret;
    }

    // --- Métodos para Horário de Saída (tabela: aux_horarios_saida) ---
    public boolean inserirHorarioSaida(String descricao) throws SQLException {
        String sql = "INSERT INTO aux_horarios_saida (descricao_horario_saida) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, descricao);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<String> listarHorarioSaida() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT descricao_horario_saida FROM aux_horarios_saida ORDER BY descricao_horario_saida";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("descricao_horario_saida"));
            }
        }
        return lista;
    }

    public boolean atualizarHorarioSaida(int id, String novaDescricao) throws SQLException {
        String sql = "UPDATE aux_horarios_saida SET descricao_horario_saida=? WHERE id_horario_saida=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novaDescricao);
            ps.setInt(2, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean excluirHorarioSaida(int id) throws SQLException {
        String sql = "DELETE FROM aux_horarios_saida WHERE id_horario_saida=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public Integer obterIdHorarioSaidaPorNome(String descricao) throws SQLException { // Renomeado, retorna Integer
        Integer ret = null;
        String sql = "SELECT id_horario_saida FROM aux_horarios_saida WHERE descricao_horario_saida ILIKE ?";
        if (descricao == null || descricao.trim().isEmpty() || "N/A".equalsIgnoreCase(descricao)) {
            return null;
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, descricao.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = rs.getInt("id_horario_saida");
                }
            }
        }
        return ret;
    }

    // --- Métodos para Acomodação (tabela: aux_acomodacoes) ---
    public boolean inserirAcomodacao(String nome) throws SQLException {
        String sql = "INSERT INTO aux_acomodacoes (nome_acomodacao) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<String> listarAcomodacao() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_acomodacao FROM aux_acomodacoes ORDER BY nome_acomodacao";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_acomodacao"));
            }
        }
        return lista;
    }

    public boolean atualizarAcomodacao(int id, String novoNome) throws SQLException {
        String sql = "UPDATE aux_acomodacoes SET nome_acomodacao=? WHERE id_acomodacao=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean excluirAcomodacao(int id) throws SQLException {
        String sql = "DELETE FROM aux_acomodacoes WHERE id_acomodacao=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public Integer buscarIdAcomodacaoPorNome(String nome) throws SQLException { // Retorno Integer
        Integer ret = null;
        String sql = "SELECT id_acomodacao FROM aux_acomodacoes WHERE nome_acomodacao ILIKE ?";
        if (nome == null || nome.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = rs.getInt("id_acomodacao");
                }
            }
        }
        return ret;
    }

    // --- Métodos para Tipos de Pagamento (tabela: aux_tipos_pagamento) ---
    public List<String> listarTiposPagamento() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_forma_pagamento FROM aux_formas_pagamento ORDER BY nome_forma_pagamento";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_forma_pagamento"));
            }
        }
        return lista;
    }
    
    public Integer buscarIdTipoPagamentoPorNome(String nome) throws SQLException {
        Integer ret = null;
        String sql = "SELECT id_forma_pagamento FROM aux_formas_pagamento WHERE nome_forma_pagamento ILIKE ?";
        if (nome == null || nome.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = rs.getInt("id_forma_pagamento");
                }
            }
        }
        return ret;
    }


    // --- Métodos para Contatos (Remetentes/Destinatários) ---
    public List<String> listarContatosRemetentes() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_razao_social FROM contatos ORDER BY nome_razao_social";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_razao_social"));
            }
        }
        return lista;
    }

    public List<String> listarContatosDestinatarios() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_razao_social FROM contatos ORDER BY nome_razao_social";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nome_razao_social"));
            }
        }
        return lista;
    }
}