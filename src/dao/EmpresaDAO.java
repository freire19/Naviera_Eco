package dao;

import model.Empresa; 
import java.sql.*;

public class EmpresaDAO {

    /** ID fixo do registro unico de configuracao da empresa */
    public static final int ID_EMPRESA_PRINCIPAL = 1;

    // Salva ou atualiza o único registro da empresa
    public boolean salvarOuAtualizar(Empresa empresa) {
        Empresa existente = buscarPorId(ID_EMPRESA_PRINCIPAL);

        String sql;
        // Usa o nome da tabela correto: "configuracao_empresa"
        if (existente == null) {
            // INSERT se não existe
            sql = "INSERT INTO configuracao_empresa (id_config, companhia, nome_embarcacao, comandante, proprietario, " +
                  "origem_padrao, gerente, linha_rio_padrao, cnpj, ie, endereco, cep, telefone, frase_relatorio, path_logo) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            // UPDATE se já existe
            sql = "UPDATE configuracao_empresa SET companhia=?, nome_embarcacao=?, comandante=?, proprietario=?, " +
                  "origem_padrao=?, gerente=?, linha_rio_padrao=?, cnpj=?, ie=?, endereco=?, cep=?, telefone=?, " +
                  "frase_relatorio=?, path_logo=? WHERE id_config=?";
        }

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (existente == null) { 
                ps.setInt(paramIndex++, 1); 
            }
            ps.setString(paramIndex++, empresa.getCompanhia());
            ps.setString(paramIndex++, empresa.getEmbarcacao()); 
            ps.setString(paramIndex++, empresa.getComandante());
            ps.setString(paramIndex++, empresa.getProprietario());
            ps.setString(paramIndex++, empresa.getOrigem());
            ps.setString(paramIndex++, empresa.getGerente());
            ps.setString(paramIndex++, empresa.getLinhaDoRio());
            ps.setString(paramIndex++, empresa.getCnpj());
            ps.setString(paramIndex++, empresa.getIe());
            ps.setString(paramIndex++, empresa.getEndereco());
            ps.setString(paramIndex++, empresa.getCep());
            ps.setString(paramIndex++, empresa.getTelefone());
            ps.setString(paramIndex++, empresa.getFrase());
            ps.setString(paramIndex++, empresa.getCaminhoFoto());

            if (existente != null) { 
                ps.setInt(paramIndex++, 1); 
            }

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar/atualizar dados da empresa: " + e.getMessage());
            System.err.println("Erro SQL em EmpresaDAO: " + e.getMessage());
            return false;
        }
    }

    public Empresa buscarPorId(int id) { 
        String sql = "SELECT id_config, companhia, nome_embarcacao, comandante, proprietario, origem_padrao, " +
                     "gerente, linha_rio_padrao, cnpj, ie, endereco, cep, telefone, frase_relatorio, path_logo " +
                     "FROM configuracao_empresa WHERE id_config = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Empresa e = new Empresa();
                    e.setId(rs.getInt("id_config"));
                    e.setCompanhia(rs.getString("companhia"));
                    e.setEmbarcacao(rs.getString("nome_embarcacao"));
                    e.setComandante(rs.getString("comandante"));
                    e.setProprietario(rs.getString("proprietario"));
                    e.setOrigem(rs.getString("origem_padrao"));
                    e.setGerente(rs.getString("gerente"));
                    e.setLinhaDoRio(rs.getString("linha_rio_padrao"));
                    e.setCnpj(rs.getString("cnpj"));
                    e.setIe(rs.getString("ie"));
                    e.setEndereco(rs.getString("endereco"));
                    e.setCep(rs.getString("cep"));
                    e.setTelefone(rs.getString("telefone"));
                    e.setFrase(rs.getString("frase_relatorio"));
                    e.setCaminhoFoto(rs.getString("path_logo"));
                    return e;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar dados da empresa por ID: " + e.getMessage());
            System.err.println("Erro SQL em EmpresaDAO: " + e.getMessage());
        }
        return null;
    }
}
