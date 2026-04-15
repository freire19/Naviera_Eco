package tests;

import service.FreteService;
import service.FreteService.ResultadoFrete;
import dao.FreteDAO.FreteData;
import dao.FreteDAO.FreteItemData;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Testes unitarios para FreteService.
 * Verifica validacoes de entrada (sem banco de dados).
 */
public class FreteServiceTest {

    private final FreteService service = new FreteService();

    // --- ResultadoFrete.erro ---

    @Test
    public void erro_criaSucessoFalso() {
        ResultadoFrete r = ResultadoFrete.erro("falha qualquer");
        assertFalse(r.sucesso);
    }

    @Test
    public void erro_numeroFreteZero() {
        ResultadoFrete r = ResultadoFrete.erro("falha");
        assertEquals(0, r.numeroFrete);
    }

    @Test
    public void erro_mensagemPreservada() {
        ResultadoFrete r = ResultadoFrete.erro("Mensagem de erro");
        assertEquals("Mensagem de erro", r.mensagem);
    }

    @Test
    public void erro_isNovoFalso() {
        ResultadoFrete r = ResultadoFrete.erro("falha");
        assertFalse(r.isNovo);
    }

    // --- ResultadoFrete construtor completo ---

    @Test
    public void resultadoFrete_construtorCompleto() {
        ResultadoFrete r = new ResultadoFrete(true, 42, "ok", true);
        assertTrue(r.sucesso);
        assertEquals(42, r.numeroFrete);
        assertEquals("ok", r.mensagem);
        assertTrue(r.isNovo);
    }

    // --- salvarOuAlterar: validacao idViagem ---

    @Test
    public void salvarOuAlterar_novoSemViagem_retornaErro() {
        FreteData data = new FreteData();
        List<FreteItemData> itens = criarItensValidos();

        ResultadoFrete r = service.salvarOuAlterar(data, itens, true, null);

        assertFalse(r.sucesso);
        assertTrue(r.mensagem.contains("Viagem Ativa"));
    }

    @Test
    public void salvarOuAlterar_alteracaoSemViagem_naoRetornaErroDeViagem() {
        // Alteracao (isNovo=false) nao exige idViagem — a validacao de viagem so vale para novo
        FreteData data = new FreteData();
        List<FreteItemData> itens = criarItensValidos();

        // Vai falhar no banco (sem conexao), mas NAO deve retornar o erro de viagem
        ResultadoFrete r = service.salvarOuAlterar(data, itens, false, null);

        // Deve falhar por erro de banco, nao por viagem
        assertFalse(r.sucesso);
        assertFalse(r.mensagem.contains("Viagem Ativa"));
    }

    // --- salvarOuAlterar: validacao itens ---

    @Test
    public void salvarOuAlterar_itensNulo_retornaErro() {
        FreteData data = new FreteData();

        ResultadoFrete r = service.salvarOuAlterar(data, null, true, 1L);

        assertFalse(r.sucesso);
        assertTrue(r.mensagem.contains("item"));
    }

    @Test
    public void salvarOuAlterar_itensVazio_retornaErro() {
        FreteData data = new FreteData();
        List<FreteItemData> itens = Collections.emptyList();

        ResultadoFrete r = service.salvarOuAlterar(data, itens, true, 1L);

        assertFalse(r.sucesso);
        assertTrue(r.mensagem.contains("item"));
    }

    @Test
    public void salvarOuAlterar_semViagemPrecedeSemItens() {
        // Quando ambos os erros sao possiveis, viagem e verificada primeiro
        FreteData data = new FreteData();

        ResultadoFrete r = service.salvarOuAlterar(data, Collections.emptyList(), true, null);

        assertFalse(r.sucesso);
        assertTrue(r.mensagem.contains("Viagem Ativa"));
    }

    // --- Helper ---

    private List<FreteItemData> criarItensValidos() {
        List<FreteItemData> itens = new ArrayList<>();
        FreteItemData item = new FreteItemData();
        item.nomeItem = "Caixa";
        item.quantidade = 1;
        item.precoUnitario = new BigDecimal("10.00");
        item.subtotal = new BigDecimal("10.00");
        itens.add(item);
        return itens;
    }
}
