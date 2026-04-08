package gui;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import dao.ConexaoBD;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.ItemFrete; 

public class TabelaPrecoFreteController implements Initializable {

    @FXML private TextField txtDescricao;
    @FXML private TextField txtPrecoNormal;
    @FXML private TextField txtPrecoDesconto;
    @FXML private Label lblStatus; 
    
    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private Button btnImprimir;
    @FXML private Button btnSair;

    @FXML private TableView<ItemFrete> tabelaItens;
    @FXML private TableColumn<ItemFrete, String> colDescricao;
    @FXML private TableColumn<ItemFrete, String> colPrecoNormal;
    @FXML private TableColumn<ItemFrete, String> colPrecoDesconto;

    private ObservableList<ItemFrete> listaItens;
    private ItemFrete itemSelecionado = null;

    // Controle para saber se deve fechar a janela ao salvar
    private boolean fecharAutomaticamente = false;

    // Formatadores
    private final DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR")));
    private final DateTimeFormatter dtfDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listaItens = FXCollections.observableArrayList();
        configurarTabela();
        carregarDados();
        
        configurarMascaraMoeda(txtPrecoNormal);
        configurarMascaraMoeda(txtPrecoDesconto);
        limparCampos(); 
    }

    private void configurarTabela() {
        colDescricao.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomeItem()));
        
        colPrecoNormal.setCellValueFactory(data -> 
            new SimpleStringProperty("R$ " + df.format(data.getValue().getPrecoUnitarioPadrao())));
        
        colPrecoDesconto.setCellValueFactory(data -> 
            new SimpleStringProperty("R$ " + df.format(data.getValue().getPrecoUnitarioDesconto())));

        tabelaItens.setItems(listaItens);

        tabelaItens.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                itemSelecionado = newSelection;
                preencherCampos(itemSelecionado);
                btnExcluir.setDisable(false);
                btnSalvar.setText("ATUALIZAR"); 
                if(lblStatus != null) lblStatus.setText("Modo: Editando " + itemSelecionado.getNomeItem());
            }
        });
    }

    private void carregarDados() {
        listaItens.clear();
        String sql = "SELECT * FROM itens_frete_padrao ORDER BY nome_item";
        
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_item_frete"); 
                String nome = rs.getString("nome_item");
                String desc = rs.getString("descricao");
                if(desc == null) desc = "";
                
                BigDecimal pNormal = rs.getBigDecimal("preco_unitario_padrao");
                BigDecimal pDesc = rs.getBigDecimal("preco_unitario_desconto");

                ItemFrete item = new ItemFrete(id, nome, desc, "UN", pNormal, pDesc, true);
                listaItens.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro ao carregar", "Falha ao buscar itens: \n" + e.getMessage());
        }
    }

    private void preencherCampos(ItemFrete item) {
        txtDescricao.setText(item.getNomeItem());
        
        BigDecimal pn = item.getPrecoUnitarioPadrao();
        BigDecimal pd = item.getPrecoUnitarioDesconto();
        
        txtPrecoNormal.setText(pn != null ? df.format(pn) : "0,00");
        txtPrecoDesconto.setText(pd != null ? df.format(pd) : "0,00");
    }

    @FXML
    void handleNovo(ActionEvent event) {
        limparCampos(); 
    }

    @FXML
    void handleSalvar(ActionEvent event) {
        if (txtDescricao.getText().trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Aviso", "Informe a descrição do item.");
            txtDescricao.requestFocus();
            return;
        }

        try {
            BigDecimal pNormal = parseMoeda(txtPrecoNormal.getText());
            BigDecimal pDesconto = parseMoeda(txtPrecoDesconto.getText());
            String nome = txtDescricao.getText().trim();

            if (itemSelecionado == null) {
                inserirItem(nome, pNormal, pDesconto);
            } else {
                atualizarItem(itemSelecionado.getIdItemFrete(), nome, pNormal, pDesconto);
            }
            
            limparCampos();
            carregarDados();

            // SE TIVER QUE FECHAR AUTOMATICAMENTE (VINDO DO FRETE), FECHA AQUI
            if (fecharAutomaticamente) {
                // Obtém a janela atual através do botão salvar e fecha
                Stage stage = (Stage) btnSalvar.getScene().getWindow();
                stage.close();
            }
            
        } catch (ParseException e) {
            showAlert(AlertType.ERROR, "Valor Inválido", "Verifique os preços digitados.");
        }
    }

    private void inserirItem(String nome, BigDecimal normal, BigDecimal desconto) {
        String sql = "INSERT INTO itens_frete_padrao (nome_item, descricao, unidade_medida, preco_unitario_padrao, preco_unitario_desconto, ativo) VALUES (?, ?, 'UN', ?, ?, TRUE)";
        try (Connection conn = ConexaoBD.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, nome); 
            stmt.setBigDecimal(3, normal);
            stmt.setBigDecimal(4, desconto);
            stmt.executeUpdate();
            showAlert(AlertType.INFORMATION, "Sucesso", "Item adicionado com sucesso!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro", "Erro ao inserir: " + e.getMessage());
        }
    }

    private void atualizarItem(int id, String nome, BigDecimal normal, BigDecimal desconto) {
        String sql = "UPDATE itens_frete_padrao SET nome_item = ?, descricao = ?, preco_unitario_padrao = ?, preco_unitario_desconto = ? WHERE id_item_frete = ?";
        try (Connection conn = ConexaoBD.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, nome);
            stmt.setBigDecimal(3, normal);
            stmt.setBigDecimal(4, desconto);
            stmt.setInt(5, id);
            stmt.executeUpdate();
            showAlert(AlertType.INFORMATION, "Sucesso", "Item editado com sucesso!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro", "Erro ao atualizar: " + e.getMessage());
        }
    }

    @FXML
    void handleExcluir(ActionEvent event) {
        if (itemSelecionado == null) return;
        
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Excluir");
        alert.setHeaderText("Deseja excluir: " + itemSelecionado.getNomeItem() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM itens_frete_padrao WHERE id_item_frete = ?";
            try (Connection conn = ConexaoBD.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, itemSelecionado.getIdItemFrete());
                stmt.executeUpdate();
                showAlert(AlertType.INFORMATION, "Sucesso", "Item excluído.");
                limparCampos();
                carregarDados();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Erro", "Não foi possível excluir.\n" + e.getMessage());
            }
        }
    }

    @FXML
    void handleImprimirRelatorio(ActionEvent event) {
        if (listaItens.isEmpty()) {
            showAlert(AlertType.WARNING, "Vazio", "Não há itens para imprimir.");
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Opções de Impressão");
        alert.setHeaderText("Quais colunas de preço você deseja imprimir?");
        alert.setContentText("Selecione uma opção:");

        ButtonType btnAmbos = new ButtonType("Ambos os Preços");
        ButtonType btnSoNormal = new ButtonType("Só Preço Normal");
        ButtonType btnSoDesconto = new ButtonType("Só Preço Desconto");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnAmbos, btnSoNormal, btnSoDesconto, btnCancelar);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() == btnCancelar) {
            return; 
        }

        boolean mostrarNormal = (result.get() == btnAmbos || result.get() == btnSoNormal);
        boolean mostrarDesconto = (result.get() == btnAmbos || result.get() == btnSoDesconto);

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(btnImprimir.getScene().getWindow())) {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            double w = pageLayout.getPrintableWidth() - 20;
            double h = pageLayout.getPrintableHeight();

            VBox pagina = new VBox(5);
            pagina.setPadding(new Insets(20));
            pagina.setPrefSize(w, h);
            pagina.setStyle("-fx-background-color: white;");

            String nomeEmp = "SISTEMA DE FRETE";
            try (Connection conn = ConexaoBD.getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT nome_embarcacao FROM configuracao_empresa LIMIT 1")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) nomeEmp = rs.getString("nome_embarcacao");
            } catch(Exception e) { System.err.println("Erro em TabelaPrecoFreteController.imprimirTabela (empresa): " + e.getMessage()); }

            Label lEmp = new Label(nomeEmp.toUpperCase()); 
            lEmp.setFont(Font.font("Arial", FontWeight.BLACK, 16)); lEmp.setAlignment(Pos.CENTER); lEmp.setMaxWidth(Double.MAX_VALUE);
            
            Label lTit = new Label("TABELA DE PREÇOS DE FRETE"); 
            lTit.setFont(Font.font("Arial", FontWeight.BOLD, 14)); lTit.setAlignment(Pos.CENTER); lTit.setMaxWidth(Double.MAX_VALUE);

            pagina.getChildren().addAll(lEmp, lTit);

            double wDescricao, wPreco;
            
            if (mostrarNormal && mostrarDesconto) {
                wDescricao = w * 0.60;
                wPreco = w * 0.20;
            } else {
                wDescricao = w * 0.70;
                wPreco = w * 0.30;
            }

            HBox header = new HBox(10);
            header.setStyle("-fx-background-color: #0d47a1; -fx-padding: 5;");
            
            Label hDesc = new Label("DESCRIÇÃO"); 
            hDesc.setTextFill(Color.WHITE); 
            hDesc.setPrefWidth(wDescricao); 
            hDesc.setFont(Font.font("System", FontWeight.BOLD, 12));
            header.getChildren().add(hDesc);

            if (mostrarNormal) {
                Label hNorm = new Label("NORMAL (R$)"); 
                hNorm.setTextFill(Color.WHITE); 
                hNorm.setPrefWidth(wPreco); 
                hNorm.setAlignment(Pos.CENTER_RIGHT); 
                hNorm.setFont(Font.font("System", FontWeight.BOLD, 12));
                header.getChildren().add(hNorm);
            }

            if (mostrarDesconto) {
                Label hDescnt = new Label("DESCONTO (R$)"); 
                hDescnt.setTextFill(Color.WHITE); 
                hDescnt.setPrefWidth(wPreco); 
                hDescnt.setAlignment(Pos.CENTER_RIGHT); 
                hDescnt.setFont(Font.font("System", FontWeight.BOLD, 12));
                header.getChildren().add(hDescnt);
            }
            
            pagina.getChildren().add(header);

            int i = 0;
            for(ItemFrete item : listaItens) {
                HBox row = new HBox(10);
                row.setPadding(new Insets(4));
                row.setStyle(i % 2 == 0 ? "-fx-background-color: white;" : "-fx-background-color: #f0f0f0;");
                
                Label cDesc = new Label(item.getNomeItem()); 
                cDesc.setPrefWidth(wDescricao);
                row.getChildren().add(cDesc);

                if (mostrarNormal) {
                    Label cNorm = new Label(df.format(item.getPrecoUnitarioPadrao())); 
                    cNorm.setPrefWidth(wPreco); 
                    cNorm.setAlignment(Pos.CENTER_RIGHT); 
                    cNorm.setTextFill(Color.web("#0d47a1"));
                    row.getChildren().add(cNorm);
                }

                if (mostrarDesconto) {
                    Label cDescnt = new Label(df.format(item.getPrecoUnitarioDesconto())); 
                    cDescnt.setPrefWidth(wPreco); 
                    cDescnt.setAlignment(Pos.CENTER_RIGHT); 
                    cDescnt.setTextFill(Color.web("#2e7d32"));
                    row.getChildren().add(cDescnt);
                }
                
                pagina.getChildren().add(row);
                i++;
            }

            Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
            pagina.getChildren().add(spacer);
            Label lRod = new Label("Impresso em: " + LocalDateTime.now().format(dtfDataHora));
            lRod.setFont(Font.font("Arial", 10)); lRod.setAlignment(Pos.CENTER_RIGHT); lRod.setMaxWidth(Double.MAX_VALUE);
            lRod.setStyle("-fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");
            pagina.getChildren().add(lRod);

            job.printPage(pageLayout, pagina);
            job.endJob();
        }
    }

    @FXML
    void handleSair(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void limparCampos() {
        txtDescricao.clear();
        txtPrecoNormal.setText("");
        txtPrecoDesconto.setText("");
        
        itemSelecionado = null;
        tabelaItens.getSelectionModel().clearSelection();
        
        btnExcluir.setDisable(true);
        btnSalvar.setText("SALVAR"); 
        
        if(lblStatus != null) lblStatus.setText("Modo: Inserção de Novo Item");
        txtDescricao.requestFocus();
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private BigDecimal parseMoeda(String valor) throws ParseException {
        if (valor == null || valor.trim().isEmpty()) return BigDecimal.ZERO;
        String limpo = valor.replace("R$", "").trim().replace(".", "").replace(",", ".");
        if (limpo.isEmpty()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(Double.parseDouble(limpo));
    }

    private void configurarMascaraMoeda(TextField tf) {
        tf.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*([\\.,]\\d*)?")) {
                tf.setText(oldValue);
            }
        });
    }

    // =============================================================
    // MÉTODOS PÚBLICOS PARA COMUNICAÇÃO COM O CADASTRO DE FRETE
    // =============================================================

    /**
     * Define o texto da descrição do item.
     * Chamado pelo CadastroFreteController quando o usuário digita um novo item.
     * Também configura a janela para fechar automaticamente após salvar.
     */
    public void setDescricaoTexto(String texto) {
        if (txtDescricao != null) {
            txtDescricao.setText(texto);
            // Opcional: Colocar o foco direto no campo de preço
            if (txtPrecoNormal != null) {
                javafx.application.Platform.runLater(() -> txtPrecoNormal.requestFocus());
            }
        }
        // Ativa o fechamento automático para agilizar o fluxo de cadastro rápido
        this.fecharAutomaticamente = true;
    }
    
    /**
     * Permite configurar manualmente se a janela deve fechar após salvar.
     */
    public void setFecharAutomaticamente(boolean fechar) {
        this.fecharAutomaticamente = fechar;
    }
}