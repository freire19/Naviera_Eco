package com.naviera.api.repository;
import com.naviera.api.model.ClienteApp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteAppRepository extends JpaRepository<ClienteApp, Long> {
    Optional<ClienteApp> findByDocumentoAndAtivoTrue(String documento);
    boolean existsByDocumento(String documento);
}
