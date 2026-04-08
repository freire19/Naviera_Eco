package com.naviera.api.service;

import com.naviera.api.dto.LojaDTO;
import com.naviera.api.dto.PedidoDTO;
import com.naviera.api.model.*;
import com.naviera.api.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LojaService {
    private final LojaParceiraRepository lojaRepo;
    private final PedidoLojaRepository pedidoRepo;
    private final ClienteAppRepository clienteRepo;

    public LojaService(LojaParceiraRepository lojaRepo, PedidoLojaRepository pedidoRepo, ClienteAppRepository clienteRepo) {
        this.lojaRepo = lojaRepo; this.pedidoRepo = pedidoRepo; this.clienteRepo = clienteRepo;
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

    public PedidoLoja vincularFrete(Long pedidoId, Long idFrete, String codigoRastreio) {
        PedidoLoja p = pedidoRepo.findById(pedidoId)
            .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
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

    private PedidoDTO toPedidoDTO(PedidoLoja p) {
        String clienteNome = clienteRepo.findById(p.getIdClienteComprador())
            .map(ClienteApp::getNome).orElse("Desconhecido");
        return new PedidoDTO(p.getId(), p.getNumeroPedido(), clienteNome,
            p.getCidadeDestino(), p.getDescricaoItens(), p.getValorTotal(),
            p.getStatus(), p.getIdFrete(), p.getCodigoRastreio(),
            p.getDataPedido() != null ? p.getDataPedido().toString() : null);
    }
}
