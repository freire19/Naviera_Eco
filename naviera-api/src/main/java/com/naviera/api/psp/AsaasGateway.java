package com.naviera.api.psp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naviera.api.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementacao Asaas do {@link PspGateway}.
 *
 * Sandbox: https://sandbox.asaas.com/api/v3
 * Producao: https://api.asaas.com/v3
 *
 * Autenticacao: header {@code access_token: <api_key>}.
 *
 * Fase atual (3.1): fundacao — criarCobranca funcional, criarSubconta stub (Fase 3.2).
 */
@Component
public class AsaasGateway implements PspGateway {

    private static final Logger log = LoggerFactory.getLogger(AsaasGateway.class);
    private static final String PROVIDER = "asaas";

    private final AsaasProperties props;
    private final Environment env;
    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();

    public AsaasGateway(AsaasProperties props, Environment env) {
        this.props = props;
        this.env = env;
        // #300: sem timeout, um slowloris upstream trava a thread + conexao indefinidamente.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.rest = new RestTemplate(factory);
    }

    @Override
    public String providerName() { return PROVIDER; }

    @Override
    public CobrancaResponse criarCobranca(CobrancaRequest req) {
        if (isBlank(props.getAsaas().getApiKey())) {
            throw new IllegalStateException(
                "ASAAS_API_KEY nao configurada — configure em application.properties antes de criar cobrancas");
        }
        if (isBlank(req.subcontaId())) {
            throw new IllegalArgumentException(
                "Empresa sem subcontaId Asaas — complete o onboarding (Fase 3.2) antes");
        }
        // #DB222: defesa em profundidade — valorBruto null geraria NPE generico no subtract
        Objects.requireNonNull(req.valorBruto(), "valorBruto obrigatorio na CobrancaRequest");

        try {
            String customerId = obterOuCriarCustomer(req);
            String billingType = mapForma(req.formaPagamento());

            BigDecimal valorLiquido = req.valorBruto().subtract(
                req.descontoAplicado() != null ? req.descontoAplicado() : BigDecimal.ZERO);

            Map<String, Object> payload = new HashMap<>();
            payload.put("customer", customerId);
            payload.put("billingType", billingType);
            payload.put("value", valorLiquido);
            payload.put("description", req.descricao() != null ? req.descricao() : "Naviera");
            payload.put("externalReference", req.tipoOrigem() + ":" + req.origemId());
            // #DB209: TZ BR central (MoneyUtils.ZONE_BR) evita divergir boleto no fim do dia
            LocalDate venc = req.vencimento() != null ? req.vencimento() : LocalDate.now(com.naviera.api.config.MoneyUtils.ZONE_BR).plusDays(1);
            payload.put("dueDate", venc.toString());

            // Split Naviera: uma parcela % vai pra subconta Naviera, resto fica com a empresa
            // (a cobranca e emitida em nome da empresa, entao o dinheiro ja cai na conta dela;
            //  o split retira a taxa Naviera e envia pra subconta Naviera)
            if (!isBlank(props.getAsaas().getNavieraSubcontaId())
                    && req.splitNavieraPct() != null
                    && req.splitNavieraPct().signum() > 0) {
                List<Map<String, Object>> split = List.of(Map.of(
                    "walletId", props.getAsaas().getNavieraSubcontaId(),
                    "percentualValue", req.splitNavieraPct()
                ));
                payload.put("split", split);
            }

            JsonNode body = post("/payments", payload);
            String pspCobrancaId = body.path("id").asText();
            String status = body.path("status").asText();

            String qrPayload = null, qrImageUrl = null, linhaDigitavel = null;
            String boletoUrl = body.hasNonNull("bankSlipUrl") ? body.get("bankSlipUrl").asText() : null;
            String checkoutUrl = body.hasNonNull("invoiceUrl") ? body.get("invoiceUrl").asText() : null;

            if ("PIX".equals(req.formaPagamento())) {
                // #302: GET pixQrCode pode falhar transitoriamente apos POST — Asaas leva alguns ms
                //   para gerar o QR. Sem retry, usuario re-emite cobranca e duplica.
                JsonNode qr = getWithRetry("/payments/" + pspCobrancaId + "/pixQrCode", 3);
                qrPayload = qr.path("payload").asText(null);
                qrImageUrl = qr.path("encodedImage").asText(null);
            } else if ("BOLETO".equals(req.formaPagamento())) {
                JsonNode idf = getWithRetry("/payments/" + pspCobrancaId + "/identificationField", 3);
                linhaDigitavel = idf.path("identificationField").asText(null);
            }

            // #DR261: preferir splits retornados pelo Asaas (regras de arredondamento dele); calculo
            //   local serve so de fallback quando body.split nao vier preenchido.
            BigDecimal splitNaviera = null;
            JsonNode splitResp = body.path("split");
            if (splitResp.isArray() && splitResp.size() > 0) {
                JsonNode totalValueNode = splitResp.get(0).path("totalValue");
                if (!totalValueNode.isMissingNode() && !totalValueNode.isNull()) {
                    try { splitNaviera = new BigDecimal(totalValueNode.asText()); } catch (NumberFormatException ignored) {}
                }
            }
            if (splitNaviera == null) {
                splitNaviera = valorLiquido
                    .multiply(req.splitNavieraPct() != null ? req.splitNavieraPct() : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            BigDecimal splitEmpresa = valorLiquido.subtract(splitNaviera);

            return new CobrancaResponse(
                PROVIDER,
                pspCobrancaId,
                mapStatus(status),
                valorLiquido,
                splitNaviera,
                splitEmpresa,
                qrPayload,
                qrImageUrl,
                linhaDigitavel,
                boletoUrl,
                checkoutUrl,
                venc.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                body.toString()
            );
        } catch (Exception e) {
            log.error("[AsaasGateway] Erro ao criar cobranca: {}", e.getMessage(), e);
            throw new RuntimeException("Erro PSP Asaas: " + e.getMessage(), e);
        }
    }

    @Override
    public SubcontaResponse criarSubconta(SubcontaRequest req) {
        if (isBlank(props.getAsaas().getApiKey())) {
            throw new IllegalStateException(
                "ASAAS_API_KEY nao configurada — configure antes de onboarding");
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", req.razaoSocial());
            payload.put("email", req.email());
            payload.put("loginEmail", req.email());
            payload.put("cpfCnpj", req.cnpj());
            if (!isBlank(req.mobilePhone())) payload.put("mobilePhone", req.mobilePhone());
            if (!isBlank(req.telefone())) payload.put("phone", req.telefone());
            if (req.incomeValue() != null) payload.put("incomeValue", req.incomeValue());
            if (!isBlank(req.birthDate())) payload.put("birthDate", req.birthDate());

            // PJ -> companyType; PF -> nao envia
            String cnpjDigits = req.cnpj() != null ? req.cnpj().replaceAll("\\D", "") : "";
            boolean isPJ = cnpjDigits.length() == 14;
            if (isPJ) {
                payload.put("companyType", isBlank(req.companyType()) ? "LIMITED" : req.companyType());
                payload.put("personType", "JURIDICA");
            } else {
                payload.put("personType", "FISICA");
            }

            // Endereco
            if (!isBlank(req.endereco())) payload.put("address", req.endereco());
            if (!isBlank(req.addressNumber())) payload.put("addressNumber", req.addressNumber());
            if (!isBlank(req.complemento())) payload.put("complement", req.complemento());
            if (!isBlank(req.bairro())) payload.put("province", req.bairro());
            if (!isBlank(req.cep())) payload.put("postalCode", req.cep());

            JsonNode body = post("/accounts", payload);
            String accountId = body.path("id").asText();
            String walletId = body.path("walletId").asText(null);
            String apiKey = body.path("apiKey").asText(null);

            // Sandbox aprova automaticamente; producao exige envio de documentos (KYC).
            // Status inicial aqui retornado como APROVADA se walletId veio preenchido.
            String status = !isBlank(walletId) ? "APROVADA" : "EM_ANALISE";

            return new SubcontaResponse(
                PROVIDER,
                !isBlank(walletId) ? walletId : accountId,
                status,
                null,
                apiKey,
                body.toString()
            );
        } catch (Exception e) {
            log.error("[AsaasGateway] Erro ao criar subconta: {}", e.getMessage(), e);
            throw new RuntimeException("Erro Asaas onboarding: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validarAssinaturaWebhook(String payload, String assinatura) {
        String secret = props.getAsaas().getWebhookSecret();
        if (isBlank(secret)) {
            if (env.acceptsProfiles(Profiles.of("prod"))) {
                log.error("[AsaasGateway] webhook-secret nao configurado em prod — rejeitando webhook");
                return false;
            }
            log.warn("[AsaasGateway] webhook-secret nao configurado — aceitando webhook sem validacao (dev only)");
            return true;
        }
        if (isBlank(assinatura)) return false;
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            // #DB205: compare constant-time para evitar timing attack que revele assinatura
            byte[] expected = hex.toString().toLowerCase().getBytes(StandardCharsets.US_ASCII);
            byte[] received = assinatura.toLowerCase().getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, received);
        } catch (Exception e) {
            log.error("[AsaasGateway] Erro ao validar assinatura webhook: {}", e.getMessage());
            return false;
        }
    }

    // ─── Helpers HTTP ───────────────────────────────────────────────

    private String obterOuCriarCustomer(CobrancaRequest req) throws Exception {
        // #DB207: normalizar cpfCnpj para digitos-apenas e URL-encode evita duplicata e injection.
        String cpfCnpjDigits = normalizeCpfCnpj(req.cpfCnpjPagador());
        if (!isBlank(cpfCnpjDigits)) {
            JsonNode list = get("/customers?cpfCnpj=" + URLEncoder.encode(cpfCnpjDigits, StandardCharsets.UTF_8));
            JsonNode data = list.path("data");
            if (data.isArray() && data.size() > 0) return data.get(0).path("id").asText();
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", req.nomePagador() != null ? req.nomePagador() : "Cliente Naviera");
        if (!isBlank(cpfCnpjDigits)) payload.put("cpfCnpj", cpfCnpjDigits);
        if (!isBlank(req.emailPagador())) payload.put("email", req.emailPagador());
        JsonNode body = post("/customers", payload);
        return body.path("id").asText();
    }

    private static String normalizeCpfCnpj(String raw) {
        return raw == null ? null : raw.replaceAll("\\D", "");
    }

    private JsonNode post(String path, Map<String, Object> body) throws Exception {
        ResponseEntity<String> res = rest.exchange(
            props.getAsaas().getBaseUrl() + path,
            HttpMethod.POST,
            new HttpEntity<>(body, headers()),
            String.class);
        return parseBody(res, path);
    }

    private JsonNode get(String path) throws Exception {
        ResponseEntity<String> res = rest.exchange(
            props.getAsaas().getBaseUrl() + path,
            HttpMethod.GET,
            new HttpEntity<>(headers()),
            String.class);
        return parseBody(res, path);
    }

    // #302: retry para GETs idempotentes (pixQrCode/identificationField) — recurso pode demorar
    //   poucos ms para ser gerado apos o POST. Backoff: 200ms, 500ms, 1s.
    private JsonNode getWithRetry(String path, int tentativas) throws Exception {
        Exception ultima = null;
        long[] backoffMs = {200L, 500L, 1000L};
        for (int i = 0; i < tentativas; i++) {
            try { return get(path); }
            catch (Exception e) {
                ultima = e;
                if (i < tentativas - 1) {
                    try { Thread.sleep(backoffMs[Math.min(i, backoffMs.length - 1)]); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ie; }
                }
            }
        }
        throw ultima != null ? ultima : new IllegalStateException("getWithRetry exhausted: " + path);
    }

    // #DR260: resposta sem body vira NPE em readTree — tratar como erro upstream.
    private JsonNode parseBody(ResponseEntity<String> res, String path) throws Exception {
        String body = res.getBody();
        if (body == null || body.isBlank()) {
            throw ApiException.badGateway("Resposta Asaas vazia em " + path + " (status=" + res.getStatusCode() + ")");
        }
        return mapper.readTree(body);
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("access_token", props.getAsaas().getApiKey());
        h.set("User-Agent", "Naviera/1.0");
        return h;
    }

    private static String mapForma(String forma) {
        return switch (forma) {
            case "PIX" -> "PIX";
            case "CARTAO" -> "CREDIT_CARD";
            case "BOLETO" -> "BOLETO";
            default -> throw new IllegalArgumentException("Forma invalida para PSP: " + forma);
        };
    }

    /** Mapeia status Asaas -> status interno (psp_cobrancas.psp_status). */
    private static String mapStatus(String asaasStatus) {
        if (asaasStatus == null) return "PENDENTE";
        return switch (asaasStatus) {
            case "CONFIRMED", "RECEIVED", "RECEIVED_IN_CASH" -> "CONFIRMADA";
            case "OVERDUE" -> "VENCIDA";
            case "REFUNDED", "REFUND_REQUESTED", "CHARGEBACK_REQUESTED", "CHARGEBACK_DISPUTE" -> "ESTORNADA";
            case "DELETED" -> "CANCELADA";
            default -> "PENDENTE";
        };
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
