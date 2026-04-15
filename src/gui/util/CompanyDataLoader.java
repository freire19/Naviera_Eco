package gui.util;

import dao.ConexaoBD;
import dao.EmpresaDAO;
import model.Empresa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * ============================================================================
 * CARREGAMENTO DE DADOS DA EMPRESA
 * ============================================================================
 *
 * Responsável por:
 * - Buscar os dados da empresa via EmpresaDAO
 * - Fallback direto via SQL caso o DAO não encontre registro
 * - Expor os dados como campos tipados para uso nos relatórios
 * ============================================================================
 */
public class CompanyDataLoader {

    private Empresa empresa;
    private String nomeEmpresa;
    private String cnpj;
    private String telefone;
    private String endereco;
    private String caminhoLogo;

    /**
     * Carrega os dados da empresa ao instanciar.
     */
    public CompanyDataLoader() {
        carregarDadosEmpresa();
    }

    // ========================================================================
    // CARREGAMENTO
    // ========================================================================

    private void carregarDadosEmpresa() {
        try {
            EmpresaDAO empresaDAO = new EmpresaDAO();
            empresa = empresaDAO.buscarPorId(EmpresaDAO.ID_EMPRESA_PRINCIPAL);

            if (empresa != null) {
                nomeEmpresa = empresa.getEmbarcacao() != null ? empresa.getEmbarcacao() : "SISTEMA";
                cnpj = empresa.getCnpj();
                telefone = empresa.getTelefone();
                endereco = empresa.getEndereco();
                caminhoLogo = empresa.getCaminhoFoto();
            } else {
                // Fallback: buscar direto da tabela de configuração
                try (Connection con = ConexaoBD.getConnection();
                     PreparedStatement stmt = con.prepareStatement(
                         "SELECT nome_embarcacao, cnpj, telefone, endereco, path_logo FROM configuracao_empresa WHERE empresa_id = ? LIMIT 1")) {
                    stmt.setInt(1, dao.DAOUtils.empresaId());
                    // DR213: try-with-resources para ResultSet
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            nomeEmpresa = rs.getString("nome_embarcacao");
                            cnpj = rs.getString("cnpj");
                            telefone = rs.getString("telefone");
                            endereco = rs.getString("endereco");
                            caminhoLogo = rs.getString("path_logo");
                        }
                    }
                }
        } catch (Exception e) {
            // DR220: logar erro em vez de silenciar completamente
            AppLogger.warn("CompanyDataLoader", "Erro ao carregar dados da empresa: " + e.getMessage());
            nomeEmpresa = "SISTEMA";
        }

        if (nomeEmpresa == null || nomeEmpresa.isEmpty()) {
            nomeEmpresa = "SISTEMA";
        }
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    public Empresa getEmpresa() { return empresa; }
    public String getNomeEmpresa() { return nomeEmpresa; }
    public String getCnpj() { return cnpj; }
    public String getTelefone() { return telefone; }
    public String getEndereco() { return endereco; }
    public String getCaminhoLogo() { return caminhoLogo; }
}
