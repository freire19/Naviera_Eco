package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO mínimo de Remetentes. O controller utiliza listarNomes() e inserir().
 * Se você já possui sua própria tabela “remetentes”, adapte a seguinte lógica
 * de SELECT/INSERT para a sua implementação real.
 */
public class RemetenteDAO {

    public List<String> listarNomes() {
        List<String> lista = new ArrayList<>();
        // Exemplo em‐memória: substitua por um SELECT real no seu DB 
        // (por ex.: SELECT DISTINCT nome_remetente FROM remetentes ORDER BY nome_remetente)
        return lista;
    }

    public void inserir(String nome) {
        // Em produção, faça INSERT INTO remetentes (nome_remetente) VALUES (?);
        System.out.println("Simulando inserção de Remetente: " + nome);
    }
}
