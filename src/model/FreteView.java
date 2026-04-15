package model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * View model para exibicao de frete em TableView.
 */
public class FreteView {
    private final SimpleStringProperty numFrete, remetente, destinatario, viagem, dataViagem, emissao, nominal, devedor, baixado, conferente, status;
    private final SimpleIntegerProperty totalVolumes;

    public FreteView(String nf, String rem, String des, String viaj, String dtViagem, String emi, String nom, String dev, String bai, String conf, String stat, int volumes) {
        this.numFrete = new SimpleStringProperty(nf);
        this.remetente = new SimpleStringProperty(rem);
        this.destinatario = new SimpleStringProperty(des);
        this.viagem = new SimpleStringProperty(viaj);
        this.dataViagem = new SimpleStringProperty(dtViagem);
        this.emissao = new SimpleStringProperty(emi);
        this.nominal = new SimpleStringProperty(nom);
        this.devedor = new SimpleStringProperty(dev);
        this.baixado = new SimpleStringProperty(bai);
        this.conferente = new SimpleStringProperty(conf);
        this.status = new SimpleStringProperty(stat == null ? "PENDENTE" : stat);
        this.totalVolumes = new SimpleIntegerProperty(volumes);
    }

    public String getNumFrete() { return numFrete.get(); }
    public String getRemetente() { return remetente.get(); }
    public String getDestinatario() { return destinatario.get(); }
    public String getViagem() { return viagem.get(); }
    public String getDataViagem() { return dataViagem.get(); }
    public String getEmissao() { return emissao.get(); }
    public String getNominal() { return nominal.get(); }
    public String getDevedor() { return devedor.get(); }
    public String getBaixado() { return baixado.get(); }
    public String getConferente() { return conferente.get(); }
    public String getStatus() { return status.get(); }
    public int getTotalVolumes() { return totalVolumes.get(); }
}
