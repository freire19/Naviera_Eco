package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Controller para a tela de Configuração da API
 * Permite configurar a conexão com o servidor web e recursos de automação
 */
public class ConfigurarApiController implements Initializable {

    @FXML private TextField txtUrlApi;
    @FXML private PasswordField txtToken;
    @FXML private TextField txtPastaArquivos;
    @FXML private Spinner<Integer> spnTamanhoMax;
    @FXML private Slider sldQualidade;
    @FXML private Label lblQualidade;
    @FXML private Label lblStatus;
    @FXML private Label lblStatusConexao;
    
    @FXML private CheckBox chkUsarNuvem;
    @FXML private VBox pnlNuvem;
    @FXML private TextField txtUrlNuvem;
    @FXML private PasswordField txtChaveNuvem;
    
    @FXML private CheckBox chkHabilitarXml;
    @FXML private CheckBox chkHabilitarFoto;
    @FXML private CheckBox chkHabilitarAudio;
    @FXML private CheckBox chkHabilitarOcr;
    @FXML private CheckBox chkHabilitarVoz;
    
    @FXML private TextField txtUrlSistemaWeb;
    @FXML private Button btnTestar;

    private static final String CONFIG_FILE = "api_config.properties";
    private Properties config = new Properties();
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // DS004: somente admin pode configurar API
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Configurar API"); return; }
        // Configurar Spinner
        spnTamanhoMax.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));
        
        // Configurar Slider
        sldQualidade.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblQualidade.setText(newVal.intValue() + "%");
        });
        
        // Configurar checkbox de nuvem
        chkUsarNuvem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            pnlNuvem.setVisible(newVal);
            pnlNuvem.setManaged(newVal);
        });
        
        // Carregar configurações salvas
        carregarConfiguracoes();
        
        // Atualizar status
        atualizarStatus();
    }

    private void carregarConfiguracoes() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config.load(fis);
                
                txtUrlApi.setText(config.getProperty("url.api", "https://sistemabarco.navdeusdealianca.com.br/api"));
                txtToken.setText(config.getProperty("token", ""));
                txtPastaArquivos.setText(config.getProperty("pasta.arquivos", getPastaDefault()));
                spnTamanhoMax.getValueFactory().setValue(Integer.parseInt(config.getProperty("tamanho.max", "10")));
                sldQualidade.setValue(Double.parseDouble(config.getProperty("qualidade", "85")));
                
                chkUsarNuvem.setSelected(Boolean.parseBoolean(config.getProperty("usar.nuvem", "false")));
                txtUrlNuvem.setText(config.getProperty("url.nuvem", ""));
                // DS005: decodificar chave nuvem se estava encoded
                String chaveRaw = config.getProperty("chave.nuvem", "");
                if ("true".equals(config.getProperty("chave.nuvem.encoded")) && !chaveRaw.isEmpty()) {
                    try { chaveRaw = new String(java.util.Base64.getDecoder().decode(chaveRaw), java.nio.charset.StandardCharsets.UTF_8); }
                    catch (Exception ignored) { /* fallback: usa valor raw */ }
                }
                txtChaveNuvem.setText(chaveRaw);
                
                chkHabilitarXml.setSelected(Boolean.parseBoolean(config.getProperty("habilitar.xml", "true")));
                chkHabilitarFoto.setSelected(Boolean.parseBoolean(config.getProperty("habilitar.foto", "true")));
                chkHabilitarAudio.setSelected(Boolean.parseBoolean(config.getProperty("habilitar.audio", "true")));
                chkHabilitarOcr.setSelected(Boolean.parseBoolean(config.getProperty("habilitar.ocr", "false")));
                chkHabilitarVoz.setSelected(Boolean.parseBoolean(config.getProperty("habilitar.voz", "false")));
                
            } catch (IOException e) {
                System.err.println("Erro ao carregar configurações: " + e.getMessage());
            }
        } else {
            // Valores padrão
            txtUrlApi.setText("https://sistemabarco.navdeusdealianca.com.br/api");
            txtPastaArquivos.setText(getPastaDefault());
        }
    }

    private String getPastaDefault() {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + "SistemaEmbarcacao" + File.separator + "uploads";
    }

    private void atualizarStatus() {
        boolean configurado = txtUrlApi.getText() != null && !txtUrlApi.getText().isEmpty();
        if (configurado) {
            lblStatus.setText("● Configurado");
            lblStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            lblStatus.setText("● Não Configurado");
            lblStatus.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void testarConexao() {
        String urlStr = txtUrlApi.getText();
        if (urlStr == null || urlStr.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Digite a URL da API primeiro!");
            return;
        }

        btnTestar.setDisable(true);
        lblStatusConexao.setText("⏳ Testando conexão...");
        lblStatusConexao.setStyle("-fx-text-fill: #3498db;");

        new Thread(() -> {
            try {
                URL url = new URL(urlStr.replace("/api", "") + "/api/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                
                Platform.runLater(() -> {
                    if (responseCode == 200) {
                        lblStatusConexao.setText("✅ Conexão OK! Servidor respondendo.");
                        lblStatusConexao.setStyle("-fx-text-fill: #27ae60;");
                    } else if (responseCode == 404) {
                        lblStatusConexao.setText("⚠️ Servidor encontrado, mas endpoint /health não existe. Conexão pode estar OK.");
                        lblStatusConexao.setStyle("-fx-text-fill: #f39c12;");
                    } else {
                        lblStatusConexao.setText("⚠️ Servidor respondeu com código: " + responseCode);
                        lblStatusConexao.setStyle("-fx-text-fill: #f39c12;");
                    }
                    btnTestar.setDisable(false);
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatusConexao.setText("❌ Erro: " + e.getMessage());
                    lblStatusConexao.setStyle("-fx-text-fill: #e74c3c;");
                    btnTestar.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void toggleToken() {
        // Alternar visibilidade do token (implementar com TextField adicional se necessário)
        // #022: nao exibe token completo — mostra apenas ultimos 4 chars
        String token = txtToken.getText();
        String masked = token.length() > 4 ? "****" + token.substring(token.length() - 4) : "****";
        mostrarAlerta(Alert.AlertType.INFORMATION, "Token", "Token configurado: " + masked);
    }

    @FXML
    private void gerarToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        for (int i = 0; i < 32; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        txtToken.setText(token.toString());
        mostrarAlerta(Alert.AlertType.INFORMATION, "Token Gerado", 
            "Novo token gerado com sucesso!\nLembre-se de copiar e salvar este token.");
    }

    @FXML
    private void selecionarPasta() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecionar Pasta para Arquivos");
        
        File pastaAtual = new File(txtPastaArquivos.getText());
        if (pastaAtual.exists()) {
            chooser.setInitialDirectory(pastaAtual);
        }
        
        Stage stage = (Stage) txtPastaArquivos.getScene().getWindow();
        File pasta = chooser.showDialog(stage);
        
        if (pasta != null) {
            txtPastaArquivos.setText(pasta.getAbsolutePath());
        }
    }

    @FXML
    private void copiarUrl() {
        String url = txtUrlSistemaWeb.getText();
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(url);
        clipboard.setContent(content);
        
        mostrarAlerta(Alert.AlertType.INFORMATION, "Copiado", "URL copiada para a área de transferência!");
    }

    @FXML
    private void restaurarPadroes() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restaurar Padrões");
        confirm.setHeaderText("Deseja restaurar as configurações padrão?");
        confirm.setContentText("Todas as configurações atuais serão perdidas.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            txtUrlApi.setText("https://sistemabarco.navdeusdealianca.com.br/api");
            txtToken.setText("");
            txtPastaArquivos.setText(getPastaDefault());
            spnTamanhoMax.getValueFactory().setValue(10);
            sldQualidade.setValue(85);
            chkUsarNuvem.setSelected(false);
            txtUrlNuvem.setText("");
            txtChaveNuvem.setText("");
            chkHabilitarXml.setSelected(true);
            chkHabilitarFoto.setSelected(true);
            chkHabilitarAudio.setSelected(true);
            chkHabilitarOcr.setSelected(false);
            chkHabilitarVoz.setSelected(false);
        }
    }

    @FXML
    private void salvar() {
        try {
            // DS012: validar que path de uploads esta dentro do diretorio do usuario
            File pastaUploads = new File(txtPastaArquivos.getText());
            String userHome = System.getProperty("user.home");
            if (!pastaUploads.getCanonicalPath().startsWith(new File(userHome).getCanonicalPath())) {
                mostrarAlerta(Alert.AlertType.WARNING, "Caminho Invalido", "A pasta de uploads deve estar dentro do diretorio do usuario.");
                return;
            }
            if (!pastaUploads.exists()) {
                pastaUploads.mkdirs();
            }
            
            // Salvar configurações
            config.setProperty("url.api", txtUrlApi.getText());
            config.setProperty("token", txtToken.getText());
            config.setProperty("pasta.arquivos", txtPastaArquivos.getText());
            config.setProperty("tamanho.max", String.valueOf(spnTamanhoMax.getValue()));
            config.setProperty("qualidade", String.valueOf((int) sldQualidade.getValue()));
            
            config.setProperty("usar.nuvem", String.valueOf(chkUsarNuvem.isSelected()));
            config.setProperty("url.nuvem", txtUrlNuvem.getText());
            // DS005: ofuscar chave nuvem com Base64 (nao e criptografia forte, mas evita leitura casual)
            String chaveNuvem = txtChaveNuvem.getText();
            if (chaveNuvem != null && !chaveNuvem.isEmpty()) {
                config.setProperty("chave.nuvem", java.util.Base64.getEncoder().encodeToString(chaveNuvem.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                config.setProperty("chave.nuvem.encoded", "true");
            } else {
                config.setProperty("chave.nuvem", "");
                config.setProperty("chave.nuvem.encoded", "false");
            }
            
            config.setProperty("habilitar.xml", String.valueOf(chkHabilitarXml.isSelected()));
            config.setProperty("habilitar.foto", String.valueOf(chkHabilitarFoto.isSelected()));
            config.setProperty("habilitar.audio", String.valueOf(chkHabilitarAudio.isSelected()));
            config.setProperty("habilitar.ocr", String.valueOf(chkHabilitarOcr.isSelected()));
            config.setProperty("habilitar.voz", String.valueOf(chkHabilitarVoz.isSelected()));
            
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                config.store(fos, "Configurações da API - Naviera");
            }
            
            atualizarStatus();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Configurações salvas com sucesso!");
            
            // Fechar janela
            ((Stage) txtUrlApi.getScene().getWindow()).close();
            
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Erro ao salvar configurações: " + e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        ((Stage) txtUrlApi.getScene().getWindow()).close();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    /**
     * Abre a tela de configuração da API
     */
    public static void abrir() {
        try {
            FXMLLoader loader = new FXMLLoader(ConfigurarApiController.class.getResource("ConfigurarApi.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Configuração da API - Naviera");
            stage.setScene(new Scene(root, 700, 800));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setContentText("Erro ao abrir tela de configuração: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Retorna a URL da API configurada
     */
    public static String getUrlApi() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            return props.getProperty("url.api", "https://sistemabarco.navdeusdealianca.com.br/api");
        } catch (IOException e) {
            return "https://sistemabarco.navdeusdealianca.com.br/api";
        }
    }
    
    /**
     * Retorna o token de acesso configurado
     */
    public static String getToken() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            return props.getProperty("token", "");
        } catch (IOException e) {
            return "";
        }
    }
    
    /**
     * Retorna a pasta de arquivos configurada
     */
    public static String getPastaArquivos() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            return props.getProperty("pasta.arquivos", System.getProperty("user.home") + "/SistemaEmbarcacao/uploads");
        } catch (IOException e) {
            return System.getProperty("user.home") + "/SistemaEmbarcacao/uploads";
        }
    }
}
