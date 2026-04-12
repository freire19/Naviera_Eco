package com.naviera.api.service;

import com.naviera.api.dto.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messaging;

    public NotificationService(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /**
     * Envia notificacao para todos os clientes conectados ao topico da empresa.
     * Topic pattern: /topic/empresa/{empresaId}/notifications
     */
    public void notify(Integer empresaId, String type, String entity, Object entityId, String message) {
        String destination = "/topic/empresa/" + empresaId + "/notifications";
        NotificationMessage msg = new NotificationMessage(type, entity, entityId, message);
        try {
            messaging.convertAndSend(destination, msg);
            log.debug("WS notification sent: empresa={}, type={}, entity={}", empresaId, type, entity);
        } catch (Exception e) {
            log.warn("Failed to send WS notification: {}", e.getMessage());
        }
    }

    // ---- Convenience methods ----

    public void passagemCriada(Integer empresaId, Object id, String numeroBilhete) {
        notify(empresaId, "PASSAGEM_CRIADA", "passagem", id, "Passagem " + numeroBilhete + " criada");
    }

    public void passagemAtualizada(Integer empresaId, Long id) {
        notify(empresaId, "PASSAGEM_ATUALIZADA", "passagem", id, "Passagem atualizada");
    }

    public void encomendaCriada(Integer empresaId, Long id, String numeroEncomenda) {
        notify(empresaId, "ENCOMENDA_CRIADA", "encomenda", id, "Encomenda " + numeroEncomenda + " criada");
    }

    public void encomendaEntregue(Integer empresaId, Long id) {
        notify(empresaId, "ENCOMENDA_ENTREGUE", "encomenda", id, "Encomenda entregue");
    }

    public void freteCriado(Integer empresaId, Long id, Long numeroFrete) {
        notify(empresaId, "FRETE_CRIADO", "frete", id, "Frete " + numeroFrete + " criado");
    }

    public void viagemAtivada(Integer empresaId, Long id) {
        notify(empresaId, "VIAGEM_ATIVADA", "viagem", id, "Viagem ativada");
    }

    public void viagemCriada(Integer empresaId, Long id) {
        notify(empresaId, "VIAGEM_CRIADA", "viagem", id, "Nova viagem criada");
    }

    public void syncCompleto(Integer empresaId, String tabela, int recebidos, int enviados) {
        notify(empresaId, "SYNC_COMPLETO", tabela, null,
            "Sync " + tabela + ": " + recebidos + " recebidos, " + enviados + " enviados");
    }
}
