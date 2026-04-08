package gui.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 * SERVIÇO DE LOG DE ERROS - SISTEMA DE EMBARCAÇÃO
 * ============================================================================
 * 
 * Esta classe gerencia o registro de erros do sistema em arquivo de log.
 * 
 * COMO USAR:
 * ----------
 * // Registrar um erro:
 * LogService.registrarErro("Descrição do erro", excecao);
 * 
 * // Registrar apenas uma mensagem:
 * LogService.registrarErro("Ocorreu um problema na operação X");
 * 
 * // Abrir o arquivo de log:
 * LogService.abrirArquivoLog();
 * 
 * ============================================================================
 */
public class LogService {

    // Arquivo de log em diretório dedicado
    private static final String ARQUIVO_LOG;
    static {
        String dir = System.getProperty("user.home") + java.io.File.separator + ".sistema_embarcacao";
        new java.io.File(dir).mkdirs();
        ARQUIVO_LOG = dir + java.io.File.separator + "log_erros.txt";
    }
    
    // Formatador de data/hora
    private static final DateTimeFormatter FORMATO_DATA_HORA = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // Separador visual
    private static final String SEPARADOR = 
        "================================================================================";
    
    /**
     * Registra um erro no arquivo de log com exceção
     * 
     * @param mensagem Descrição do erro
     * @param e Exceção ocorrida
     */
    public static void registrarErro(String mensagem, Throwable e) {
        try (FileWriter fw = new FileWriter(ARQUIVO_LOG, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String dataHora = LocalDateTime.now().format(FORMATO_DATA_HORA);
            
            pw.println(SEPARADOR);
            pw.println("DATA/HORA: " + dataHora);
            pw.println("MENSAGEM: " + mensagem);
            
            if (e != null) {
                pw.println("EXCEÇÃO: " + e.getClass().getName());
                pw.println("DETALHES: " + e.getMessage());
                pw.println();
                pw.println("STACK TRACE:");
                e.printStackTrace(pw);
            }
            
            pw.println(SEPARADOR);
            pw.println();
            
        } catch (IOException ex) {
            System.err.println("Erro ao gravar log: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Registra uma mensagem de erro simples no arquivo de log
     * 
     * @param mensagem Descrição do erro
     */
    public static void registrarErro(String mensagem) {
        registrarErro(mensagem, null);
    }
    
    /**
     * Registra um erro fatal (não capturado) no arquivo de log
     * 
     * @param thread Thread onde ocorreu o erro
     * @param e Exceção ocorrida
     */
    public static void registrarErroFatal(Thread thread, Throwable e) {
        String mensagem = String.format(
            "ERRO FATAL NA THREAD '%s' - O sistema encontrou um erro crítico não tratado.",
            thread.getName()
        );
        registrarErro(mensagem, e);
    }
    
    /**
     * Verifica se o arquivo de log existe
     * 
     * @return true se o arquivo existe, false caso contrário
     */
    public static boolean arquivoExiste() {
        File arquivo = new File(ARQUIVO_LOG);
        return arquivo.exists() && arquivo.length() > 0;
    }
    
    /**
     * Abre o arquivo de log com o editor padrão do sistema
     * 
     * @return true se abriu com sucesso, false caso contrário
     */
    public static boolean abrirArquivoLog() {
        try {
            File arquivo = new File(ARQUIVO_LOG);
            
            if (!arquivo.exists()) {
                return false;
            }
            
            // Usar Desktop para abrir com o programa padrão
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(arquivo);
                    return true;
                }
            }
            
            // Fallback para Windows
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("notepad.exe " + arquivo.getAbsolutePath());
                return true;
            }
            
            return false;
            
        } catch (IOException e) {
            System.err.println("Erro ao abrir arquivo de log: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Retorna o caminho completo do arquivo de log
     * 
     * @return Caminho do arquivo
     */
    public static String getCaminhoArquivo() {
        return new File(ARQUIVO_LOG).getAbsolutePath();
    }
    
    /**
     * Limpa o arquivo de log (cria um novo vazio)
     * 
     * @return true se limpou com sucesso
     */
    public static boolean limparLog() {
        try {
            File arquivo = new File(ARQUIVO_LOG);
            if (arquivo.exists()) {
                arquivo.delete();
            }
            
            // Criar novo arquivo com cabeçalho
            try (PrintWriter pw = new PrintWriter(new FileWriter(ARQUIVO_LOG))) {
                pw.println("================================================================================");
                pw.println("           LOG DE ERROS - SISTEMA DE GERENCIAMENTO DE EMBARCAÇÃO");
                pw.println("           Arquivo criado em: " + LocalDateTime.now().format(FORMATO_DATA_HORA));
                pw.println("================================================================================");
                pw.println();
            }
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Erro ao limpar log: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Registra uma informação no log (não é erro, apenas registro)
     * 
     * @param info Informação a ser registrada
     */
    public static void registrarInfo(String info) {
        try (FileWriter fw = new FileWriter(ARQUIVO_LOG, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String dataHora = LocalDateTime.now().format(FORMATO_DATA_HORA);
            pw.println("[INFO] " + dataHora + " - " + info);
            
        } catch (IOException ex) {
            System.err.println("Erro ao gravar log: " + ex.getMessage());
        }
    }
}
