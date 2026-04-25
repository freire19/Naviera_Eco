package com.naviera.api.repository;
import com.naviera.api.model.Rota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RotaRepository extends JpaRepository<Rota, Long> {
    List<Rota> findByEmpresaId(Integer empresaId);
}
