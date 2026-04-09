package gui;

import dao.ConexaoBD;
import gui.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

public class EstornoPagamentoController {

    @FXML private Label lblValorTotal;
    @FXML private Label lblValorPago;
    @FXML private TextField txtValorEstorno;
    @FXML private TextField txtMotivo;
    @FXML private ComboBox<String> cmbFormaDevolucao;
    @FXML private PasswordField txtSenhaAutorizador;
    @FXML private Button btnConfirmar;

    private BigDecimal pagoOriginal = BigDecimal.ZERO;
    private boolean confirmado = false;
    private int idAutorizador = 0;
    private String nomeAutorizador = "";

    // Getters
    public BigDecimal getValorEstorno() {
        // DL038: usa MoneyUtil para parsing consistente independente de Locale
        return gui.util.MoneyUtil.parseBigDecimalSafe(txtValorEstorno.getText());
    }
    public String getMotivo() { return txtMotivo.getText().toUpperCase(); }
    public String getFormaDevolucao() { return cmbFormaDevolucao.getValue(); }
    public boolean isConfirmado() { return confirmado; }
    public int getIdAutorizador() { return idAutorizador; }
    public String getNomeAutorizador() { return nomeAutorizador; }

    @FXML
    public void initialize() {
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Estorno de Pagamento"); return; }
        // DR010: carrega formas de pagamento em background
        Thread bg = new Thread(() -> {
            try {
                carregarFormasPagamento();
            } catch (Exception e) {
                System.err.println("Erro em EstornoPagamentoController (bg init): " + e.getMessage());
                javafx.application.Platform.runLater(() -> gui.util.AlertHelper.errorSafe("EstornoPagamentoController", e));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    public void setDados(BigDecimal total, BigDecimal pago) {
        this.pagoOriginal = pago != null ? pago : BigDecimal.ZERO;
        lblValorTotal.setText(String.format("R$ %,.2f", total));
        lblValorPago.setText(String.format("R$ %,.2f", pago));
        // DL038: formatar com separador consistente (pt-BR: virgula decimal)
        txtValorEstorno.setText(String.format("%,.2f", pago));
    }

    /** Overload para compatibilidade com callers legados. */
    @Deprecated
    public void setDados(double total, double pago) {
        setDados(BigDecimal.valueOf(total), BigDecimal.valueOf(pago));
    }

    private void carregarFormasPagamento() {
        ObservableList<String> formas = FXCollections.observableArrayList();
        try (Connection con = ConexaoBD.getConnection();
             ResultSet rs = con.prepareStatement("SELECT nome_forma_pagamento FROM aux_formas_pagamento").executeQuery()) {
            while(rs.next()) formas.add(rs.getString(1));
        } catch (SQLException e) { formas.addAll("DINHEIRO", "PIX"); }
        javafx.application.Platform.runLater(() -> {
            cmbFormaDevolucao.setItems(formas);
            cmbFormaDevolucao.getSelectionModel().selectFirst();
        });
    }

    @FXML void confirmar() {
        // 1. Validacoes basicas
        BigDecimal v = getValorEstorno();
        BigDecimal limiteMaximo = pagoOriginal.add(model.StatusPagamento.TOLERANCIA_PAGAMENTO);
        if (v.compareTo(new BigDecimal("0.001")) <= 0 || v.compareTo(limiteMaximo) > 0) {
            AlertHelper.error("Valor inválido! O valor deve ser maior que zero e não pode ultrapassar o valor pago.");
            return;
        }
        if (txtMotivo.getText().trim().isEmpty() || txtMotivo.getText().length() < 3) {
            AlertHelper.error("O motivo do estorno é obrigatório.");
            return;
        }
        
        String senhaDigitada = txtSenhaAutorizador.getText();
        if (senhaDigitada.isEmpty()) {
            AlertHelper.error("A senha do autorizador é obrigatória para realizar estornos.");
            return;
        }

        // 2. Validação da Senha no Banco (Suporte a Criptografia)
        if (validarAutorizadorNoBanco(senhaDigitada)) {
            confirmado = true;
            ((Stage) btnConfirmar.getScene().getWindow()).close();
        } else {
            AlertHelper.error("Senha inválida ou usuário sem permissão de Gerente.");
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
                    if(hashDoBanco != null) {
                        // Sempre usa BCrypt — sem fallback plaintext
                        if(BCrypt.checkpw(senhaDigitada, hashDoBanco)) {
                            senhaConfere = true;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Hash nao e BCrypt valido — ignora este usuario
                    System.err.println("Hash invalido para usuario " + nome + ": formato nao-BCrypt");
                }

                if (senhaConfere) {
                    this.idAutorizador = id;
                    this.nomeAutorizador = nome;
                    return true; // Achou um gerente com essa senha!
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.error("Erro ao conectar no banco para validar senha: " + e.getMessage());
        }
        return false; // Nenhuma senha bateu
    }

    @FXML void cancelar() { ((Stage) btnConfirmar.getScene().getWindow()).close(); }
    
}