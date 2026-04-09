package gui.util;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.util.function.Consumer;

/**
 * Servico centralizado para OCR (Tesseract) e reconhecimento de voz (Vosk).
 * Extrai logica duplicada de CadastroFreteController e InserirEncomendaController.
 */
public class OcrAudioService {

    // DR130: substituir caminhos hardcoded Windows por caminho relativo ao home do usuario
    private static final String BASE_DIR = System.getProperty("user.home") + File.separator + ".sistema_embarcacao";
    private static final String TESSDATA_PATH = BASE_DIR + File.separator + "tessdata";
    private static final String MODELO_VOZ_PATH = BASE_DIR + File.separator + "modelo-voz";
    private static final String IDIOMA = "por";
    private static final int DURACAO_GRAVACAO_MS = 5000;
    private static final int CHUNK_SIZE = 4096;
    private static final float SAMPLE_RATE = 16000;

    /**
     * Executa OCR em uma imagem usando Tesseract.
     * Deve ser chamado em background thread.
     *
     * @param imagemFile arquivo de imagem (PNG, JPG)
     * @return texto reconhecido
     * @throws Exception se OCR falhar
     */
    public static String executarOCR(File imagemFile) throws Exception {
        ITesseract instance = new Tesseract();
        instance.setDatapath(TESSDATA_PATH);
        instance.setLanguage(IDIOMA);
        return instance.doOCR(imagemFile);
    }

    /**
     * Grava audio do microfone por 5 segundos e converte para texto usando Vosk.
     * Deve ser chamado em background thread.
     *
     * @return texto reconhecido
     * @throws Exception se microfone ou modelo falhar
     */
    public static String executarReconhecimentoVoz() throws Exception {
        Model model = new Model(MODELO_VOZ_PATH);
        TargetDataLine microphone = null;
        Recognizer recognizer = null;
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            recognizer = new Recognizer(model, (int) SAMPLE_RATE);

            microphone.open(format);
            microphone.start();

            byte[] data = new byte[CHUNK_SIZE];
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < DURACAO_GRAVACAO_MS) {
                int numBytesRead = microphone.read(data, 0, CHUNK_SIZE);
                recognizer.acceptWaveForm(data, numBytesRead);
            }

            String jsonResult = recognizer.getFinalResult();
            microphone.stop();
            microphone.close();

            return extrairTextoDoJson(jsonResult);
        } finally {
            // DR115: garantir fechamento de microphone e recognizer em caso de excecao
            if (microphone != null) {
                try { microphone.stop(); } catch (Exception ignored) {}
                try { microphone.close(); } catch (Exception ignored) {}
            }
            if (recognizer != null) {
                try { recognizer.close(); } catch (Exception ignored) {}
            }
            model.close();
        }
    }

    /**
     * Executa OCR em background thread e retorna resultado via callback.
     */
    public static void executarOCRAsync(File imagemFile, Consumer<String> onSuccess, Consumer<Exception> onError) {
        Thread t = new Thread(() -> {
            try {
                String resultado = executarOCR(imagemFile);
                onSuccess.accept(resultado);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Executa reconhecimento de voz em background thread e retorna resultado via callback.
     */
    public static void executarVozAsync(Consumer<String> onSuccess, Consumer<Exception> onError) {
        Thread t = new Thread(() -> {
            try {
                String resultado = executarReconhecimentoVoz();
                onSuccess.accept(resultado);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static String extrairTextoDoJson(String jsonResult) {
        if (jsonResult != null && jsonResult.contains("\"text\" : \"")) {
            return jsonResult.split("\"text\" : \"")[1]
                    .replace("\"}", "")
                    .replace("\n", "")
                    .trim();
        }
        return "";
    }
}
