package gui;

import gui.util.SyncClient;
import gui.util.SyncClient.SyncEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller para a tela de configuração de sincronização.
 */
public class ConfigurarSincronizacaoController implements Initializable, SyncClient.SyncListener {

    @FXML private TextField txtServerUrl;
    @FXML private TextField txtLoginSync;
    @FXML private PasswordField txtSenhaSync;
    @FXML private PasswordField txtToken;
    @FXML private CheckBox chkAutoSync;
    @FXML private Spinner<Integer> spnIntervalo;
    @FXML private Label lblStatusConexao;
    @FXML private Label lblUltimaSync;
    @FXML private Label lblPassageirosPend;
    @FXML private Label lblPassagensPend;
    @FXML private Label lblViagensPend;
    @FXML private Label lblEncomendasPend;
    @FXML private Label lblFretesPend;
    @FXML private TextArea txtLog;
    @FXML private Button btnTestarConexao;
    @FXML private Button btnSincronizarAgora;
    
    private SyncClient syncClient;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Configurar Sincronizacao"); return; }
        syncClient = SyncClient.getInstance();
        syncClient.addListener(this);
        
        // Configurar Spinner
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 5);
        spnIntervalo.setValueFactory(valueFactory);
        
        // Carregar configurações
        carregarConfiguracoes();
        
        // Atualizar pendências
        atualizarPendencias();
        
        log("Tela de sincronização inicializada.");
    }
    
    private void carregarConfiguracoes() {
        txtServerUrl.setText(syncClient.getServerUrl());
        // Pre-preenche login. Se vazio no SyncClient, usa o do usuario logado (email) como sugestao.
        String loginPersistido = syncClient.getLogin();
        if (loginPersistido == null || loginPersistido.isEmpty()) {
            model.Usuario u = gui.util.SessaoUsuario.getUsuarioLogado();
            if (u != null && u.getEmail() != null && !u.getEmail().isEmpty()) {
                loginPersistido = u.getEmail();
            }
        }
        txtLoginSync.setText(loginPersistido == null ? "" : loginPersistido);
        // Senha e token nunca sao persistidos (DS5-204) — deixar em branco
        chkAutoSync.setSelected(syncClient.isAutoSyncEnabled());
        spnIntervalo.getValueFactory().setValue(syncClient.getSyncIntervalMinutes());
        
        LocalDateTime ultima = syncClient.getUltimaSincronizacao();
        if (ultima != null) {
            lblUltimaSync.setText(ultima.format(formatter));
        } else {
            lblUltimaSync.setText("Nunca sincronizado");
        }
        
        // Limpar mensagem de status ao carregar
        lblStatusConexao.setText("");
    }
    
    @FXML
    private void testarConexao() {
        lblStatusConexao.setText("⏳ Testando conexão...");
        lblStatusConexao.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
        btnTestarConexao.setDisable(true);
        
        // Atualizar URL temporariamente
        String urlAnterior = syncClient.getServerUrl();
        String novaUrl = txtServerUrl.getText().trim();
        
        if (novaUrl.isEmpty()) {
            lblStatusConexao.setText("❌ URL não pode estar vazia!");
            lblStatusConexao.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
            btnTestarConexao.setDisable(false);
            return;
        }
        
        // Aplica credenciais da UI antes de testar — se token preenchido, usa token; senao login+senha
        aplicarCredenciaisUi(novaUrl);

        syncClient.testarConexao().thenAccept(sucesso -> {
            Platform.runLater(() -> {
                btnTestarConexao.setDisable(false);
                if (sucesso) {
                    lblStatusConexao.setText("✅ Conexão bem-sucedida!");
                    lblStatusConexao.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    log("✅ Conexão com o servidor estabelecida com sucesso.");
                } else {
                    lblStatusConexao.setText("❌ Falha na conexão!");
                    lblStatusConexao.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    syncClient.setServerUrl(urlAnterior);
                    log("❌ ERRO: Falha ao conectar com o servidor " + novaUrl);
                }
            });
        });
    }
    
    @FXML
    private void atualizarPendencias() {
        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try (Connection conn = dao.ConexaoBD.getConnection()) {
                int pass = contarPendentes(conn, "passageiros");
                int passagens = contarPendentes(conn, "passagens");
                int viagens = contarPendentes(conn, "viagens");
                int encomendas = contarPendentes(conn, "encomendas");
                int fretes = contarPendentes(conn, "fretes");
                Platform.runLater(() -> {
                    lblPassageirosPend.setText(String.valueOf(pass));
                    lblPassagensPend.setText(String.valueOf(passagens));
                    lblViagensPend.setText(String.valueOf(viagens));
                    lblEncomendasPend.setText(String.valueOf(encomendas));
                    lblFretesPend.setText(String.valueOf(fretes));
                    log("Contagem de pendências atualizada.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> log("ERRO ao contar pendências: " + e.getMessage()));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }
    
    private static final java.util.Set<String> TABELAS_SYNC_PERMITIDAS = java.util.Set.of(
        "passageiros", "passagens", "viagens", "encomendas", "fretes"
    );

    // DR212: try-with-resources para evitar leak de PreparedStatement/ResultSet
    private int contarPendentes(Connection conn, String tabela) {
        if (!TABELAS_SYNC_PERMITIDAS.contains(tabela)) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM " + tabela + " WHERE sincronizado = FALSE AND (excluido = FALSE OR excluido IS NULL) AND empresa_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dao.DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            // Tabela pode não ter a coluna sincronizado ainda
        }
        return 0;
    }
    
    @FXML
    private void sincronizarAgora() {
        // Limpar mensagem de erro anterior
        lblStatusConexao.setText("");
        
        btnSincronizarAgora.setDisable(true);
        btnSincronizarAgora.setText("🔄 Sincronizando...");
        
        log("Iniciando sincronização manual...");
        
        syncClient.sincronizarTudo().thenAccept(resultado -> {
            Platform.runLater(() -> {
                btnSincronizarAgora.setDisable(false);
                btnSincronizarAgora.setText("🚀 Sincronizar Agora");
                
                if (resultado.sucesso) {
                    // Limpar mensagem de erro e mostrar sucesso
                    lblStatusConexao.setText("✅ Sincronização bem-sucedida!");
                    lblStatusConexao.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    
                    log("✅ Sincronização concluída!");
                    log("   Enviados: " + resultado.registrosEnviados);
                    log("   Recebidos: " + resultado.registrosRecebidos);
                    
                    lblUltimaSync.setText(LocalDateTime.now().format(formatter));
                    atualizarPendencias();
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Sincronização");
                    alert.setHeaderText("Sincronização concluída!");
                    alert.setContentText(String.format(
                        "Registros enviados: %d\nRegistros recebidos: %d",
                        resultado.registrosEnviados, resultado.registrosRecebidos
                    ));
                    alert.showAndWait();
                } else {
                    lblStatusConexao.setText("❌ Erro na sincronização!");
                    lblStatusConexao.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    
                    log("❌ Erro na sincronização: " + resultado.mensagem);
                    for (String erro : resultado.erros) {
                        log("   - " + erro);
                    }
                    
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erro na Sincronização");
                    alert.setHeaderText("Ocorreram erros durante a sincronização");
                    alert.setContentText(resultado.mensagem);
                    alert.showAndWait();
                }
            });
        });
    }
    
    @FXML
    private void salvarConfiguracoes() {
        String novaUrl = txtServerUrl.getText().trim();
        aplicarCredenciaisUi(novaUrl);
        syncClient.setAutoSyncEnabled(chkAutoSync.isSelected());
        syncClient.setSyncIntervalMinutes(spnIntervalo.getValue());
        syncClient.salvarConfiguracoes();

        log("Configurações salvas com sucesso.");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configurações");
        alert.setHeaderText(null);
        alert.setContentText("Configurações salvas com sucesso!");
        alert.showAndWait();
    }
    
    /**
     * Le credenciais da UI e aplica no SyncClient.
     * Prioriza token JWT se preenchido; senao usa login+senha.
     * Senha e token sao mantidos apenas em memoria (DS5-204).
     */
    private void aplicarCredenciaisUi(String url) {
        String token = txtToken.getText();
        String login = txtLoginSync.getText().trim();
        String senha = txtSenhaSync.getText();
        if (token != null && !token.isEmpty()) {
            syncClient.configurar(url, token);
            if (login != null && !login.isEmpty()) {
                syncClient.setLogin(login); // persiste o login como identificador
                syncClient.salvarConfiguracoes();
            }
        } else if (login != null && !login.isEmpty() && senha != null && !senha.isEmpty()) {
            syncClient.configurar(url, login, senha);
        } else {
            // Nem token nem credenciais — apenas aplica a URL
            syncClient.setServerUrl(url);
            if (login != null && !login.isEmpty()) {
                syncClient.setLogin(login);
            }
        }
    }

    @FXML
    private void fechar() {
        syncClient.removeListener(this);
        Stage stage = (Stage) txtServerUrl.getScene().getWindow();
        stage.close();
    }
    
    private void log(String mensagem) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            txtLog.appendText("[" + timestamp + "] " + mensagem + "\n");
        });
    }
    
    @Override
    public void onSyncEvent(SyncEvent evento, String mensagem) {
        log(evento.name() + ": " + mensagem);
    }
}
