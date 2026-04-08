package com.naviera.api.repository;
import com.naviera.api.model.Viagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ViagemRepository extends JpaRepository<Viagem, Long> {
    @Query("SELECT v FROM Viagem v WHERE v.ativa = true ORDER BY v.dataViagem DESC")
    List<Viagem> findAtivas();

    @Query("SELECT v FROM Viagem v WHERE v.isAtual = true ORDER BY v.dataViagem DESC")
    List<Viagem> findAtuais();
}
