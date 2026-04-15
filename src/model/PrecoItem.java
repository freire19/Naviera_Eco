package model;

import java.math.BigDecimal;

/**
 * Item de preco para tabela de precos de encomenda/frete.
 */
public class PrecoItem {
    public String desc;
    public String un;
    public BigDecimal val;

    public PrecoItem(String d, String u, BigDecimal v) {
        desc = d;
        un = u;
        val = v;
    }
}
