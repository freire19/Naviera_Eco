package dao;

import model.Passagem;
import gui.util.SessaoUsuario;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Importar AuxiliaresDAO
import dao.AuxiliaresDAO;

/**
 * DAO para operações de persistência da Passagem.
 */
public class PassagemDAO {

    private final AuxiliaresDAO auxiliaresDAO; // Adicionar instância de AuxiliaresDAO

    public PassagemDAO() {
        this.auxiliaresDAO = new AuxiliaresDAO(); // Inicializar
    }

    /**
     * Obtém o próximo número de bilhete disponível no banco de dados.
     * @return O próximo número de bilhete.
     */
    public int obterProximoBilhete() {
        String sql = "SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) + 1 FROM passagens";
        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter próximo número de bilhete: " + e.getMessage());
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Insere uma nova passagem no banco de dados.
     * @param passagem O objeto Passagem a ser inserido.
     * @return true se a inserção for bem-sucedida, false caso contrário.
     */
    public boolean inserir(Passagem passagem) {
        String sql = "INSERT INTO passagens (" +
                     "numero_bilhete, id_passageiro, id_viagem, data_emissao, assento, id_acomodacao, id_rota, id_tipo_passagem, " +
                     "id_agente, numero_requisicao, valor_alimentacao, valor_transporte, valor_cargas, valor_desconto_tarifa, " +
                     "valor_total, valor_desconto_geral, valor_a_pagar, valor_pago, troco, valor_devedor, id_forma_pagamento, " +
                     "id_caixa, id_usuario_emissor, status_passagem, observacoes, id_horario_saida" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, String.valueOf(passagem.getNumBilhete()));
            stmt.setLong(2, passagem.getIdPassageiro());

            if (passagem.getIdViagem() == null || passagem.getIdViagem() == 0L) {
                System.err.println("Erro: ID da viagem na Passagem não foi definido antes de inserir.");
                return false;
            }
            stmt.setLong(3, passagem.getIdViagem());

            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.setString(5, passagem.getAssento());

            // Chamar AuxiliaresDAO
            Integer idAcomodacaoInt = auxiliaresDAO.obterIdAuxiliar("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", passagem.getAcomodacao());
            stmt.setObject(6, idAcomodacaoInt); // idAcomodacaoInt já é Integer, usar setObject é seguro

            if (passagem.getIdRota() == null || passagem.getIdRota() == 0L) {
                System.err.println("Erro: ID da rota na Passagem não foi definido antes de inserir.");
                return false;
            }
            stmt.setLong(7, passagem.getIdRota());

            // Chamar AuxiliaresDAO
            Integer idTipoPassagemInt = auxiliaresDAO.obterIdAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", passagem.getTipoPassagemAux());
            stmt.setObject(8, idTipoPassagemInt); // idTipoPassagemInt já é Integer

            Integer idAgenteInt = auxiliaresDAO.obterIdAuxiliar("aux_agentes", "nome_agente", "id_agente", passagem.getAgenteAux());
            stmt.setObject(9, idAgenteInt); // idAgenteInt já é Integer

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

            // Chamar AuxiliaresDAO
            Integer idFormaPagamentoInt = auxiliaresDAO.obterIdAuxiliar("aux_formas_pagamento", "nome_forma_pagamento", "id_forma_pagamento", passagem.getFormaPagamento());
            stmt.setObject(21, idFormaPagamentoInt); // idFormaPagamentoInt já é Integer

            Integer idCaixaInt = auxiliaresDAO.obterIdAuxiliar("caixas", "nome_caixa", "id_caixa", passagem.getCaixa());
            stmt.setObject(22, idCaixaInt); // idCaixaInt já é Integer

            Integer idUsuarioEmissor; // Alterado para Integer para corresponder ao model
            Integer sessaoUserId = SessaoUsuario.isUsuarioLogado() ? SessaoUsuario.getUsuarioLogado().getId() : null;
            if (sessaoUserId != null && sessaoUserId > 0) {
                idUsuarioEmissor = sessaoUserId;
            } else {
                System.err.println("Aviso: Nenhum usuário logado na sessão ou ID de usuário nulo. Usando ID de emissor padrão 2.");
                idUsuarioEmissor = 2; // Mantido como Integer
            }
            stmt.setObject(23, idUsuarioEmissor); // setObject é seguro para Integer

            stmt.setString(24, passagem.getStatusPassagem());
            stmt.setString(25, passagem.getObservacoes());
            // Conversão segura de Long para Integer aqui, se idHorarioSaida no model for Long e no DB for Integer
            // Ou se no model for Integer e no DB for Integer, apenas usar o valor direto
            // Pelo model.Passagem.java que ajustamos, idHorarioSaida é Integer.
            stmt.setObject(26, passagem.getIdHorarioSaida()); // setObject é seguro para Integer


            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        passagem.setId(rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir passagem: " + e.getMessage());
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) {
                System.err.println("Provável violação de chave única (bilhete já existente).");
            }
        } catch (Exception e) {
            System.err.println("Erro inesperado ao inserir passagem: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Atualiza uma passagem existente no banco de dados.
     * @param passagem O objeto Passagem a ser atualizado.
     * @return true se a atualização for bem-sucedida, false caso contrário.
     */
    public boolean atualizar(Passagem passagem) {
        String sql = "UPDATE passagens SET " +
                     "numero_bilhete = ?, id_passageiro = ?, id_viagem = ?, data_emissao = ?, assento = ?, id_acomodacao = ?, id_rota = ?, id_tipo_passagem = ?, " +
                     "id_agente = ?, numero_requisicao = ?, valor_alimentacao = ?, valor_transporte = ?, valor_cargas = ?, valor_desconto_tarifa = ?, " +
                     "valor_total = ?, valor_desconto_geral = ?, valor_a_pagar = ?, valor_pago = ?, troco = ?, valor_devedor = ?, id_forma_pagamento = ?, " +
                     "id_caixa = ?, id_usuario_emissor = ?, status_passagem = ?, observacoes = ?, data_ultima_atualizacao = CURRENT_TIMESTAMP, id_horario_saida = ? " +
                     "WHERE id_passagem = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, String.valueOf(passagem.getNumBilhete()));
            stmt.setLong(2, passagem.getIdPassageiro());

            if (passagem.getIdViagem() == null || passagem.getIdViagem() == 0L) {
                System.err.println("Erro: ID da viagem na Passagem não foi definido antes de atualizar.");
                return false;
            }
            stmt.setLong(3, passagem.getIdViagem());

            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.setString(5, passagem.getAssento());
            // Chamar AuxiliaresDAO
            Integer idAcomodacaoInt = auxiliaresDAO.obterIdAuxiliar("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", passagem.getAcomodacao());
            stmt.setObject(6, idAcomodacaoInt);

            if (passagem.getIdRota() == null || passagem.getIdRota() == 0L) {
                System.err.println("Erro: ID da rota na Passagem não foi definido antes de atualizar.");
                return false;
            }
            stmt.setLong(7, passagem.getIdRota());

            // Chamar AuxiliaresDAO
            Integer idTipoPassagemInt = auxiliaresDAO.obterIdAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", passagem.getTipoPassagemAux());
            stmt.setObject(8, idTipoPassagemInt);
            Integer idAgenteInt = auxiliaresDAO.obterIdAuxiliar("aux_agentes", "nome_agente", "id_agente", passagem.getAgenteAux());
            stmt.setObject(9, idAgenteInt);

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

            // Chamar AuxiliaresDAO
            Integer idFormaPagamentoInt = auxiliaresDAO.obterIdAuxiliar("aux_formas_pagamento", "nome_forma_pagamento", "id_forma_pagamento", passagem.getFormaPagamento());
            stmt.setObject(21, idFormaPagamentoInt);

            Integer idCaixaInt = auxiliaresDAO.obterIdAuxiliar("caixas", "nome_caixa", "id_caixa", passagem.getCaixa());
            stmt.setObject(22, idCaixaInt);

            Integer idUsuarioEmissor; // Alterado para Integer para corresponder ao model
            Integer sessaoUserId = SessaoUsuario.isUsuarioLogado() ? SessaoUsuario.getUsuarioLogado().getId() : null;
            if (sessaoUserId != null && sessaoUserId > 0) {
                idUsuarioEmissor = sessaoUserId;
            } else {
                System.err.println("Aviso: Nenhum usuário logado na sessão ao atualizar. Mantendo o ID original ou padrão.");
                idUsuarioEmissor = (passagem.getIdUsuarioEmissor() != null && passagem.getIdUsuarioEmissor() > 0) ? passagem.getIdUsuarioEmissor() : 2; // Mantido como Integer
            }
            stmt.setObject(23, idUsuarioEmissor);

            stmt.setString(24, passagem.getStatusPassagem());
            stmt.setString(25, passagem.getObservacoes());
            stmt.setObject(26, passagem.getIdHorarioSaida()); // setObject é seguro para Integer
            stmt.setLong(27, passagem.getId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar passagem: " + e.getMessage());
            e.printStackTrace();
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) {
                System.err.println("Provável violação de chave única (bilhete já existente).");
            }
        } catch (Exception e) {
            System.err.println("Erro inesperado ao atualizar passagem: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Exclui uma passagem do banco de dados.
     * @param id O ID da passagem a ser excluída.
     * @return true se a exclusão for bem-sucedida, false caso contrário.
     */
    public boolean excluir(long id) {
        String sql = "DELETE FROM passagens WHERE id_passagem = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir passagem: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Mapeia um ResultSet para um objeto Passagem.
     * Este método centraliza a lógica de mapeamento para evitar duplicação.
     */
    private Passagem mapResultSetToPassagem(ResultSet rs) throws SQLException {
        Passagem passage = new Passagem();

        // Conversões seguras para Long
        Object idPassagemObj = rs.getObject("id_passagem");
        passage.setId(idPassagemObj != null ? ((Number) idPassagemObj).longValue() : null);

        Object idViagemObj = rs.getObject("id_viagem");
        passage.setIdViagem(idViagemObj != null ? ((Number) idViagemObj).longValue() : null);

        Object idRotaObj = rs.getObject("id_rota");
        passage.setIdRota(idRotaObj != null ? ((Number) idRotaObj).longValue() : null);

        Object idPassageiroObj = rs.getObject("id_passageiro");
        passage.setIdPassageiro(idPassageiroObj != null ? ((Number) idPassageiroObj).longValue() : null);

        // Conversões seguras para Integer
        Object idHorarioSaidaObj = rs.getObject("id_horario_saida");
        passage.setIdHorarioSaida(idHorarioSaidaObj != null ? ((Number) idHorarioSaidaObj).intValue() : null);

        Object idTipoPassagemObj = rs.getObject("id_tipo_passagem");
        passage.setIdTipoPassagem(idTipoPassagemObj != null ? ((Number) idTipoPassagemObj).intValue() : null);

        Object idAgenteObj = rs.getObject("id_agente");
        passage.setIdAgente(idAgenteObj != null ? ((Number) idAgenteObj).intValue() : null);

        Object idAcomodacaoObj = rs.getObject("id_acomodacao");
        passage.setIdAcomodacao(idAcomodacaoObj != null ? ((Number) idAcomodacaoObj).intValue() : null);

        Object idFormaPagamentoObj = rs.getObject("id_forma_pagamento");
        passage.setIdFormaPagamento(idFormaPagamentoObj != null ? ((Number) idFormaPagamentoObj).intValue() : null);

        Object idCaixaObj = rs.getObject("id_caixa");
        passage.setIdCaixa(idCaixaObj != null ? ((Number) idCaixaObj).intValue() : null);


        // Atributos diretos ou sem necessidade de conversão complexa
        passage.setNumBilhete(rs.getInt("numero_bilhete")); // int, não precisa de Object e Number
        passage.setNomePassageiro(rs.getString("nome_passageiro"));
        passage.setNumeroDoc(rs.getString("numero_documento"));
        passage.setDataNascimento(rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null);

        passage.setDataViagem(rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toLocalDate() : null);
        passage.setDescricaoHorarioSaida(rs.getString("descricao_horario_saida"));

        passage.setOrigem(rs.getString("origem"));
        passage.setDestino(rs.getString("destino"));

        passage.setAssento(rs.getString("assento"));
        passage.setValorTotal(rs.getBigDecimal("valor_total"));
        passage.setValorDesconto(rs.getBigDecimal("valor_desconto_geral"));
        passage.setValorAPagar(rs.getBigDecimal("valor_a_pagar"));
        passage.setValorPago(rs.getBigDecimal("valor_pago"));
        passage.setTroco(rs.getBigDecimal("troco"));
        passage.setDevedor(rs.getBigDecimal("valor_devedor"));
        passage.setStatusPassagem(rs.getString("status_passagem"));

        // Preenchendo os nomes auxiliares usando os IDs recém-mapeados
        passage.setTipoPassagemAux(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", passage.getIdTipoPassagem()));
        passage.setAgenteAux(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_agentes", "nome_agente", "id_agente", passage.getIdAgente()));
        passage.setAcomodacao(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", passage.getIdAcomodacao()));
        passage.setFormaPagamento(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_formas_pagamento", "nome_forma_pagamento", "id_forma_pagamento", passage.getIdFormaPagamento()));
        passage.setCaixa(auxiliaresDAO.buscarNomeAuxiliarPorId("caixas", "nome_caixa", "id_caixa", passage.getIdCaixa()));


        passage.setValorAlimentacao(rs.getBigDecimal("valor_alimentacao"));
        passage.setValorTransporte(rs.getBigDecimal("valor_transporte"));
        passage.setValorCargas(rs.getBigDecimal("valor_cargas"));
        passage.setValorDescontoTarifa(rs.getBigDecimal("valor_desconto_tarifa"));
        passage.setRequisicao(rs.getString("numero_requisicao"));
        String nacionalidade = rs.getString("nome_nacionalidade");
        passage.setNacionalidade(nacionalidade != null ? nacionalidade : "N/A");

        passage.setStrViagem(rs.getString("str_viagem"));
        // id_viagem e id_rota já foram mapeados acima usando Object/Number
        // passage.setIdViagem(rs.getLong("id_viagem"));
        // passage.setIdRota(rs.getLong("id_rota"));

        return passage;
    }


    /**
     * Lista todas as passagens do banco de dados.
     * @return Uma lista de objetos Passagem.
     */
    public List<Passagem> listarTodos() { // PUBLICO, deveria ser visível
        List<Passagem> passagens = new ArrayList<>();
        String sql = "SELECT p.id_passagem, p.numero_bilhete, pa.nome_passageiro, pa.numero_documento, pa.data_nascimento, " +
                     "v.data_viagem, ahs.descricao_horario_saida, r.origem, r.destino, p.assento, p.valor_total, p.valor_desconto_geral, p.valor_a_pagar, " +
                     "p.valor_pago, p.troco, p.valor_devedor, p.status_passagem, " +
                     "tp.nome_tipo_passagem, ag.nome_agente, ac.nome_acomodacao, fp.nome_forma_pagamento, cx.nome_caixa, " +
                     "p.valor_alimentacao, p.valor_transporte, p.valor_cargas, p.valor_desconto_tarifa, " +
                     "p.numero_requisicao, an.nome_nacionalidade, " +
                     "v.id_viagem, v.id_rota, v.id_horario_saida, " +
                     "CONCAT(TO_CHAR(v.data_viagem, 'DD/MM/YYYY'), ' - ', ahs.descricao_horario_saida, ' - ', r.origem, ' - ', r.destino, ' - ', e.nome) AS str_viagem, " +
                     "p.id_tipo_passagem, p.id_agente, p.id_acomodacao, p.id_forma_pagamento, p.id_caixa, p.id_passageiro " + // Incluir id_passageiro para mapResultSetToPassagem
                     "FROM passagens p " +
                     "JOIN passageiros pa ON p.id_passageiro = pa.id_passageiro " +
                     "JOIN viagens v ON p.id_viagem = v.id_viagem " +
                     "JOIN rotas r ON v.id_rota = r.id " +
                     "JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "LEFT JOIN aux_tipos_passagem tp ON p.id_tipo_passagem = tp.id_tipo_passagem " +
                     "LEFT JOIN aux_agentes ag ON p.id_agente = ag.id_agente " +
                     "LEFT JOIN aux_acomodacoes ac ON p.id_acomodacao = ac.id_acomodacao " +
                     "LEFT JOIN aux_formas_pagamento fp ON p.id_forma_pagamento = fp.id_forma_pagamento " +
                     "LEFT JOIN caixas cx ON p.id_caixa = cx.id_caixa " +
                     "LEFT JOIN aux_nacionalidades an ON pa.id_nacionalidade = an.id_nacionalidade " +
                     "ORDER BY p.data_emissao DESC, p.numero_bilhete DESC";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                passagens.add(mapResultSetToPassagem(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar todas as passagens: " + e.getMessage());
            e.printStackTrace();
        }
        return passagens;
    }

    /**
     * Filtra passagens com base em um modo de pesquisa e texto.
     * @param modo O modo de pesquisa (ex: "Número Bilhete", "Passageiro").
     * @param texto O texto a ser pesquisado.
     * @return Uma lista de objetos Passagem que correspondem ao filtro.
     */
    public List<Passagem> filtrar(String modo, String texto) { // PUBLICO, deveria ser visível
        List<Passagem> passagens = new ArrayList<>();
        String sqlBase = "SELECT p.id_passagem, p.numero_bilhete, pa.nome_passageiro, pa.numero_documento, pa.data_nascimento, " +
                         "v.data_viagem, ahs.descricao_horario_saida, r.origem, r.destino, p.assento, p.valor_total, p.valor_desconto_geral, p.valor_a_pagar, " +
                         "p.valor_pago, p.troco, p.valor_devedor, p.status_passagem, " +
                         "tp.nome_tipo_passagem, ag.nome_agente, ac.nome_acomodacao, fp.nome_forma_pagamento, cx.nome_caixa, " +
                         "p.valor_alimentacao, p.valor_transporte, p.valor_cargas, p.valor_desconto_tarifa, " +
                         "p.numero_requisicao, an.nome_nacionalidade, " +
                         "v.id_viagem, v.id_rota, v.id_horario_saida, " +
                         "CONCAT(TO_CHAR(v.data_viagem, 'DD/MM/YYYY'), ' - ', ahs.descricao_horario_saida, ' - ', r.origem, ' - ', r.destino, ' - ', e.nome) AS str_viagem, " +
                         "p.id_tipo_passagem, p.id_agente, p.id_acomodacao, p.id_forma_pagamento, p.id_caixa, p.id_passageiro " + // Incluir id_passageiro
                         "FROM passagens p " +
                         "JOIN passageiros pa ON p.id_passageiro = pa.id_passageiro " +
                         "JOIN viagens v ON p.id_viagem = v.id_viagem " +
                         "JOIN rotas r ON v.id_rota = r.id " +
                         "JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                         "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                         "LEFT JOIN aux_tipos_passagem tp ON p.id_tipo_passagem = tp.id_tipo_passagem " +
                         "LEFT JOIN aux_agentes ag ON p.id_agente = ag.id_agente " +
                         "LEFT JOIN aux_acomodacoes ac ON p.id_acomodacao = ac.id_acomodacao " +
                         "LEFT JOIN aux_formas_pagamento fp ON p.id_forma_pagamento = fp.id_forma_pagamento " +
                         "LEFT JOIN caixas cx ON p.id_caixa = cx.id_caixa " +
                         "LEFT JOIN aux_nacionalidades an ON pa.id_nacionalidade = an.id_nacionalidade ";

        String whereClause = "";
        String param = "%" + texto.toLowerCase() + "%";

        switch (modo) {
            case "Número Bilhete":
                whereClause = "WHERE CAST(p.numero_bilhete AS TEXT) ILIKE ?";
                break;
            case "Passageiro":
                whereClause = "WHERE LOWER(pa.nome_passageiro) ILIKE ?";
                break;
            case "Nº Documento":
                whereClause = "WHERE LOWER(pa.numero_documento) ILIKE ?";
                break;
            case "Data Partida":
                whereClause = "WHERE TO_CHAR(v.data_viagem, 'DD/MM/YYYY') ILIKE ?";
                break;
            default:
                return listarTodos();
        }

        String sql = sqlBase + whereClause + " ORDER BY p.data_emissao DESC, p.numero_bilhete DESC";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    passagens.add(mapResultSetToPassagem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao filtrar passagens: " + e.getMessage());
            e.printStackTrace();
        }
        return passagens;
    }
}