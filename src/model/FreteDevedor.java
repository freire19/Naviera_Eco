package model;

public class FreteDevedor {
    private String total, baixado, devedor, numFrete;

    public FreteDevedor(double total, double baixado, double devedor, String numFrete) {
        this.total = String.format("R$ %.2f", total);
        this.baixado = String.format("R$ %.2f", baixado);
        this.devedor = String.format("R$ %.2f", devedor);
        this.numFrete = numFrete;
    }

    public String getTotal() { return total; }
    public String getBaixado() { return baixado; }
    public String getDevedor() { return devedor; }
    public String getNumFrete() { return numFrete; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FreteDevedor that = (FreteDevedor) o;
        return java.util.Objects.equals(numFrete, that.numFrete);
    }
    @Override
    public int hashCode() { return java.util.Objects.hashCode(numFrete); }
}
