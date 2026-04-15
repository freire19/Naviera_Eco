package dao;

import model.Frete;
import model.StatusPagamento;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import util.AppLogger;

public class FreteDAO {

    // ====== Metodos de escrita (DM057: extraidos do CadastroFreteController) ======

    /**
     * Gera proximo numero de frete via sequence.
     * Fallback para MAX+1 se sequence nao existir.
     */
    public long gerarNumeroFrete() throws SQLException {
        String sql = "SELECT nextval('seq_numero_frete')";
        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            AppLogger.warn("FreteDAO", "Sequence seq_numero_frete nao encontrada. Usando fallback MAX+1.");
            String fallback = "SELECT COALESCE(MAX(numero_frete), 0) + 1 FROM fretes WHERE empresa_id = ?";
            try (Connection conn = ConexaoBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(fallback)) {
                stmt.setInt(1, DAOUtils.empresaId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        }
        return 1;
    }

    /**
     * Busca um frete pelo numero_frete (retorna ResultSet-like data como Frete completo).
     */
    public Frete buscarPorNumero(long numeroFrete) throws SQLException {
        String sql = "SELECT * FROM fretes WHERE numero_frete = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setLong(1, numeroFrete);
            pst.setInt(2, DAOUtils.empresaId());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetCompleto(rs);
                }
            }
        }
        return null;
    }

    /**
     * Busca valores financeiros existentes de um frete (para preservar no UPDATE).
     */
    public FreteFinanceiro buscarFinanceiro(Connection conn, long idFrete) throws SQLException {
        String sql = "SELECT valor_pago, troco, tipo_pagamento, nome_caixa FROM fretes WHERE id_frete = ? AND empresa_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idFrete);
            ps.setInt(2, DAOUtils.empresaId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FreteFinanceiro f = new FreteFinanceiro();
                    f.valorPago = rs.getBigDecimal("valor_pago");
                    if (f.valorPago == null) f.valorPago = BigDecimal.ZERO;
                    f.troco = rs.getBigDecimal("troco");
                    if (f.troco == null) f.troco = BigDecimal.ZERO;
                    f.tipoPagamento = rs.getString("tipo_pagamento");
                    f.nomeCaixa = rs.getString("nome_caixa");
                    return f;
                }
            }
        }
        return new FreteFinanceiro();
    }

    /**
     * Insere um novo frete no banco (dentro de transacao existente).
     */
    public void inserir(Connection conn, FreteData data) throws SQLException {
        String sql = "INSERT INTO fretes (id_frete, numero_frete, data_emissao, data_saida_viagem, local_transporte, " +
                "remetente_nome_temp, destinatario_nome_temp, rota_temp, conferente_temp, cidade_cobranca, " +
                "observacoes, num_notafiscal, valor_notafiscal, peso_notafiscal, valor_total_itens, desconto, " +
                "valor_frete_calculado, valor_pago, troco, valor_devedor, tipo_pagamento, nome_caixa, " +
                "status_frete, id_viagem, empresa_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            int i = 1;
            pst.setLong(i++, data.idFrete);
            pst.setLong(i++, data.numeroFrete);
            pst.setDate(i++, Date.valueOf(LocalDate.now()));
            pst.setDate(i++, data.dataSaida != null ? Date.valueOf(data.dataSaida) : null);
            pst.setString(i++, data.localTransporte);
            pst.setString(i++, data.remetente);
            pst.setString(i++, data.destinatario);
            pst.setString(i++, data.rota);
            pst.setString(i++, data.conferente);
            pst.setString(i++, data.cidadeCobranca);
            pst.setString(i++, data.observacoes);
            pst.setString(i++, data.numNotaFiscal);
            pst.setBigDecimal(i++, data.valorNotaFiscal);
            pst.setBigDecimal(i++, data.pesoNotaFiscal);
            pst.setBigDecimal(i++, data.totalItens);
            pst.setBigDecimal(i++, BigDecimal.ZERO); // desconto
            pst.setBigDecimal(i++, data.valorFreteCalculado);
            pst.setBigDecimal(i++, BigDecimal.ZERO); // valor_pago
            pst.setBigDecimal(i++, BigDecimal.ZERO); // troco
            pst.setBigDecimal(i++, data.valorFreteCalculado); // valor_devedor = total
            pst.setString(i++, null); // tipo_pagamento
            pst.setString(i++, null); // nome_caixa
            pst.setString(i++, "PENDENTE"); // status_frete
            if (data.idViagem != null) {
                pst.setLong(i++, data.idViagem);
            } else {
                pst.setNull(i++, Types.BIGINT);
            }
            pst.setInt(i++, DAOUtils.empresaId());
            pst.executeUpdate();
        }
    }

    /**
     * Atualiza um frete existente preservando dados financeiros (dentro de transacao existente).
     */
    public void atualizar(Connection conn, FreteData data) throws SQLException {
        // DL034: preservar valor_pago/troco/devedor/pagamento existentes
        FreteFinanceiro fin = buscarFinanceiro(conn, data.idFrete);
        BigDecimal devedorRecalculado = data.valorFreteCalculado.subtract(fin.valorPago).max(BigDecimal.ZERO);
        String statusRecalculado = StatusPagamento.calcularPorSaldo(devedorRecalculado, fin.valorPago).name();

        String sql = "UPDATE fretes SET data_emissao = ?, data_saida_viagem = ?, local_transporte = ?, " +
                "remetente_nome_temp = ?, destinatario_nome_temp = ?, rota_temp = ?, conferente_temp = ?, " +
                "cidade_cobranca = ?, observacoes = ?, num_notafiscal = ?, valor_notafiscal = ?, peso_notafiscal = ?, " +
                "valor_total_itens = ?, desconto = ?, valor_frete_calculado = ?, valor_pago = ?, troco = ?, " +
                "valor_devedor = ?, tipo_pagamento = ?, nome_caixa = ?, status_frete = ?, id_viagem = ? " +
                "WHERE id_frete = ? AND empresa_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            int i = 1;
            pst.setDate(i++, Date.valueOf(LocalDate.now()));
            pst.setDate(i++, data.dataSaida != null ? Date.valueOf(data.dataSaida) : null);
            pst.setString(i++, data.localTransporte);
            pst.setString(i++, data.remetente);
            pst.setString(i++, data.destinatario);
            pst.setString(i++, data.rota);
            pst.setString(i++, data.conferente);
            pst.setString(i++, data.cidadeCobranca);
            pst.setString(i++, data.observacoes);
            pst.setString(i++, data.numNotaFiscal);
            pst.setBigDecimal(i++, data.valorNotaFiscal);
            pst.setBigDecimal(i++, data.pesoNotaFiscal);
            pst.setBigDecimal(i++, data.totalItens);
            pst.setBigDecimal(i++, BigDecimal.ZERO); // desconto
            pst.setBigDecimal(i++, data.valorFreteCalculado);
            pst.setBigDecimal(i++, fin.valorPago);
            pst.setBigDecimal(i++, fin.troco);
            pst.setBigDecimal(i++, devedorRecalculado);
            pst.setString(i++, fin.tipoPagamento);
            pst.setString(i++, fin.nomeCaixa);
            pst.setString(i++, statusRecalculado);
            if (data.idViagem != null) {
                pst.setLong(i++, data.idViagem);
            } else {
                pst.setNull(i++, Types.BIGINT);
            }
            pst.setLong(i++, data.idFrete);
            pst.setInt(i++, DAOUtils.empresaId());
            pst.executeUpdate();
        }
    }

    /**
     * Deleta e reinsere itens de um frete (dentro de transacao existente).
     */
    public void salvarItens(Connection conn, long idFrete, List<FreteItemData> itens) throws SQLException {
        try (PreparedStatement pstDel = conn.prepareStatement("DELETE FROM frete_itens WHERE id_frete = ?")) {
            pstDel.setLong(1, idFrete);
            pstDel.executeUpdate();
        }
        String sqlItem = "INSERT INTO frete_itens (id_frete, nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item) VALUES (?,?,?,?,?)";
        try (PreparedStatement pst = conn.prepareStatement(sqlItem)) {
            for (FreteItemData it : itens) {
                pst.setLong(1, idFrete);
                pst.setString(2, it.nomeItem);
                pst.setInt(3, it.quantidade);
                pst.setBigDecimal(4, it.precoUnitario);
                pst.setBigDecimal(5, it.subtotal);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }

    /**
     * Busca itens de um frete.
     */
    public List<FreteItemData> buscarItens(long idFrete) throws SQLException {
        List<FreteItemData> itens = new ArrayList<>();
        String sql = "SELECT nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item FROM frete_itens WHERE id_frete = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setLong(1, idFrete);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    FreteItemData item = new FreteItemData();
                    item.nomeItem = rs.getString("nome_item_ou_id_produto");
                    item.quantidade = rs.getInt("quantidade");
                    item.precoUnitario = rs.getBigDecimal("preco_unitario");
                    item.subtotal = rs.getBigDecimal("subtotal_item");
                    itens.add(item);
                }
            }
        }
        return itens;
    }

    /**
     * Busca lista de contatos (nomes) para combo boxes.
     */
    public List<String> listarContatos() {
        List<String> contatos = new ArrayList<>();
        String sql = "SELECT nome_razao_social FROM contatos ORDER BY nome_razao_social";
        try (Connection c = ConexaoBD.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                String nome = r.getString(1);
                if (nome != null) contatos.add(nome);
            }
        } catch (SQLException e) {
            AppLogger.warn("FreteDAO", "Erro ao listar contatos: " + e.getMessage());
        }
        return contatos;
    }

    /**
     * Insere um novo contato e retorna o nome em uppercase.
     */
    public String inserirContato(String nome) throws SQLException {
        String nomeUpper = nome.toUpperCase();
        String sql = "INSERT INTO contatos (nome_razao_social) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nomeUpper);
            pst.executeUpdate();
        }
        return nomeUpper;
    }

    /**
     * Lista nomes de conferentes da empresa.
     */
    public List<String> listarNomesConferentes() {
        List<String> nomes = new ArrayList<>();
        String sql = "SELECT nome_conferente FROM conferentes WHERE empresa_id = ? ORDER BY nome_conferente";
        try (Connection c = ConexaoBD.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, DAOUtils.empresaId());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    String nome = r.getString(1);
                    if (nome != null) nomes.add(nome);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("FreteDAO", "Erro ao listar conferentes: " + e.getMessage());
        }
        return nomes;
    }

    /**
     * Lista rotas formatadas ("Origem - Destino") da empresa.
     */
    public List<String> listarRotasFormatadas() {
        List<String> rotas = new ArrayList<>();
        String sql = "SELECT origem, destino FROM rotas WHERE empresa_id = ? ORDER BY origem, destino";
        try (Connection c = ConexaoBD.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, DAOUtils.empresaId());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    String o = r.getString("origem");
                    String d = r.getString("destino");
                    String rd = "";
                    if (o != null && !o.trim().isEmpty()) rd += o.trim();
                    if (d != null && !d.trim().isEmpty()) {
                        if (!rd.isEmpty()) rd += " - ";
                        rd += d.trim();
                    }
                    if (!rd.isEmpty()) rotas.add(rd);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("FreteDAO", "Erro ao listar rotas: " + e.getMessage());
        }
        return rotas;
    }

    // ====== DTOs internos para transferencia de dados ======

    /** Dados financeiros preservados durante UPDATE */
    public static class FreteFinanceiro {
        public BigDecimal valorPago = BigDecimal.ZERO;
        public BigDecimal troco = BigDecimal.ZERO;
        public String tipoPagamento;
        public String nomeCaixa;
    }

    /** Dados de um frete para INSERT/UPDATE */
    public static class FreteData {
        public long idFrete;
        public long numeroFrete;
        public LocalDate dataSaida;
        public String localTransporte;
        public String remetente;
        public String destinatario;
        public String rota;
        public String conferente;
        public String cidadeCobranca;
        public String observacoes;
        public String numNotaFiscal;
        public BigDecimal valorNotaFiscal = BigDecimal.ZERO;
        public BigDecimal pesoNotaFiscal = BigDecimal.ZERO;
        public BigDecimal totalItens = BigDecimal.ZERO;
        public BigDecimal valorFreteCalculado = BigDecimal.ZERO;
        public Long idViagem;
    }

    /** Dados de um item de frete */
    public static class FreteItemData {
        public String nomeItem;
        public int quantidade;
        public BigDecimal precoUnitario = BigDecimal.ZERO;
        public BigDecimal subtotal = BigDecimal.ZERO;
    }

    private Frete mapResultSetCompleto(ResultSet rs) throws SQLException {
        Frete f = new Frete();
        f.setIdFrete(rs.getLong("id_frete"));
        f.setNumeroFrete(String.valueOf(rs.getLong("numero_frete")));
        f.setIdViagem(rs.getLong("id_viagem"));
        f.setNomeRemetente(rs.getString("remetente_nome_temp"));
        f.setNomeDestinatario(rs.getString("destinatario_nome_temp"));
        f.setNomeRota(rs.getString("rota_temp"));
        f.setNomeConferente(rs.getString("conferente_temp"));
        f.setStatus(rs.getString("status_frete"));
        Date de = rs.getDate("data_emissao");
        if (de != null) f.setDataEmissao(de.toLocalDate());
        Date dv = rs.getDate("data_saida_viagem");
        if (dv != null) f.setDataViagem(dv.toLocalDate());
        f.setValorNominal(rs.getBigDecimal("valor_total_itens"));
        f.setValorPago(rs.getBigDecimal("valor_pago"));
        f.setValorDevedor(rs.getBigDecimal("valor_devedor"));
        return f;
    }

    // ====== Metodos de leitura existentes ======

    /**
     * Busca fretes no banco de dados com base em múltiplos filtros.
     * @param idViagem O ID da viagem para filtrar. Se for null, busca de todas as viagens.
     * @param termoBuscaItem Um texto para pesquisar na descrição dos itens do frete.
     * @param dataInicio Data de emissão inicial para o filtro de período.
     * @param dataFim Data de emissão final para o filtro de período.
     * @return Uma lista de objetos Frete.
     */
    public List<Frete> buscarFretes(Long idViagem, String termoBuscaItem, LocalDate dataInicio, LocalDate dataFim) {
        List<Frete> fretes = new ArrayList<>();
        
        // Alterei o SQL para fazer JOIN com a tabela de viagens e pegar a data_viagem
        StringBuilder sql = new StringBuilder(
            // DP010: subquery correlacionada substituida por LEFT JOIN agrupado
            "SELECT f.id_frete, f.numero_frete, f.id_viagem, f.remetente_nome_temp AS remetente_nome, " +
            "f.destinatario_nome_temp AS destinatario_nome, f.rota_temp AS rota, " +
            "f.data_emissao, f.valor_total_itens AS valor_nominal, f.valor_devedor, f.valor_pago, " +
            "f.conferente_temp AS conferente, f.status_frete, " +
            "v.data_viagem, " +
            "COALESCE(fiv.total_volumes, 0) AS total_volumes " +
            "FROM fretes f " +
            "LEFT JOIN viagens v ON f.id_viagem = v.id_viagem " +
            "LEFT JOIN (SELECT id_frete, SUM(quantidade) AS total_volumes FROM frete_itens GROUP BY id_frete) fiv ON fiv.id_frete = f.id_frete "
        );

        List<Object> parametros = new ArrayList<>();
        List<String> condicoes = new ArrayList<>();

        // Multi-tenant: sempre filtrar por empresa
        condicoes.add("f.empresa_id = ?");
        parametros.add(DAOUtils.empresaId());

        if (idViagem != null && idViagem > 0) {
            condicoes.add("f.id_viagem = ?");
            parametros.add(idViagem);
        }

        if (termoBuscaItem != null && !termoBuscaItem.trim().isEmpty()) {
            condicoes.add("EXISTS (SELECT 1 FROM frete_itens fi WHERE fi.id_frete = f.id_frete AND fi.nome_item_ou_id_produto ILIKE ?)");
            parametros.add("%" + termoBuscaItem.trim() + "%");
        }
        
        if (dataInicio != null) {
            condicoes.add("f.data_emissao >= ?");
            parametros.add(Date.valueOf(dataInicio));
        }
        
        if (dataFim != null) {
            condicoes.add("f.data_emissao <= ?");
            parametros.add(Date.valueOf(dataFim));
        }

        if (!condicoes.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", condicoes));
        }

        sql.append(" ORDER BY f.id_frete DESC");

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Frete frete = new Frete();
                    frete.setIdFrete(rs.getLong("id_frete"));
                    frete.setNumeroFrete(rs.getString("numero_frete"));
                    frete.setIdViagem(rs.getLong("id_viagem"));
                    frete.setNomeRemetente(rs.getString("remetente_nome"));
                    frete.setNomeDestinatario(rs.getString("destinatario_nome"));
                    frete.setNomeRota(rs.getString("rota"));
                    
                    // Preenchendo a data da viagem
                    Date dataViagemSql = rs.getDate("data_viagem");
                    if (dataViagemSql != null) {
                        frete.setDataViagem(dataViagemSql.toLocalDate());
                    }

                    Date dataEmissaoSql = rs.getDate("data_emissao");
                    if (dataEmissaoSql != null) {
                        frete.setDataEmissao(dataEmissaoSql.toLocalDate());
                    }
                    
                    frete.setValorNominal(rs.getBigDecimal("valor_nominal"));
                    frete.setValorDevedor(rs.getBigDecimal("valor_devedor"));
                    frete.setValorPago(rs.getBigDecimal("valor_pago"));
                    frete.setNomeConferente(rs.getString("conferente"));
                    frete.setStatus(rs.getString("status_frete"));
                    frete.setTotalVolumes(rs.getInt("total_volumes"));
                    
                    fretes.add(frete);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("FreteDAO", "Erro SQL em FreteDAO: " + e.getMessage());
        }
        return fretes;
    }
    
    /**
     * Lista todos os fretes de uma viagem específica.
     * @param idViagem O ID da viagem para a qual os fretes serão listados.
     * @return Uma lista de objetos Frete da viagem especificada.
     */
    public List<Frete> listarPorViagem(long idViagem) {
        return buscarFretes(idViagem, null, null, null);
    }

    /**
     * Exclui um frete e seus itens em transação.
     * @param idFrete o id_frete a excluir
     * @return true se excluído com sucesso
     */
    public boolean excluir(long idFrete) {
        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstItems = conn.prepareStatement(
                    "DELETE FROM frete_itens WHERE id_frete = ? AND id_frete IN (SELECT id_frete FROM fretes WHERE empresa_id = ?)")) {
                pstItems.setLong(1, idFrete);
                pstItems.setInt(2, DAOUtils.empresaId());
                pstItems.executeUpdate();
            }

            boolean ok;
            try (PreparedStatement pstFrete = conn.prepareStatement(
                    "DELETE FROM fretes WHERE id_frete = ? AND empresa_id = ?")) {
                pstFrete.setLong(1, idFrete);
                pstFrete.setInt(2, DAOUtils.empresaId());
                ok = pstFrete.executeUpdate() > 0;
            }

            conn.commit();
            return ok;
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { /* ignorado */ } }
            AppLogger.warn("FreteDAO", "Erro SQL em FreteDAO.excluir: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignorado */ }
            }
        }
    }
}