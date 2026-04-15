package dao;

import model.Passagem;
import gui.util.SessaoUsuario;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import util.AppLogger;

// DR026: Convencao de retorno de erro neste DAO — metodos que retornam int usam -1 para
// indicar falha; metodos que retornam Long/Object retornam null em caso de erro.
// Esta convencao nao e uniforme em todos os DAOs do projeto (veja DR026 em STATUS.md).
public class PassagemDAO {

    private final AuxiliaresDAO auxiliaresDAO = new AuxiliaresDAO();

    public int obterProximoBilhete() {
        // Usa sequence para evitar race condition (DL001)
        String sql = "SELECT nextval('seq_numero_bilhete')";
        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            // Fallback se sequence não existir ainda (rodar script 005)
            AppLogger.warn("PassagemDAO", "Sequence seq_numero_bilhete não encontrada. Usando fallback MAX+1. Execute o script 005.");
            String fallback = "SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) + 1 FROM passagens";
            try (Connection conn = ConexaoBD.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(fallback)) {
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException ex) { AppLogger.warn("PassagemDAO", "Erro: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); }
        }
        return 1;
    }

    public boolean inserir(Passagem passagem) {
        String sql = "INSERT INTO passagens (" +
                     "numero_bilhete, id_passageiro, id_viagem, data_emissao, assento, id_acomodacao, id_rota, id_tipo_passagem, " +
                     "id_agente, numero_requisicao, valor_alimentacao, valor_transporte, valor_cargas, valor_desconto_tarifa, " +
                     "valor_total, valor_desconto_geral, valor_a_pagar, valor_pago, troco, valor_devedor, " +
                     "id_caixa, id_usuario_emissor, status_passagem, observacoes, id_horario_saida, " +
                     "valor_pagamento_dinheiro, valor_pagamento_pix, valor_pagamento_cartao, empresa_id" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, String.valueOf(passagem.getNumBilhete()));
            stmt.setLong(2, passagem.getIdPassageiro());
            stmt.setLong(3, passagem.getIdViagem());
            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.setString(5, passagem.getAssento());
            stmt.setObject(6, auxiliaresDAO.obterIdAuxiliar("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", passagem.getAcomodacao()));
            stmt.setLong(7, passagem.getIdRota());
            stmt.setObject(8, auxiliaresDAO.obterIdAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", passagem.getTipoPassagemAux()));
            stmt.setObject(9, auxiliaresDAO.obterIdAuxiliar("aux_agentes", "nome_agente", "id_agente", passagem.getAgenteAux()));
            stmt.setString(10, passagem.getRequisicao());
            stmt.setBigDecimal(11, passagem.getValorAlimentacao());
            stmt.setBigDecimal(12, passagem.getValorTransporte());
            stmt.setBigDecimal(13, passagem.getValorCargas());
            stmt.setBigDecimal(14, passagem.getValorDescontoTarifa());
            stmt.setBigDecimal(15, passagem.getValorTotal());
            stmt.setBigDecimal(16, passagem.getValorDesconto());
            stmt.setBigDecimal(17, passagem.getValorAPagar());
            stmt.setBigDecimal(18, passagem.getValorPago());
            stmt.setBigDecimal(19, passagem.getTroco());
            stmt.setBigDecimal(20, passagem.getDevedor());
            
            stmt.setObject(21, auxiliaresDAO.obterIdAuxiliar("caixas", "nome_caixa", "id_caixa", passagem.getCaixa()));
            // DR221: logging em WARN se sessao estiver corrompida (inserir sem usuario = auditoria incompleta)
            Integer sessaoUserId = null;
            try { if(SessaoUsuario.isUsuarioLogado()) sessaoUserId = SessaoUsuario.getUsuarioLogado().getId(); }
            catch(Exception e) { AppLogger.warn("PassagemDAO", "AVISO: passagem sera inserida sem usuario emissor: " + e.getMessage()); }
            stmt.setObject(22, sessaoUserId);
            stmt.setString(23, passagem.getStatusPassagem());
            stmt.setString(24, passagem.getObservacoes());
            stmt.setObject(25, passagem.getIdHorarioSaida());
            
            stmt.setBigDecimal(26, passagem.getValorPagamentoDinheiro());
            stmt.setBigDecimal(27, passagem.getValorPagamentoPix());
            stmt.setBigDecimal(28, passagem.getValorPagamentoCartao());
            stmt.setInt(29, DAOUtils.empresaId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) passagem.setId(rs.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) { AppLogger.warn("PassagemDAO", "Erro SQL em PassagemDAO: " + e.getMessage()); }
        return false;
    }

    public boolean atualizar(Passagem passagem) {
        String sql = "UPDATE passagens SET " +
                     "id_passageiro=?, id_viagem=?, assento=?, id_acomodacao=?, id_rota=?, id_tipo_passagem=?, " +
                     "id_agente=?, numero_requisicao=?, valor_alimentacao=?, valor_transporte=?, valor_cargas=?, valor_desconto_tarifa=?, " +
                     "valor_total=?, valor_desconto_geral=?, valor_a_pagar=?, valor_pago=?, troco=?, valor_devedor=?, " +
                     "id_caixa=?, status_passagem=?, observacoes=?, id_horario_saida=?, " +
                     "valor_pagamento_dinheiro=?, valor_pagamento_pix=?, valor_pagamento_cartao=? " +
                     "WHERE id_passagem=? AND empresa_id=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, passagem.getIdPassageiro());
            stmt.setLong(2, passagem.getIdViagem());
            stmt.setString(3, passagem.getAssento());
            stmt.setObject(4, auxiliaresDAO.obterIdAuxiliar("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", passagem.getAcomodacao()));
            stmt.setLong(5, passagem.getIdRota());
            stmt.setObject(6, auxiliaresDAO.obterIdAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", passagem.getTipoPassagemAux()));
            stmt.setObject(7, auxiliaresDAO.obterIdAuxiliar("aux_agentes", "nome_agente", "id_agente", passagem.getAgenteAux()));
            stmt.setString(8, passagem.getRequisicao());
            stmt.setBigDecimal(9, passagem.getValorAlimentacao());
            stmt.setBigDecimal(10, passagem.getValorTransporte());
            stmt.setBigDecimal(11, passagem.getValorCargas());
            stmt.setBigDecimal(12, passagem.getValorDescontoTarifa());
            stmt.setBigDecimal(13, passagem.getValorTotal());
            stmt.setBigDecimal(14, passagem.getValorDesconto());
            stmt.setBigDecimal(15, passagem.getValorAPagar());
            stmt.setBigDecimal(16, passagem.getValorPago());
            stmt.setBigDecimal(17, passagem.getTroco());
            stmt.setBigDecimal(18, passagem.getDevedor());
            
            stmt.setObject(19, auxiliaresDAO.obterIdAuxiliar("caixas", "nome_caixa", "id_caixa", passagem.getCaixa()));
            stmt.setString(20, passagem.getStatusPassagem());
            stmt.setString(21, passagem.getObservacoes());
            stmt.setObject(22, passagem.getIdHorarioSaida());
            
            stmt.setBigDecimal(23, passagem.getValorPagamentoDinheiro());
            stmt.setBigDecimal(24, passagem.getValorPagamentoPix());
            stmt.setBigDecimal(25, passagem.getValorPagamentoCartao());
            
            stmt.setLong(26, passagem.getId());
            stmt.setInt(27, DAOUtils.empresaId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) { AppLogger.warn("PassagemDAO", "Erro SQL em PassagemDAO: " + e.getMessage()); }
        return false;
    }

    public boolean excluir(long id) {
        String sql = "DELETE FROM passagens WHERE id_passagem = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setInt(2, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { AppLogger.warn("PassagemDAO", "Erro SQL em PassagemDAO: " + e.getMessage()); }
        return false;
    }

    private String getBaseQuery() {
        return "SELECT p.*, pa.nome_passageiro, pa.numero_documento, pa.data_nascimento, " +
               "v.data_viagem, v.data_chegada, ahs.descricao_horario_saida, r.origem, r.destino, " +
               "CONCAT(TO_CHAR(v.data_viagem, 'DD/MM/YYYY'), ' - ', ahs.descricao_horario_saida, ' - ', r.origem, ' - ', r.destino, ' - ', e.nome) AS str_viagem, " +
               "an.nome_nacionalidade " +
               "FROM passagens p " +
               "JOIN passageiros pa ON p.id_passageiro = pa.id_passageiro " +
               "JOIN viagens v ON p.id_viagem = v.id_viagem " +
               "JOIN rotas r ON v.id_rota = r.id " + 
               "JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
               "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
               "LEFT JOIN aux_nacionalidades an ON pa.id_nacionalidade = an.id_nacionalidade ";
    }

    private boolean detectarTemDataChegada(ResultSet rs) throws SQLException {
        java.sql.ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if ("data_chegada".equalsIgnoreCase(meta.getColumnName(i))) return true;
        }
        return false;
    }

    // DR207: variavel local de thread em vez de campo de instancia (thread-safe)
    private final ThreadLocal<Boolean> temDataChegadaTL = ThreadLocal.withInitial(() -> false);

    private Passagem mapResultSetToPassagem(ResultSet rs) throws SQLException {
        Passagem p = new Passagem();
        p.setId(rs.getLong("id_passagem"));
        p.setNumBilhete(rs.getInt("numero_bilhete"));
        p.setIdPassageiro(rs.getLong("id_passageiro"));
        p.setIdViagem(rs.getLong("id_viagem"));
        java.sql.Date dtEmissaoP = rs.getDate("data_emissao");
        p.setDataEmissao(dtEmissaoP != null ? dtEmissaoP.toLocalDate() : null);
        p.setAssento(rs.getString("assento"));
        p.setIdAcomodacao(rs.getObject("id_acomodacao") != null ? rs.getInt("id_acomodacao") : null);
        p.setIdRota(rs.getLong("id_rota"));
        p.setIdTipoPassagem(rs.getObject("id_tipo_passagem") != null ? rs.getInt("id_tipo_passagem") : null);
        p.setIdAgente(rs.getObject("id_agente") != null ? rs.getInt("id_agente") : null);
        p.setRequisicao(rs.getString("numero_requisicao"));
        p.setValorAlimentacao(rs.getBigDecimal("valor_alimentacao"));
        p.setValorTransporte(rs.getBigDecimal("valor_transporte"));
        p.setValorCargas(rs.getBigDecimal("valor_cargas"));
        p.setValorDescontoTarifa(rs.getBigDecimal("valor_desconto_tarifa"));
        p.setValorTotal(rs.getBigDecimal("valor_total"));
        p.setValorDesconto(rs.getBigDecimal("valor_desconto_geral"));
        p.setValorAPagar(rs.getBigDecimal("valor_a_pagar"));
        p.setValorPago(rs.getBigDecimal("valor_pago"));
        p.setTroco(rs.getBigDecimal("troco"));
        p.setDevedor(rs.getBigDecimal("valor_devedor"));
        
        p.setValorPagamentoDinheiro(rs.getBigDecimal("valor_pagamento_dinheiro"));
        p.setValorPagamentoPix(rs.getBigDecimal("valor_pagamento_pix"));
        p.setValorPagamentoCartao(rs.getBigDecimal("valor_pagamento_cartao"));
        
        p.setIdFormaPagamento(rs.getObject("id_forma_pagamento") != null ? rs.getInt("id_forma_pagamento") : null);
        
        p.setIdCaixa(rs.getObject("id_caixa") != null ? rs.getInt("id_caixa") : null);
        p.setIdUsuarioEmissor(rs.getObject("id_usuario_emissor") != null ? rs.getInt("id_usuario_emissor") : null);
        p.setStatusPassagem(rs.getString("status_passagem"));
        p.setObservacoes(rs.getString("observacoes"));
        p.setIdHorarioSaida(rs.getObject("id_horario_saida") != null ? rs.getInt("id_horario_saida") : null);
        p.setNomePassageiro(rs.getString("nome_passageiro"));
        p.setNumeroDoc(rs.getString("numero_documento"));
        if(rs.getDate("data_nascimento") != null) p.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
        p.setNacionalidade(rs.getString("nome_nacionalidade"));
        if(rs.getDate("data_viagem") != null) p.setDataViagem(rs.getDate("data_viagem").toLocalDate());
        
        // data_chegada e opcional (presente apenas em queries com JOIN viagens)
        if (temDataChegadaTL.get()) {
            if (rs.getDate("data_chegada") != null) {
                p.setStrViagem(p.getStrViagem() + "||" + rs.getDate("data_chegada").toString());
            }
        }

        p.setDescricaoHorarioSaida(rs.getString("descricao_horario_saida"));
        p.setOrigem(rs.getString("origem"));
        p.setDestino(rs.getString("destino"));
        
        p.setAcomodacao(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", p.getIdAcomodacao()));
        p.setTipoPassagemAux(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", p.getIdTipoPassagem()));
        p.setAgenteAux(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_agentes", "nome_agente", "id_agente", p.getIdAgente()));
        p.setFormaPagamento(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_formas_pagamento", "nome_forma_pagamento", "id_forma_pagamento", p.getIdFormaPagamento()));
        p.setCaixa(auxiliaresDAO.buscarNomeAuxiliarPorId("caixas", "nome_caixa", "id_caixa", p.getIdCaixa()));
        return p;
    }

    public List<Passagem> listarTodos() {
        List<Passagem> passagens = new ArrayList<>();
        // #DB030: pre-carregar caches auxiliares para evitar N+1 no cold-start
        try { auxiliaresDAO.preCarregarCachesPassagem(); } catch (SQLException e) { /* cache opcional */ }
        String sql = getBaseQuery() + "WHERE p.empresa_id = ? ORDER BY p.data_emissao DESC, p.numero_bilhete DESC LIMIT 500";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
            temDataChegadaTL.set(detectarTemDataChegada(rs));
            while (rs.next()) passagens.add(mapResultSetToPassagem(rs));
            }
        } catch (SQLException e) { AppLogger.warn("PassagemDAO", "Erro SQL em PassagemDAO: " + e.getMessage()); }
        return passagens;
    }

    public List<Passagem> listarPorViagem(long idViagem) {
        List<Passagem> passagens = new ArrayList<>();
        try { auxiliaresDAO.preCarregarCachesPassagem(); } catch (SQLException e) { /* cache opcional */ }
        String sql = getBaseQuery() + " WHERE p.empresa_id = ? AND p.id_viagem = ? ORDER BY pa.nome_passageiro";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setLong(2, idViagem);
            try (ResultSet rs = stmt.executeQuery()) {
                temDataChegadaTL.set(detectarTemDataChegada(rs));
            while (rs.next()) passagens.add(mapResultSetToPassagem(rs));
            }
        } catch (SQLException e) { AppLogger.warn("PassagemDAO", "Erro SQL em PassagemDAO: " + e.getMessage()); }
        return passagens;
    }

    public List<Passagem> filtrarRelatorio(LocalDate dataInicio, LocalDate dataFim, String viagemStr, String rotaStr,
                                           String tipoPagamento, String caixa, String agente, String tipoPassagem,
                                           String nomePassageiro, String statusPagamento) throws SQLException {
        // DP092: pre-carregar caches auxiliares para evitar N+1 no cold-start
        try { auxiliaresDAO.preCarregarCachesPassagem(); } catch (SQLException e) { /* cache opcional */ }
        List<Passagem> passagens = new ArrayList<>();
        // #088: LEFT JOINs extras para filtrar por agente/tipo/pagamento/caixa no SQL (antes era removeIf em Java)
        StringBuilder sqlBuilder = new StringBuilder(getBaseQuery());
        sqlBuilder.append("LEFT JOIN aux_agentes _ag ON p.id_agente = _ag.id_agente ");
        sqlBuilder.append("LEFT JOIN aux_tipos_passagem _tp ON p.id_tipo_passagem = _tp.id_tipo_passagem ");
        sqlBuilder.append("LEFT JOIN aux_formas_pagamento _fp ON p.id_forma_pagamento = _fp.id_forma_pagamento ");
        sqlBuilder.append("LEFT JOIN caixas _cx ON p.id_caixa = _cx.id_caixa ");
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        // Multi-tenant: sempre filtrar por empresa
        conditions.add("p.empresa_id = ?");
        params.add(DAOUtils.empresaId());

        // Filtro por viagem (extrair ID do inicio da string "16 - 04/03/2026 - ...")
        if (viagemStr != null && !viagemStr.trim().isEmpty()) {
            try {
                String idPart = viagemStr.split(" - ")[0].trim();
                long idViagem = Long.parseLong(idPart);
                conditions.add("p.id_viagem = ?");
                params.add(idViagem);
            } catch (Exception e) { /* ignore parse error */ }
        }

        if (dataInicio != null) { conditions.add("v.data_viagem >= ?"); params.add(Date.valueOf(dataInicio)); }
        if (dataFim != null) { conditions.add("v.data_viagem <= ?"); params.add(Date.valueOf(dataFim)); }
        if (nomePassageiro != null && !nomePassageiro.trim().isEmpty()) {
            conditions.add("pa.nome_passageiro ILIKE ?"); params.add("%" + nomePassageiro + "%");
        }

        if (statusPagamento != null && !statusPagamento.equals("Todos")) {
            if (statusPagamento.equals("Falta Pagar")) conditions.add("p.valor_devedor > 0.01");
            else if (statusPagamento.equals("Pagos")) conditions.add("p.valor_devedor <= 0.01");
        }

        // #088: filtros que antes eram removeIf em Java — agora no SQL
        if (agente != null && !agente.trim().isEmpty()) {
            conditions.add("_ag.nome_agente ILIKE ?"); params.add(agente);
        }
        if (tipoPassagem != null && !tipoPassagem.trim().isEmpty()) {
            conditions.add("_tp.nome_tipo_passagem ILIKE ?"); params.add(tipoPassagem);
        }
        if (rotaStr != null && !rotaStr.trim().isEmpty()) {
            conditions.add("(r.origem || ' - ' || r.destino) ILIKE ?"); params.add(rotaStr);
        }
        if (tipoPagamento != null && !tipoPagamento.trim().isEmpty()) {
            conditions.add("_fp.nome_forma_pagamento ILIKE ?"); params.add(tipoPagamento);
        }
        if (caixa != null && !caixa.trim().isEmpty()) {
            conditions.add("_cx.nome_caixa ILIKE ?"); params.add(caixa);
        }

        if (!conditions.isEmpty()) sqlBuilder.append(" WHERE ").append(String.join(" AND ", conditions));
        sqlBuilder.append(" ORDER BY v.data_viagem, pa.nome_passageiro");

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                temDataChegadaTL.set(detectarTemDataChegada(rs));
            while (rs.next()) passagens.add(mapResultSetToPassagem(rs));
            }
        }

        // #088: filtros movidos de Java (removeIf) para SQL — aplicados antes da query acima
        // Os filtros abaixo sao mantidos como fallback defensivo APENAS para campos sem JOIN no getBaseQuery
        // Nota: agente, tipoPassagem, tipoPagamento e caixa agora sao filtrados via SQL (acima)

        return passagens;
    }

