package com.naviera.api.psp;

import com.naviera.api.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final TransactionTemplate tx;

    public EmpresaPspService(PspGateway gateway, JdbcTemplate jdbc, TransactionTemplate tx) {
        this.gateway = gateway;
        this.jdbc = jdbc;
        this.tx = tx;
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

    // #237: onboarding NAO pode estar dentro de @Transactional — gateway.criarSubconta()
    //   faz HTTP que segura conexao DB durante latencia do Asaas. Usa TX curta para validacao e
    //   TX2 apenas para UPDATE final. #235: valida empresa.ativo antes de onboard.
    public Map<String, Object> onboarding(Integer empresaId, OnboardingRequest req) {
        Boolean validOk = tx.execute(st -> {
            var rows = jdbc.queryForList(
                "SELECT psp_subconta_id, ativo FROM empresas WHERE id = ?", empresaId);
            if (rows.isEmpty()) throw ApiException.notFound("Empresa nao encontrada");
            Object ativo = rows.get(0).get("ativo");
            if (ativo != null && Boolean.FALSE.equals(ativo)) {
                throw ApiException.forbidden("Empresa inativa — reative antes do onboarding");
            }
            String existing = (String) rows.get(0).get("psp_subconta_id");
            if (existing != null && !existing.isBlank()) {
                throw ApiException.conflict("Empresa ja tem subconta cadastrada: " + existing);
            }
            return true;
        });
        if (validOk == null) throw ApiException.badRequest("Falha ao validar empresa");

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

        // HTTP externo FORA de transacao (pool nao fica preso durante latencia do Asaas)
        SubcontaResponse resp = gateway.criarSubconta(subReq);

        // TX2 curta apenas para persistir o resultado
        tx.executeWithoutResult(s -> jdbc.update(
            "UPDATE empresas SET psp_provider = ?, psp_subconta_id = ? WHERE id = ?",
            resp.pspProvider(), resp.pspSubcontaId(), empresaId));

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
