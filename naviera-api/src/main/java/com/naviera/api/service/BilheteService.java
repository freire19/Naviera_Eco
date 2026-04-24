package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import static com.naviera.api.config.MoneyUtils.toBigDecimal;
import com.naviera.api.dto.BilheteDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class BilheteService {

    private final JdbcTemplate jdbc;
    private final com.naviera.api.config.CryptoUtil crypto;
    private static final int TOTP_PERIOD = 30; // seconds
    private static final int TOTP_DIGITS = 6;

    public BilheteService(JdbcTemplate jdbc, com.naviera.api.config.CryptoUtil crypto) {
        this.jdbc = jdbc;
        this.crypto = crypto;
    }

    /**
     * Compra uma passagem via app e gera bilhete digital com TOTP
     */
    @Transactional
    public BilheteDTO comprar(Long clienteAppId, Long idViagem, Long idRota, Long idTipoPassagem) {
        // 1. Buscar dados do cliente
        var cliente = jdbc.queryForMap(
            "SELECT id, nome, documento FROM clientes_app WHERE id = ? AND ativo = TRUE", clienteAppId);

        String nomeCliente = (String) cliente.get("nome");
        String docCliente = (String) cliente.get("documento");

        // 2. Verificar se a viagem existe e está ativa — derivar empresaId server-side (fix DS4-001)
        // #219: data_viagem >= CURRENT_DATE — nao vender bilhete para viagem passada.
        var viagem = jdbc.queryForMap("""
            SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.empresa_id,
                   e.nome as embarcacao, r.origem, r.destino,
                   hs.descricao as horario_saida, v.id_horario_saida,
                   r.id as id_rota
            FROM viagens v
            JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
            JOIN rotas r ON v.id_rota = r.id
            LEFT JOIN aux_horarios_saida hs ON v.id_horario_saida = hs.id_horario_saida
            WHERE v.id_viagem = ? AND v.is_atual = TRUE AND v.data_viagem >= CURRENT_DATE
            """, idViagem);

        Integer empresaId = ((Number) viagem.get("empresa_id")).intValue();

        // 3. Buscar tarifa, filtrando por empresa_id
        var tarifa = jdbc.queryForMap("""
            SELECT valor_transporte, valor_alimentacao, valor_cargas, valor_desconto
            FROM tarifas WHERE id_rota = ? AND id_tipo_passagem = ? AND empresa_id = ?
            """, idRota != null ? idRota : viagem.get("id_rota"), idTipoPassagem, empresaId);

        var valorTransporte = toBigDecimal(tarifa.get("valor_transporte"));
        var valorAlimentacao = toBigDecimal(tarifa.get("valor_alimentacao"));
        var valorCargas = toBigDecimal(tarifa.get("valor_cargas"));
        var valorDesconto = toBigDecimal(tarifa.get("valor_desconto"));
        var valorTotal = valorTransporte.add(valorAlimentacao).add(valorCargas).subtract(valorDesconto);

        // 4. Criar ou reutilizar passageiro, filtrando por empresa_id
        Long idPassageiro;
        var passageiros = jdbc.queryForList(
            "SELECT id_passageiro FROM passageiros WHERE numero_documento = ? AND empresa_id = ?", docCliente, empresaId);
        if (passageiros.isEmpty()) {
            idPassageiro = jdbc.queryForObject("""
                INSERT INTO passageiros (nome_passageiro, numero_documento, empresa_id)
                VALUES (?, ?, ?) RETURNING id_passageiro
                """, Long.class, nomeCliente, docCliente, empresaId);
        } else {
            idPassageiro = ((Number) passageiros.get(0).get("id_passageiro")).longValue();
        }

        // 5. Gerar numero do bilhete com advisory lock para evitar race condition no MAX+1
        jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> null, empresaId);
        String numeroBilhete = jdbc.queryForObject(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_bilhete FROM '[0-9]+') AS INTEGER)), 0) + 1 FROM passagens WHERE id_viagem = ? AND empresa_id = ?",
            Integer.class, idViagem, empresaId).toString();
        numeroBilhete = String.format("%05d", Integer.parseInt(numeroBilhete));

        // 6. Inserir passagem com empresa_id
        Long idPassagem = jdbc.queryForObject("""
            INSERT INTO passagens (numero_bilhete, id_passageiro, id_viagem, data_emissao,
                id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao, valor_cargas,
                valor_desconto_tarifa, valor_total, valor_a_pagar, valor_pago, valor_devedor,
                status_passagem, valor_pagamento_pix, id_horario_saida, observacoes, empresa_id)
            VALUES (?, ?, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0,
                'EMITIDA', ?, ?, 'Compra via App Naviera', ?)
            RETURNING id_passagem
            """, Long.class,
            numeroBilhete, idPassageiro, idViagem,
            idRota != null ? idRota : viagem.get("id_rota"),
            idTipoPassagem,
            valorTransporte, valorAlimentacao, valorCargas, valorDesconto,
            valorTotal, valorTotal, valorTotal,
            valorTotal,
            viagem.get("id_horario_saida"),
            empresaId
        );

        // 7. Gerar TOTP secret e QR hash
        String totpSecret = generateSecret();
        String qrHash = generateQRHash(idPassagem, totpSecret);

        // 8. Criar bilhete digital — #226: totp_secret cifrado at-rest (AES-GCM).
        jdbc.update("""
            INSERT INTO bilhetes_digitais (id_passagem, id_cliente_app, totp_secret, qr_hash)
            VALUES (?, ?, ?, ?)
            """, idPassagem, clienteAppId, crypto.encrypt(totpSecret), qrHash);

        // 9. Buscar nome tipo passagem e acomodação
        String tipoPassagem = "Adulto";
        try {
            tipoPassagem = jdbc.queryForObject(
                "SELECT nome_tipo_passagem FROM aux_tipos_passagem WHERE id_tipo_passagem = ?",
                String.class, idTipoPassagem);
        } catch (Exception ignored) {}

        return new BilheteDTO(
            idPassagem, idPassagem, numeroBilhete,
            nomeCliente, maskCPF(docCliente),
            (String) viagem.get("origem"), (String) viagem.get("destino"),
            (String) viagem.get("embarcacao"),
            viagem.get("data_viagem").toString(),
            (String) viagem.get("horario_saida"),
            "Rede", tipoPassagem,
            "R$ " + valorTotal.toPlainString(),
            "VALIDO", totpSecret, qrHash,
            idViagem.toString()
        );
    }

    /**
     * Lista bilhetes do cliente logado
     */
    public List<Map<String, Object>> listarPorCliente(Long clienteAppId) {
        // DS4-007/DS4-018 fix: NAO retornar totp_secret ao client
        return jdbc.queryForList("""
            SELECT b.id, b.id_passagem, b.qr_hash, b.status,
                   p.numero_bilhete, p.valor_total, p.data_emissao, p.status_passagem,
                   ps.nome_passageiro,
                   r.origem, r.destino,
                   e.nome as embarcacao,
                   v.data_viagem, v.id_viagem,
                   hs.descricao as horario_saida
            FROM bilhetes_digitais b
            JOIN passagens p ON b.id_passagem = p.id_passagem
            JOIN passageiros ps ON p.id_passageiro = ps.id_passageiro
            JOIN viagens v ON p.id_viagem = v.id_viagem
            JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
            JOIN rotas r ON p.id_rota = r.id
            LEFT JOIN aux_horarios_saida hs ON v.id_horario_saida = hs.id_horario_saida
            WHERE b.id_cliente_app = ? AND b.status != 'CANCELADO'
            ORDER BY v.data_viagem DESC
            """, clienteAppId);
    }

    /**
     * DS4-007 fix: Gera TOTP server-side para um bilhete do cliente.
     * Secret nunca sai do servidor.
     */
    public Map<String, Object> gerarTOTPPorBilhete(Long clienteAppId, Long bilheteId) {
        var rows = jdbc.queryForList(
            "SELECT totp_secret FROM bilhetes_digitais WHERE id = ? AND id_cliente_app = ? AND status != 'CANCELADO'",
            bilheteId, clienteAppId);
        if (rows.isEmpty()) throw ApiException.notFound("Bilhete nao encontrado");

        String secret = crypto.decrypt((String) rows.get(0).get("totp_secret"));
        long now = Instant.now().getEpochSecond();
        String code = generateTOTP(secret, now);
        int timeLeft = TOTP_PERIOD - (int) (now % TOTP_PERIOD);
        return Map.of("code", code, "timeLeft", timeLeft);
    }

    /**
     * Valida um bilhete escaneado (para o operador)
     */
    @Transactional
    public Map<String, Object> validar(Integer empresaId, String qrHash, String totpCode, String operador) {
        // Join com passagens para garantir que o bilhete pertence a esta empresa
        var bilhetes = jdbc.queryForList("""
            SELECT b.* FROM bilhetes_digitais b
            JOIN passagens p ON b.id_passagem = p.id_passagem
            WHERE b.qr_hash = ? AND p.empresa_id = ?
            """, qrHash, empresaId);

        if (bilhetes.isEmpty())
            throw ApiException.notFound("Bilhete não encontrado.");

        var bilhete = bilhetes.get(0);
        String status = (String) bilhete.get("status");

        if ("EMBARCADO".equals(status))
            throw ApiException.conflict("Bilhete já foi utilizado para embarque.");
        if ("CANCELADO".equals(status))
            throw ApiException.conflict("Bilhete cancelado.");
        if ("EXPIRADO".equals(status))
            throw ApiException.conflict("Bilhete expirado.");

        // #659: janela apertada (atual + 1 anterior = 60s max); rate-limit em attempts.
        //   Contador `totp_attempts` acumula; reset no sucesso.
        Long bilheteId = ((Number) bilhete.get("id")).longValue();
        Integer attempts = (Integer) bilhete.getOrDefault("totp_attempts", 0);
        if (attempts == null) attempts = 0;
        if (attempts >= 10) throw ApiException.tooManyRequests("Muitas tentativas — bilhete temporariamente bloqueado.");

        String secret = crypto.decrypt((String) bilhete.get("totp_secret"));
        long now = Instant.now().getEpochSecond();
        boolean valid = false;
        for (int i = -1; i <= 0; i++) {
            String expected = generateTOTP(secret, now + (i * TOTP_PERIOD));
            if (expected.equals(totpCode)) { valid = true; break; }
        }

        if (!valid) {
            jdbc.update("UPDATE bilhetes_digitais SET totp_attempts = COALESCE(totp_attempts, 0) + 1 WHERE id = ?", bilheteId);
            throw ApiException.badRequest("Código de segurança inválido ou expirado.");
        }

        // Marcar como embarcado (reset de attempts no sucesso)
        jdbc.update("""
            UPDATE bilhetes_digitais SET status = 'EMBARCADO',
                data_embarque = CURRENT_TIMESTAMP, validado_por = ?, totp_attempts = 0
            WHERE qr_hash = ?
            """, operador, qrHash);

        // Retornar dados do passageiro, filtrando por empresa_id
        Long idPassagem = ((Number) bilhete.get("id_passagem")).longValue();
        return jdbc.queryForMap("""
            SELECT p.numero_bilhete, ps.nome_passageiro, p.valor_total,
                   r.origem, r.destino, p.assento, v.data_viagem
            FROM passagens p
            JOIN passageiros ps ON p.id_passageiro = ps.id_passageiro
            JOIN viagens v ON p.id_viagem = v.id_viagem
            JOIN rotas r ON p.id_rota = r.id
            WHERE p.id_passagem = ? AND p.empresa_id = ?
            """, idPassagem, empresaId);
    }

    // ═══ TOTP CRYPTO ═══

    /**
     * Gera TOTP de 6 dígitos baseado em HMAC-SHA256
     */
    public static String generateTOTP(String secret, long timeSeconds) {
        long counter = timeSeconds / TOTP_PERIOD;
        try {
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int code = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                     | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            return String.format("%0" + TOTP_DIGITS + "d", code % (int) Math.pow(10, TOTP_DIGITS));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar TOTP", e);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateQRHash(Long idPassagem, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(("NV-PSG-" + idPassagem).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar QR hash", e);
        }
    }

    private String maskCPF(String doc) {
        if (doc == null || doc.length() < 11) return doc;
        String nums = doc.replaceAll("\\D", "");
        if (nums.length() == 11) return "***." + nums.substring(3, 6) + "." + nums.substring(6, 9) + "-" + nums.substring(9);
        return doc;
    }

}
