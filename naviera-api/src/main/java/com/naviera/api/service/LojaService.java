package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.LojaDTO;
import com.naviera.api.dto.PedidoDTO;
import com.naviera.api.model.*;
import com.naviera.api.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class LojaService {
    private final LojaParceiraRepository lojaRepo;
    private final PedidoLojaRepository pedidoRepo;
    private final ClienteAppRepository clienteRepo;
    private final JdbcTemplate jdbc;

    public LojaService(LojaParceiraRepository lojaRepo, PedidoLojaRepository pedidoRepo,
                       ClienteAppRepository clienteRepo, JdbcTemplate jdbc) {
        this.lojaRepo = lojaRepo; this.pedidoRepo = pedidoRepo;
        this.clienteRepo = clienteRepo; this.jdbc = jdbc;
    }

    public List<LojaDTO> listarAtivas() {
        return lojaRepo.findByAtivaTrue().stream().map(this::toDTO).toList();
    }

    public List<LojaDTO> listarPorCidade(String cidade) {
        return lojaRepo.findByCidade(cidade).stream().map(this::toDTO).toList();
    }

    public LojaDTO buscarMinhaLoja(Long clienteId) {
        return lojaRepo.findByIdClienteApp(clienteId).map(this::toDTO)
            .orElseThrow(() -> new RuntimeException("Loja não encontrada para este cliente"));
    }

    public List<PedidoDTO> pedidosDaLoja(Long clienteId) {
        LojaParceira loja = lojaRepo.findByIdClienteApp(clienteId)
            .orElseThrow(() -> new RuntimeException("Loja não encontrada"));
        return pedidoRepo.findByIdLojaOrderByDataPedidoDesc(loja.getId()).stream()
            .map(p -> toPedidoDTO(p)).toList();
    }

    public List<PedidoDTO> minhasCompras(Long clienteId) {
        return pedidoRepo.findByIdClienteCompradorOrderByDataPedidoDesc(clienteId).stream()
            .map(this::toPedidoDTO).toList();
    }

    /**
     * DS4-019 fix: verify the pedido belongs to the caller's loja before linking.
     */
    public PedidoLoja vincularFrete(Long pedidoId, Long idFrete, String codigoRastreio, Long clienteId) {
        LojaParceira loja = lojaRepo.findByIdClienteApp(clienteId)
            .orElseThrow(() -> ApiException.forbidden("Voce nao possui uma loja cadastrada"));
        PedidoLoja p = pedidoRepo.findById(pedidoId)
            .orElseThrow(() -> ApiException.notFound("Pedido nao encontrado"));
        if (!loja.getId().equals(p.getIdLoja())) {
            throw ApiException.forbidden("Este pedido nao pertence a sua loja");
        }
        p.setIdFrete(idFrete);
        p.setCodigoRastreio(codigoRastreio);
        p.setStatus("EM_TRANSITO");
        return pedidoRepo.save(p);
    }

    private LojaDTO toDTO(LojaParceira l) {
        return new LojaDTO(l.getId(), l.getNomeFantasia(), l.getSegmento(),
            l.getRotasAtendidas(), l.getVerificada(),
            l.getTotalEntregas(), l.getNotaMedia(),
            l.getTelefoneComercial(), l.getEmailComercial());
    }

    public List<Map<String, Object>> listarAvaliacoes(Long idLoja) {
        return jdbc.queryForList("""
            SELECT a.*, c.nome AS nome_cliente
            FROM avaliacoes_loja a
            LEFT JOIN clientes_app c ON a.id_cliente = c.id
            WHERE a.id_loja = ?
            ORDER BY a.data_avaliacao DESC""", idLoja);
    }

    @Transactional
    public Map<String, Object> criarAvaliacao(Long idLoja, Long clienteId, Object nota, Object comentario) {
        // #DL040: validar nota [1,5] e impedir duplicata por cliente
        if (nota == null) throw ApiException.badRequest("nota obrigatoria");
        int n;
        try { n = ((Number) nota).intValue(); } catch (ClassCastException e) {
            throw ApiException.badRequest("nota deve ser numero inteiro entre 1 e 5");
        }
        if (n < 1 || n > 5) throw ApiException.badRequest("nota deve estar entre 1 e 5");
        Integer existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM avaliacoes_loja WHERE id_loja = ? AND id_cliente = ?",
            Integer.class, idLoja, clienteId);
        if (existing != null && existing > 0) {
            throw ApiException.conflict("Voce ja avaliou esta loja");
        }
        jdbc.update("""
            INSERT INTO avaliacoes_loja (id_loja, id_cliente, nota, comentario)
            VALUES (?, ?, ?, ?)""",
            idLoja, clienteId, n, comentario);

        jdbc.update("""
            UPDATE lojas_parceiras SET nota_media = (
                SELECT AVG(nota) FROM avaliacoes_loja WHERE id_loja = ?
            ) WHERE id = ?""", idLoja, idLoja);

        return Map.of("mensagem", "Avaliacao registrada");
    }

    public Map<String, Object> stats(Long idLoja) {
        return jdbc.queryForMap("""
            SELECT
                COUNT(p.id) AS total_pedidos,
                COALESCE((SELECT AVG(nota) FROM avaliacoes_loja WHERE id_loja = ?), 0) AS nota_media,
                COALESCE((SELECT COUNT(*) FROM avaliacoes_loja WHERE id_loja = ?), 0) AS total_avaliacoes,
                COALESCE(SUM(p.valor_total), 0) AS receita_total
            FROM pedidos_loja p
            WHERE p.id_loja = ?""", idLoja, idLoja, idLoja);
    }

    private PedidoDTO toPedidoDTO(PedidoLoja p) {
        String clienteNome = clienteRepo.findById(p.getIdClienteComprador())
            .map(ClienteApp::getNome).orElse("Desconhecido");
        return new PedidoDTO(p.getId(), p.getNumeroPedido(), clienteNome,
            p.getCidadeDestino(), p.getDescricaoItens(), p.getValorTotal(),
            p.getStatus(), p.getIdFrete(), p.getCodigoRastreio(),
            p.getDataPedido() != null ? p.getDataPedido().toString() : null);
    }
}
