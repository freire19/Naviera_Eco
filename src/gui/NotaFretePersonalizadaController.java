package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.List;

public class NotaFretePersonalizadaController {

    @FXML private Label lblRemetente;
    @FXML private Label lblDestinatario;
    @FXML private Label lblConferente;
    @FXML private Label lblRota;
    @FXML private Label lblDataHora;       // data/hora do frete
    @FXML private Label lblPagamento;      // info pagamento
    @FXML private Label lblTotalGeral;     // total
    @FXML private Label lblDataHoraFinal;  // data/hora final impressao
    @FXML private TableView<ItemNota> tabelaItens;
    @FXML private TableColumn<ItemNota,String> colQuant;
    @FXML private TableColumn<ItemNota,String> colItem;
    @FXML private TableColumn<ItemNota,String> colPreco;
    @FXML private TableColumn<ItemNota,String> colTotal;

    @FXML private Button btnImprimirNota;
    @FXML private AnchorPane rootPane;

    private ObservableList<ItemNota> lista = FXCollections.observableArrayList();

    @FXML
    public void initialize(){
        // Propriedades colunas
        colQuant.setCellValueFactory(new PropertyValueFactory<>("quantStr"));
        colItem.setCellValueFactory(new PropertyValueFactory<>("nomeItem"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoStr"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalStr"));

        tabelaItens.setItems(lista);

        btnImprimirNota.setOnAction(e-> imprimirNota());
    }

    /**
     * Preenche dados da tela:
     * @param remetente    ex: "Fulano"
     * @param destinatario ex: "Ciclano"
     * @param conferente   ex: "Conferente A"
     * @param rota         ex: "Rota B"
     * @param dataHora     ex: "10/02/2026 14:50"
     * @param pagamento    ex: "ValorPago=..., Devedor=..."
     * @param total        ex: 345.0
     * @param itens        lista de ItemNota
     */
    public void setDadosNotaFrete(String remetente,
                                  String destinatario,
                                  String conferente,
                                  String rota,
                                  String dataHora,
                                  String pagamento,
                                  double total,
                                  List<ItemNota> itens) {
        lblRemetente.setText("Remetente: " + remetente);
        lblDestinatario.setText("Destinatário: " + destinatario);
        lblConferente.setText("Conferente: " + conferente);
        lblRota.setText("Rota: " + rota);
        lblDataHora.setText("Data/Hora: " + dataHora);

        lblPagamento.setText("Pagamento: " + pagamento);
        lblTotalGeral.setText("TOTAL: R$ " + String.format("%.2f", total));

        // Data/Hora final da impressao
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();
        lblDataHoraFinal.setText("Impresso em: " + agora.toString());

        lista.clear();
        for(ItemNota it : itens) {
            lista.add(it);
        }
    }

    private void imprimirNota(){
        PrinterJob job= PrinterJob.createPrinterJob();
        if(job!=null){
            boolean ok= job.showPrintDialog(rootPane.getScene().getWindow());
            if(ok){
                boolean success= job.printPage(rootPane);
                if(success) {
                    job.endJob();
                }
            }
        }
    }

    // Classe para mostrar na tabela
    public static class ItemNota {
        private int quant;
        private String nomeItem;
        private double preco;

        public ItemNota(int q, String nome, double p){
            quant = q;
            nomeItem = nome;
            preco = p;
        }

        public int getQuant(){ return quant; }
        public String getNomeItem(){ return nomeItem; }
        public double getPreco(){ return preco; }
        public double getTotal(){ return quant*preco; }

        public String getQuantStr(){ return String.valueOf(quant);}
        public String getPrecoStr(){
            return String.format("R$ %.2f", preco);
        }
        public String getTotalStr(){
            return String.format("R$ %.2f", getTotal());
        }
    }
}
