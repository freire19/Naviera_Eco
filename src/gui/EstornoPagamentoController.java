package gui;

import dao.ConexaoBD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// IMPORTANTE: Essa biblioteca é necessária para conferir a senha criptografada
import org.mindrot.jbcrypt.BCrypt; 

public class EstornoPagamentoController {

    @FXML private Label lblValorTotal;
    @FXML private Label lblValorPago;
    @FXML private TextField txtValorEstorno;
    @FXML private TextField txtMotivo;
    @FXML private ComboBox<String> cmbFormaDevolucao;
    @FXML private PasswordField txtSenhaAutorizador;
    @FXML private Button btnConfirmar;

    private double pagoOriginal;
    private boolean confirmado = false;
    private int idAutorizador = 0;
    private String nomeAutorizador = "";

    // Getters
    public double getValorEstorno() { 
        try { return Double.parseDouble(txtValorEstorno.getText().replace(",", ".").trim()); } catch(Exception e) { return 0.0; }
    }
    public String getMotivo() { return txtMotivo.getText().toUpperCase(); }
    public String getFormaDevolucao() { return cmbFormaDevolucao.getValue(); }
    public boolean isConfirmado() { return confirmado; }
    public int getIdAutorizador() { return idAutorizador; }
    public String getNomeAutorizador() { return nomeAutorizador; }

    @FXML
    public void initialize() {
        carregarFormasPagamento();
    }

    public void setDados(double total, double pago) {
        this.pagoOriginal = pago;
        lblValorTotal.setText(String.format("R$ %.2f", total));
        lblValorPago.setText(String.format("R$ %.2f", pago));
        txtValorEstorno.setText(String.format("%.2f", pago)); 
    }

    private void carregarFormasPagamento() {
        ObservableList<String> formas = FXCollections.observableArrayList();
        try (Connection con = ConexaoBD.getConnection();
             ResultSet rs = con.prepareStatement("SELECT nome_forma_pagamento FROM aux_formas_pagamento").executeQuery()) {
            while(rs.next()) formas.add(rs.getString(1));
        } catch (SQLException e) { formas.addAll("DINHEIRO", "PIX"); }
        cmbFormaDevolucao.setItems(formas);
        cmbFormaDevolucao.getSelectionModel().selectFirst();
    }

    @FXML void confirmar() {
        // 1. Validações básicas
        double v = getValorEstorno();
        if (v <= 0.001 || v > pagoOriginal + 0.01) {
            alertErro("Valor inválido! O valor deve ser maior que zero e não pode ultrapassar o valor pago.");
            return;
        }
        if (txtMotivo.getText().trim().isEmpty() || txtMotivo.getText().length() < 3) {
            alertErro("O motivo do estorno é obrigatório.");
            return;
        }
        
        String senhaDigitada = txtSenhaAutorizador.getText();
        if (senhaDigitada.isEmpty()) {
            alertErro("A senha do autorizador é obrigatória para realizar estornos.");
            return;
        }

        // 2. Validação da Senha no Banco (Suporte a Criptografia)
        if (validarAutorizadorNoBanco(senhaDigitada)) {
            confirmado = true;
            ((Stage) btnConfirmar.getScene().getWindow()).close();
        } else {
            alertErro("Senha inválida ou usuário sem permissão de Gerente.");
            txtSenhaAutorizador.clear();
        }
    }
    
    // Valida a senha comparando com todos os gerentes do banco
    private boolean validarAutorizadorNoBanco(String senhaDigitada) {
        // Busca a hash de todos que são 'Gerente'
        String sql = "SELECT id_usuario, nome_completo, senha_hash FROM usuarios WHERE funcao ILIKE '%Gerente%'";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while(rs.next()) {
                String hashDoBanco = rs.getString("senha_hash");
                int id = rs.getInt("id_usuario");
                String nome = rs.getString("nome_completo");

                // Verifica se a senha bate com a hash usando BCrypt
                boolean senhaConfere = false;
                try {
                    if(hashDoBanco != null && hashDoBanco.startsWith("$2a$")) {
                        // Se for hash BCrypt
                        if(BCrypt.checkpw(senhaDigitada, hashDoBanco)) {
                            senhaConfere = true;
                        }
                    } else if (hashDoBanco != null && hashDoBanco.equals(senhaDigitada)) {
                        // Se for senha plana (texto normal - fallback)
                        senhaConfere = true;
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao verificar hash: " + e.getMessage());
                }

                if (senhaConfere) {
                    this.idAutorizador = id;
                    this.nomeAutorizador = nome;
                    return true; // Achou um gerente com essa senha!
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            alertErro("Erro ao conectar no banco para validar senha: " + e.getMessage());
        }
        return false; // Nenhuma senha bateu
    }

    @FXML void cancelar() { ((Stage) btnConfirmar.getScene().getWindow()).close(); }
    
    private void alertErro(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro de Validação");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}