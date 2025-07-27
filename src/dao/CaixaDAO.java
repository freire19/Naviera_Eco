package dao;

import java.util.Collections;
import java.util.List;

/**
 * Stub de CaixaDAO para compilar sem erros.
 * 
 * Se você, futuramente, quiser reimplementar acesso real ao banco,
 * basta alterar estes métodos para usar sua conexão JDBC/ORM, etc.
 */
public class CaixaDAO {

    /**
     * Retorna lista vazia de caixas.
     * Se precisar, troque por query real ao banco.
     */
    public List<String> listarTodos() {
        return Collections.emptyList();
    }

    /**
     * Stub para inserir um novo caixa.
     * Retorna 'false' para indicar “não implementado”.
     */
    public boolean inserir(String nomeCaixa) {
        // TODO: implementar inserção real no banco, se desejar.
        return false;
    }

    /**
     * Stub para gerar novo ID de caixa.
     * Retorna 0 apenas para não quebrar o frontend.
     */
    public int gerarNovoIdCaixa() {
        // TODO: retornar realmente o próximo ID a partir do banco, se desejar.
        return 0;
    }
}
