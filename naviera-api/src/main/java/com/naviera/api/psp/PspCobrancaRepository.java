package com.naviera.api.psp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PspCobrancaRepository extends JpaRepository<PspCobranca, Long> {

    Optional<PspCobranca> findByPspProviderAndPspCobrancaId(String provider, String cobrancaId);

    List<PspCobranca> findByTipoOrigemAndOrigemIdOrderByDataCriacaoDesc(String tipoOrigem, Long origemId);

    /** Ultima cobranca criada para uma origem (PASSAGEM:123 / ENCOMENDA:456 / FRETE:789). */
    default Optional<PspCobranca> findUltimaPorOrigem(String tipoOrigem, Long origemId) {
        return findByTipoOrigemAndOrigemIdOrderByDataCriacaoDesc(tipoOrigem, origemId)
            .stream().findFirst();
    }
}
