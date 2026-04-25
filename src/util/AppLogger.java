package util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

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

    // #DS5-219: redacao de PII em logs do desktop. Mascara CPF/CNPJ, email, JWT-like e senha=..., antes de imprimir.
    private static final Pattern CPF_CNPJ = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b|\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern JWT_LIKE = Pattern.compile("\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b");
    private static final Pattern SENHA_PARAM = Pattern.compile("(?i)(senha|password|pwd|token|secret)\\s*[:=]\\s*([^\\s,;}]+)");

    private static String redact(String msg) {
        if (msg == null || msg.isEmpty()) return msg;
        String r = JWT_LIKE.matcher(msg).replaceAll("***JWT***");
        r = CPF_CNPJ.matcher(r).replaceAll("***DOC***");
        r = EMAIL.matcher(r).replaceAll("***EMAIL***");
        r = SENHA_PARAM.matcher(r).replaceAll("$1=***");
        return r;
    }

    private AppLogger() {}

    public static void debug(String tag, String msg) {
        System.out.println(format("DEBUG", tag, redact(msg)));
    }

    public static void info(String tag, String msg) {
        System.out.println(format("INFO", tag, redact(msg)));
    }

    public static void warn(String tag, String msg) {
        System.err.println(format("WARN", tag, redact(msg)));
    }

    public static void error(String tag, String msg) {
        System.err.println(format("ERROR", tag, redact(msg)));
    }

    public static void error(String tag, String msg, Throwable t) {
        System.err.println(format("ERROR", tag, redact(msg)));
        if (t != null) {
            String trace = getShortTrace(t, 3);
            System.err.println(redact(trace));
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