    public List<Passagem> listarExtratoPorPassageiro(String nomePassageiro, String status) {
        // #079: pre-carregar caches auxiliares para evitar N+1 no cold-start
        try { auxiliaresDAO.preCarregarCachesPassagem(); } catch (SQLException e) { /* cache opcional */ }
        List<Passagem> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append(getBaseQuery());
        sql.append("WHERE p.empresa_id = ? AND pa.nome_passageiro ILIKE ? ");

        if (status != null && !status.equals("TODOS")) {
            if (status.equals("PENDENTES")) {
                sql.append("AND (p.valor_devedor > 0.01 OR p.valor_pago < p.valor_total) ");
            } else if (status.equals("PAGOS")) {
                sql.append("AND (p.valor_devedor <= 0.01 AND p.valor_pago >= p.valor_total) ");
            }
        }
        sql.append("ORDER BY v.data_viagem DESC");

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setString(2, "%" + nomePassageiro + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                temDataChegadaTL.set(detectarTemDataChegada(rs));
                while (rs.next()) lista.add(mapResultSetToPassagem(rs));
            }
        } catch (SQLException e) { AppLogger.warn("PassagemDAO", "Erro SQL em PassagemDAO: " + e.getMessage()); }
        return lista;
    }
    
    /**
     * #DB007: Quita divida por ID do passageiro (seguro contra homonimos).
     */
    public boolean quitarDividaTotalPassageiroPorId(long idPassageiro) {
        String sql = "UPDATE passagens SET " +
                     "valor_pagamento_dinheiro = COALESCE(valor_pagamento_dinheiro, 0) + (valor_total - valor_pago), " +
                     "valor_pago = valor_total, " +
                     "valor_devedor = 0, " +
                     "status_passagem = 'PAGO' " +
                     "WHERE id_passageiro = ? " +
                     "AND empresa_id = ? " +
                     "AND valor_devedor > 0";
        try (Connection conn = ConexaoBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, idPassageiro);
                    stmt.setInt(2, DAOUtils.empresaId());
                    rows = stmt.executeUpdate();
                }
                conn.commit();
                return rows > 0;
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException re) { AppLogger.warn("PassagemDAO", "Erro no rollback: " + re.getMessage()); }
                throw ex;
            }
        } catch (SQLException e) {
            AppLogger.warn("PassagemDAO", "Erro SQL em PassagemDAO.quitarDividaPorId: " + e.getMessage());
            return false;
        }
    }

    /** @deprecated Use quitarDividaTotalPassageiroPorId(long) para evitar homonimos. */
    public boolean quitarDividaTotalPassageiro(String nomePassageiro) {
        String sql = "UPDATE passagens p SET " +
                     "valor_pagamento_dinheiro = COALESCE(valor_pagamento_dinheiro, 0) + (valor_total - valor_pago), " +
                     "valor_pago = valor_total, " +
                     "valor_devedor = 0, " +
                     "status_passagem = 'PAGO' " +
                     "FROM passageiros pa " +
                     "WHERE p.id_passageiro = pa.id_passageiro " +
                     "AND TRIM(pa.nome_passageiro) ILIKE TRIM(?) " +
                     "AND p.empresa_id = ? " +
                     "AND p.valor_devedor > 0";
        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            int rows;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nomePassageiro);
                stmt.setInt(2, DAOUtils.empresaId());
                rows = stmt.executeUpdate();
            }

            // #020: Fallback LIKE removido — busca exata apenas (TRIM+ILIKE).
            // Para quitacao segura, usar metodo por ID do passageiro.

            conn.commit();
            return rows > 0;
        } catch (SQLException e) {
            AppLogger.warn("PassagemDAO", "Erro SQL em PassagemDAO.quitarDivida: " + e.getMessage());
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { AppLogger.warn("PassagemDAO", "Erro no rollback: " + ex.getMessage()); } }
            return false;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { AppLogger.warn("PassagemDAO", "Erro: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); } }
        }
    }
}