package com.naviera.api.psp;

import com.naviera.api.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestra onboarding de empresa no PSP:
 *   - valida se empresa ja tem subconta
 *   - chama PspGateway.criarSubconta()
 *   - grava psp_subconta_id e psp_provider na tabela empresas
 */
@Service
public class EmpresaPspService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaPspService.class);

    private final PspGateway gateway;
    private final JdbcTemplate jdbc;

    public EmpresaPspService(PspGateway gateway, JdbcTemplate jdbc) {
        this.gateway = gateway;
        this.jdbc = jdbc;
    }

    /** Retorna status PSP da empresa logada: provider, subcontaId, active. */
    public Map<String, Object> status(Integer empresaId) {
        var rows = jdbc.queryForList(
            "SELECT psp_provider, psp_subconta_id FROM empresas WHERE id = ?",
            empresaId);
        if (rows.isEmpty()) throw ApiException.notFound("Empresa nao encontrada");

        String provider = (String) rows.get(0).get("psp_provider");
        String subcontaId = (String) rows.get(0).get("psp_subconta_id");
        Map<String, Object> resp = new HashMap<>();
        resp.put("provider", provider);
        resp.put("subcontaId", subcontaId);
        resp.put("status", subcontaId != null && !subcontaId.isBlank() ? "ATIVA" : "SEM_SUBCONTA");
        return resp;
    }

    @Transactional
    public Map<String, Object> onboarding(Integer empresaId, OnboardingRequest req) {
        var rows = jdbc.queryForList(
            "SELECT psp_subconta_id FROM empresas WHERE id = ?", empresaId);
        if (rows.isEmpty()) throw ApiException.notFound("Empresa nao encontrada");
        String existing = (String) rows.get(0).get("psp_subconta_id");
        if (existing != null && !existing.isBlank()) {
            throw ApiException.conflict("Empresa ja tem subconta cadastrada: " + existing);
        }

        SubcontaRequest subReq = new SubcontaRequest(
            empresaId,
            req.razaoSocial(),
            req.cnpj(),
            req.email(),
            req.telefone(),
            req.mobilePhone(),
            req.responsavelNome(),
            req.responsavelCpf(),
            req.birthDate(),
            req.companyType(),
            req.incomeValue(),
            req.endereco(),
            req.addressNumber(),
            req.complemento(),
            req.bairro(),
            req.cep(),
            req.cidade(),
            req.estado()
        );

        SubcontaResponse resp = gateway.criarSubconta(subReq);

        jdbc.update(
            "UPDATE empresas SET psp_provider = ?, psp_subconta_id = ? WHERE id = ?",
            resp.pspProvider(), resp.pspSubcontaId(), empresaId);

        log.info("[EmpresaPspService] Subconta criada para empresa {} — provider={}, walletId={}, status={}",
            empresaId, resp.pspProvider(), resp.pspSubcontaId(), resp.status());

        Map<String, Object> out = new HashMap<>();
        out.put("provider", resp.pspProvider());
        out.put("subcontaId", resp.pspSubcontaId());
        out.put("status", resp.status());
        if (resp.onboardingUrl() != null) out.put("onboardingUrl", resp.onboardingUrl());
        out.put("mensagem", "APROVADA".equals(resp.status())
            ? "Subconta criada e aprovada. Ja pode receber pagamentos."
            : "Subconta criada, aguardando KYC. Finalize o envio de documentos.");
        return out;
    }
}
