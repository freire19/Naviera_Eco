package com.naviera.api.repository;
import com.naviera.api.model.LojaParceira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface LojaParceiraRepository extends JpaRepository<LojaParceira, Long> {
    List<LojaParceira> findByAtivaTrue();
    Optional<LojaParceira> findByIdClienteApp(Long idClienteApp);
    @Query(value = "SELECT * FROM lojas_parceiras WHERE ativa = true AND :cidade = ANY(rotas_atendidas)", nativeQuery = true)
    List<LojaParceira> findByCidade(String cidade);
}
