package service;

import dao.ConexaoBD;
import dao.DAOUtils;
import dao.FreteDAO;
import dao.FreteDAO.FreteData;
import dao.FreteDAO.FreteItemData;
import util.AppLogger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Camada de servico para operacoes de Frete.
 * DM057: extraido de CadastroFreteController para separar logica de negocio da UI.
 *
 * Responsabilidades:
 * - Gerenciar transacoes (commit/rollback)
 * - Orquestrar DAO calls
 * - Calcular valores derivados
 *
 * NAO faz: manipulacao de UI, alerts, acesso a componentes JavaFX.
 */
public class FreteService {

    private final FreteDAO freteDAO = new FreteDAO();

    /**
     * Resultado de uma operacao de salvar/alterar frete.
     */
    public static class ResultadoFrete {
        public final boolean sucesso;
        public final long numeroFrete;
        public final String mensagem;
        public final boolean isNovo;

        public ResultadoFrete(boolean sucesso, long numeroFrete, String mensagem, boolean isNovo) {
            this.sucesso = sucesso;
            this.numeroFrete = numeroFrete;
            this.mensagem = mensagem;
            this.isNovo = isNovo;
        }

        public static ResultadoFrete erro(String mensagem) {
            return new ResultadoFrete(false, 0, mensagem, false);
        }
    }

    /**
     * Salva um novo frete ou atualiza um existente, com seus itens, em transacao atomica.
     *
     * @param data      dados do frete (campos do formulario)
     * @param itens     lista de itens do frete
     * @param isNovo    true = INSERT, false = UPDATE
     * @param idViagem  ID da viagem ativa (obrigatorio para novo frete)
     * @return resultado da operacao
     */
    public ResultadoFrete salvarOuAlterar(FreteData data, List<FreteItemData> itens, boolean isNovo, Long idViagem) {
        if (isNovo && idViagem == null) {
            return ResultadoFrete.erro("Nao e possivel salvar um novo frete sem uma Viagem Ativa definida.");
        }
        if (itens == null || itens.isEmpty()) {
            return ResultadoFrete.erro("Adicione pelo menos um item ao frete.");
        }

        data.idViagem = idViagem;

        // Calcular total dos itens
        BigDecimal totalItens = itens.stream()
                .map(i -> i.subtotal != null ? i.subtotal : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.totalItens = totalItens;

        // Se valorFreteCalculado nao foi definido explicitamente, usar totalItens
        if (data.valorFreteCalculado == null || data.valorFreteCalculado.compareTo(BigDecimal.ZERO) == 0) {
            data.valorFreteCalculado = totalItens;
        }

        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            if (isNovo) {
                long numero = freteDAO.gerarNumeroFrete();
                data.idFrete = numero;
                data.numeroFrete = numero;
                freteDAO.inserir(conn, data);
            } else {
                freteDAO.atualizar(conn, data);
            }

            // Salvar itens (delete + re-insert)
            freteDAO.salvarItens(conn, data.idFrete, itens);

            conn.commit();

            String msg = isNovo
                    ? "Frete numero " + data.numeroFrete + " salvo com sucesso!"
                    : "Frete numero " + data.numeroFrete + " alterado com sucesso!";
            return new ResultadoFrete(true, data.numeroFrete, msg, isNovo);

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { AppLogger.error("FreteService", ex.getMessage(), ex); } }
            AppLogger.error("FreteService", e.getMessage(), e);
            return ResultadoFrete.erro("Erro no banco de dados: " + e.getMessage());
        } catch (Exception e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { AppLogger.error("FreteService", ex.getMessage(), ex); } }
            AppLogger.error("FreteService", e.getMessage(), e);
            return ResultadoFrete.erro("Erro inesperado: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { AppLogger.error("FreteService", ex.getMessage(), ex); }
            }
        }
    }
}
