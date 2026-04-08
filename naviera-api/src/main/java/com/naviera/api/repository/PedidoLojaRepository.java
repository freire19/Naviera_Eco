package com.naviera.api.repository;
import com.naviera.api.model.PedidoLoja;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoLojaRepository extends JpaRepository<PedidoLoja, Long> {
    List<PedidoLoja> findByIdLojaOrderByDataPedidoDesc(Long idLoja);
    List<PedidoLoja> findByIdClienteCompradorOrderByDataPedidoDesc(Long idCliente);
    List<PedidoLoja> findByIdLojaAndStatus(Long idLoja, String status);
}
