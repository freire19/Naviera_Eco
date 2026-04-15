package service;

import util.AppLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

/**
 * Servico responsavel por executar backup do banco de dados via pg_dump.
 * Extraido de TelaPrincipalController para separar logica de infraestrutura da UI.
 */
public class BackupService {

    private static final String TAG = "BackupService";

    /**
     * Resultado de uma operacao de backup.
     */
    public static class BackupResult {
        private final boolean sucesso;
        private final String mensagem;
        private final long tamanhoBytes;

        public BackupResult(boolean sucesso, String mensagem, long tamanhoBytes) {
            this.sucesso = sucesso;
            this.mensagem = mensagem;
            this.tamanhoBytes = tamanhoBytes;
        }

        public boolean isSucesso() { return sucesso; }
        public String getMensagem() { return mensagem; }
        public long getTamanhoBytes() { return tamanhoBytes; }

        public String getTamanhoFormatado() {
            long bytes = tamanhoBytes;
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Executa o backup do banco de dados usando pg_dump.
     * Le credenciais de db.properties e grava o arquivo SQL no destino informado.
     *
     * @param destino arquivo onde o backup sera salvo
     * @return resultado com sucesso/falha e mensagem descritiva
     */
    public BackupResult executarBackup(File destino) {
        // Ler credenciais de db.properties
        String host = "localhost";
        String porta = "5432";
        String banco = "naviera_eco";
        String usuario = "postgres";
        String senha = "";
        try {
            java.util.Properties dbProps = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream("db.properties")) {
                dbProps.load(fis);
            }
            String url = dbProps.getProperty("db.url", "");
            // Extrai host, porta e banco da URL JDBC: jdbc:postgresql://host:porta/banco
            if (url.contains("//")) {
                String parte = url.substring(url.indexOf("//") + 2);
                if (parte.contains(":")) host = parte.substring(0, parte.indexOf(":"));
                if (parte.contains(":") && parte.contains("/")) porta = parte.substring(parte.indexOf(":") + 1, parte.indexOf("/"));
                if (parte.contains("/")) banco = parte.substring(parte.indexOf("/") + 1);
            }
            usuario = dbProps.getProperty("db.usuario", usuario);
            senha = dbProps.getProperty("db.senha", senha);
        } catch (Exception ex) {
            AppLogger.warn(TAG, "Aviso: nao foi possivel ler db.properties para backup. Usando defaults.");
        }

        try {
            // Construir comando pg_dump
            ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", porta,
                "-U", usuario,
                "-d", banco,
                "-f", destino.getAbsolutePath(),
                "--format=plain",
                "--no-owner",
                "--no-privileges"
            );

            // DR025: usar .pgpass temporario em vez de PGPASSWORD no environment
            File pgpassFile = null;
            if (senha != null && !senha.isEmpty()) {
                pgpassFile = File.createTempFile(".pgpass_naviera_", ".tmp");
                pgpassFile.deleteOnExit();
                // Restringir permissoes (pg_dump exige 0600)
                pgpassFile.setReadable(false, false);
                pgpassFile.setReadable(true, true);
                pgpassFile.setWritable(false, false);
                pgpassFile.setWritable(true, true);
                Files.writeString(pgpassFile.toPath(),
                    host + ":" + porta + ":" + banco + ":" + usuario + ":" + senha + "\n");
                pb.environment().put("PGPASSFILE", pgpassFile.getAbsolutePath());
            }
            pb.environment().put("PGCONNECT_TIMEOUT", "10");

            pb.redirectErrorStream(true);
            Process processo = pb.start();

            // Ler saida do processo
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processo.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = processo.waitFor();

            // DR025: limpar .pgpass temporario imediatamente apos uso
            if (pgpassFile != null && pgpassFile.exists()) pgpassFile.delete();

            if (exitCode == 0 && destino.exists()) {
                AppLogger.info(TAG, "Backup realizado com sucesso: " + destino.getAbsolutePath());
                return new BackupResult(true,
                    "O arquivo foi salvo em:\n" + destino.getAbsolutePath(),
                    destino.length());
            } else {
                AppLogger.error(TAG, "Falha ao gerar backup. Exit code: " + exitCode + ". Output: " + output);
                return new BackupResult(false,
                    "Verifique se o PostgreSQL está instalado e se o comando 'pg_dump' está disponível.\n\n" +
                    "Detalhes: " + output, 0);
            }

        } catch (IOException ex) {
            AppLogger.error(TAG, "Erro de IO ao executar pg_dump: " + ex.getMessage());
            return new BackupResult(false,
                "O utilitário 'pg_dump' do PostgreSQL não foi encontrado no sistema.\n\n" +
                "SOLUÇÃO:\n" +
                "1. Verifique se o PostgreSQL está instalado\n" +
                "2. Adicione o caminho do PostgreSQL às variáveis de ambiente\n" +
                "   Geralmente: C:\\Program Files\\PostgreSQL\\17\\bin\n" +
                "3. Reinicie o sistema após configurar", 0);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            AppLogger.error(TAG, "Backup interrompido: " + ex.getMessage());
            return new BackupResult(false, "Backup foi interrompido.", 0);
        }
    }
}
