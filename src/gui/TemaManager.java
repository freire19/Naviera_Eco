package gui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TemaManager {
    private static boolean isModoEscuro = false;
    private static final String CSS_CLARO = "/css/main.css";
    private static final String CSS_ESCURO = "/css/dark.css";
    
    public static void setModoEscuro(boolean escuro) {
        isModoEscuro = escuro;
    }
    
    public static boolean isModoEscuro() {
        return isModoEscuro;
    }
    
    public static void aplicarTema(Scene scene) {
        if (scene == null) return;
        
        scene.getStylesheets().clear();
        String cssParaCarregar = isModoEscuro ? CSS_ESCURO : CSS_CLARO;
        
        try {
            java.net.URL cssUrl = TemaManager.class.getResource(cssParaCarregar);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("CSS nao encontrado no classpath: " + cssParaCarregar);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar CSS: " + cssParaCarregar + " - " + e.getMessage());
        }
    }
    
    public static void aplicarTemaEmNovaJanela(Stage stage) {
        if (stage != null && stage.getScene() != null) {
            aplicarTema(stage.getScene());
        }
    }
    
    /**
     * Cria uma nova Scene com o tema correto já aplicado
     */
    public static Scene criarSceneComTema(Parent root) {
        Scene scene = new Scene(root);
        aplicarTema(scene);
        return scene;
    }
    
    /**
     * Cria uma nova Scene com dimensões específicas e tema correto já aplicado
     */
    public static Scene criarSceneComTema(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        aplicarTema(scene);
        return scene;
    }
}
