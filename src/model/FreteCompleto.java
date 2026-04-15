package model;

import java.util.List;

/**
 * Frete com dados completos para relatorio de fretes.
 */
public class FreteCompleto {
    public String numeroFrete, remetente, destinatario;
    public double valorTotal, valorPago, valorDevedor;
    public List<FreteItemDetalhe> itens;
}
