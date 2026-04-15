package gui;

import gui.util.PermissaoService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import util.AppLogger;

/**
 * Controlador da tela CadastroCliente.fxml
 */
public class CadastroClienteController {

    @FXML
    public TextField txtRazaoSocial;

    @FXML
    private TextField txtCnpj;

    @FXML
    private TextField txtEndereco;

    @FXML
    private TextField txtTelefone;

    @FXML
    private Button btnSalvar;

    @FXML
    private Button btnEditar;

    @FXML
    private Button btnFechar;

    // Se precisar guardar um ID para edição, use aqui
    private int clienteId = -1;

    @FXML
    private void initialize() {
        if (!PermissaoService.isOperacional()) { PermissaoService.exigirOperacional("Cadastro de Cliente"); return; }
        // Botão Salvar
        btnSalvar.setOnAction(e -> {
            salvarClienteNoBanco();
        });

        // Botão Fechar
        btnFechar.setOnAction(e -> {
            fecharJanela();
        });

        // Botão Editar (exemplo; depende da sua lógica de edição)
        btnEditar.setOnAction(e -> {
            habilitarEdicao(true);
        });
    }

    public void carregarDadosCliente(int id, String razaoSocial, String cnpj, String endereco, String telefone) {
        this.clienteId = id;
        txtRazaoSocial.setText(razaoSocial);
        txtCnpj.setText(cnpj);
        txtEndereco.setText(endereco);
        txtTelefone.setText(telefone);

        btnEditar.setDisable(false); // habilita botão Editar
    }

    private void salvarClienteNoBanco() {
        // Usa ConexaoBD centralizado (sem senha hardcoded)
        String sqlInsert = "INSERT INTO clientes (nome, cpf_cnpj, endereco, telefone) "
                         + "VALUES (?, ?, ?, ?)";

        String sqlUpdate = "UPDATE clientes SET nome=?, cpf_cnpj=?, endereco=?, telefone=? "
                         + "WHERE id=?";

        try (Connection conn = dao.ConexaoBD.getConnection()) {

            if (clienteId == -1) {
                // Novo registro
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setString(1, txtRazaoSocial.getText());
                    stmt.setString(2, txtCnpj.getText());
                    stmt.setString(3, txtEndereco.getText());
                    stmt.setString(4, txtTelefone.getText());
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Cliente cadastrado com sucesso!");
                }
            } else {
                // Atualização de registro existente
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setString(1, txtRazaoSocial.getText());
                    stmt.setString(2, txtCnpj.getText());
                    stmt.setString(3, txtEndereco.getText());
                    stmt.setString(4, txtTelefone.getText());
                    stmt.setInt(5, clienteId);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Cliente atualizado com sucesso!");
                }
            }
            fecharJanela();
        } catch (SQLException e) {
            AppLogger.error("CadastroClienteController", e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "Erro ao salvar cliente: " + e.getMessage());
        }
    }

    private void habilitarEdicao(boolean habilitar) {
        txtRazaoSocial.setDisable(!habilitar);
        txtCnpj.setDisable(!habilitar);
        txtEndereco.setDisable(!habilitar);
        txtTelefone.setDisable(!habilitar);
    }

    private void fecharJanela() {
        Stage stage = (Stage) btnFechar.getScene().getWindow();
        stage.close();
    }
}
