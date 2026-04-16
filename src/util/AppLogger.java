package util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger leve para o projeto Naviera Desktop.
 * Substitui chamadas diretas a System.err.println e e.printStackTrace()
 * com saida estruturada contendo timestamp, nivel e tag.
 *
 * Formato: 2026-04-10 12:00:00 [LEVEL] [Tag] Mensagem
 * Para erros com Throwable: imprime as 3 primeiras linhas do stack trace.
 */
public class AppLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppLogger() {}

    public static void debug(String tag, String msg) {
        System.out.println(format("DEBUG", tag, msg));
    }

    public static void info(String tag, String msg) {
        System.out.println(format("INFO", tag, msg));
    }

    public static void warn(String tag, String msg) {
        System.err.println(format("WARN", tag, msg));
    }

    public static void error(String tag, String msg) {
        System.err.println(format("ERROR", tag, msg));
    }

    public static void error(String tag, String msg, Throwable t) {
        System.err.println(format("ERROR", tag, msg));
        if (t != null) {
            String trace = getShortTrace(t, 3);
            System.err.println(trace);
        }
    }

    private static String format(String level, String tag, String msg) {
        return LocalDateTime.now().format(FMT) + " [" + level + "] [" + tag + "] " + msg;
    }

    /**
     * Retorna as primeiras {@code maxLines} linhas do stack trace.
     */
    private static String getShortTrace(Throwable t, int maxLines) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String[] lines = sw.toString().split("\\R");
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(lines.length, maxLines + 1); // +1 para incluir a linha da excecao
        for (int i = 0; i < limit; i++) {
            sb.append(lines[i]);
            if (i < limit - 1) sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
