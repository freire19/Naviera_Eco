package com.naviera.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Dados invalidos");
        return ResponseEntity.badRequest().body(Map.of("erro", msg));
    }

    // #DS5-040: log apenas o nome do constraint, nao a mensagem completa do JDBC.
    //   PostgreSQL inclui valores violados ("Key (cpf)=(123.456.789-00)") na message —
    //   stack do `e` vai pro log pleno em DEBUG, mas em INFO/ERROR deixa fora.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Conflito de integridade: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return ResponseEntity.status(409).body(Map.of("erro", "Conflito de integridade nos dados"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception e, HttpServletRequest req) {
        // #313: correlationId em 5xx — devolve no body + log para casar request/log em prod.
        //   Reusa header X-Request-Id se ja existir (proxy upstream pode setar); senao gera um.
        String correlationId = req != null ? req.getHeader("X-Request-Id") : null;
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        // #DS5-040: mensagem da exception pode conter parametros de query — registrar
        //   apenas a classe; stack continua em DEBUG via TRACE_LOG, ativavel pontualmente.
        log.error("Erro nao tratado [{}] correlationId={}", e.getClass().getSimpleName(), correlationId);
        log.debug("Stack trace [correlationId={}]", correlationId, e);
        return ResponseEntity.internalServerError()
            .header("X-Request-Id", correlationId)
            .body(Map.of("erro", "Erro interno do servidor", "correlationId", correlationId));
    }
}
