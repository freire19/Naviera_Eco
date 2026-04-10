package com.naviera.api.repository;

import com.naviera.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u WHERE (LOWER(u.nome) = LOWER(:login) OR LOWER(u.email) = LOWER(:login)) AND (u.excluido = false OR u.excluido IS NULL)")
    Optional<Usuario> findByLogin(@Param("login") String login);
}
