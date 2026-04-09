package gui.util;

import javafx.scene.image.Image;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DP029: Cache de imagens para evitar leitura repetida do disco.
 * Logo da empresa e carregado uma vez e reutilizado em todos os relatorios/impressoes.
 */
public class ImageCache {

    private static final ConcurrentHashMap<String, Image> cache = new ConcurrentHashMap<>();

    /** Retorna Image do cache ou carrega do disco na primeira chamada. */
    public static Image get(String caminhoArquivo) {
        if (caminhoArquivo == null || caminhoArquivo.isEmpty()) return null;
        return cache.computeIfAbsent(caminhoArquivo, path -> {
            try {
                return new Image("file:" + path);
            } catch (Exception e) {
                System.err.println("ImageCache: erro ao carregar imagem '" + path + "': " + e.getMessage());
                return null;
            }
        });
    }

    /** Invalida cache (chamar se logo mudar em runtime). */
    public static void invalidar() { cache.clear(); }
}
