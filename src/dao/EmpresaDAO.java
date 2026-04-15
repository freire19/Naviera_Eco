package dao;

import model.Empresa;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import util.AppLogger;

// D026: dados da empresa (CNPJ, tel) sao publicos e usados em recibos/impressao.
// Auth de escrita esta no CadastroEmpresaController (exigirAdmin). Leitura livre e intencional.
public class EmpresaDAO {

    /** @deprecated Usar buscar() que usa TenantContext. Mantido para compatibilidade. */
    @Deprecated
    public static final int ID_EMPRESA_PRINCIPAL = 1;

    // DP012: cache por empresa_id — configuracao_empresa raramente muda
    private static final ConcurrentHashMap<Integer, Empresa> cacheEmpresas = new ConcurrentHashMap<>();

    /** Invalida cache da empresa atual. */
    public static void invalidarCache() {
        cacheEmpresas.remove(DAOUtils.empresaId());
    }

    /** Busca configuracao da empresa do tenant atual. */
    public Empresa buscar() {
        int empresaId = DAOUtils.empresaId();

        // DP012: retorna cache se disponivel
        Empresa cached = cacheEmpresas.get(empresaId);
        if (cached != null) return cached;

        String sql = "SELECT id_config, companhia, nome_embarcacao, comandante, proprietario, origem_padrao, " +
                     "gerente, linha_rio_padrao, cnpj, ie, endereco, cep, telefone, frase_relatorio, path_logo, recomendacoes_bilhete " +
                     "FROM configuracao_empresa WHERE empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Empresa e = mapearResultSet(rs);
                    cacheEmpresas.put(empresaId, e);
                    return e;
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("EmpresaDAO", "Erro SQL em EmpresaDAO.buscar: " + e.getMessage());
        }
        return null;
    }

    /**
     * Compatibilidade com chamadas legadas que passam um id (ex: ID_EMPRESA_PRINCIPAL).
     * O parametro {@code id} e IGNORADO — sempre usa o tenant do TenantContext.
     * Em ambiente multi-tenant cada Desktop opera com um unico tenant, portanto o id
     * passado sempre corresponde ao tenant atual. Nao usar para buscar empresas
     * arbitrarias: criar novo metodo com query WHERE id = ? nesse caso.
     */
    public Empresa buscarPorId(int id) {
        return buscar();
    }

    public boolean salvarOuAtualizar(Empresa empresa) {
        int empresaId = DAOUtils.empresaId();
        Empresa existente = buscar();

        String sql;
        if (existente == null) {
            sql = "INSERT INTO configuracao_empresa (empresa_id, companhia, nome_embarcacao, comandante, proprietario, " +
                  "origem_padrao, gerente, linha_rio_padrao, cnpj, ie, endereco, cep, telefone, frase_relatorio, path_logo, recomendacoes_bilhete) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE configuracao_empresa SET companhia=?, nome_embarcacao=?, comandante=?, proprietario=?, " +
                  "origem_padrao=?, gerente=?, linha_rio_padrao=?, cnpj=?, ie=?, endereco=?, cep=?, telefone=?, " +
                  "frase_relatorio=?, path_logo=?, recomendacoes_bilhete=? WHERE empresa_id=?";
        }

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (existente == null) {
                ps.setInt(paramIndex++, empresaId);
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
            ps.setString(paramIndex++, empresa.getRecomendacoesBilhete());

            if (existente != null) {
                ps.setInt(paramIndex++, empresaId);
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) invalidarCache();
            return affectedRows > 0;

        } catch (SQLException e) {
            AppLogger.warn("EmpresaDAO", "Erro SQL em EmpresaDAO.salvarOuAtualizar: " + e.getMessage());
            return false;
        }
    }

    private Empresa mapearResultSet(ResultSet rs) throws SQLException {
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
        e.setRecomendacoesBilhete(rs.getString("recomendacoes_bilhete"));
        return e;
    }
}
