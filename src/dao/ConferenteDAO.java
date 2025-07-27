package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO mínimo de Conferentes. O controller utiliza listarNomes() e inserir().
 * Se já existir a tabela “conferentes” no seu BD, troque estes métodos
 * para fazer SELECT/INSERT reais.
 */
public class ConferenteDAO {

    public List<String> listarNomes() {
        List<String> lista = new ArrayList<>();
        // Em produção: SELECT DISTINCT nome_conferente FROM conferentes ORDER BY nome_conferente
        return lista;
    }

    public void inserir(String nome) {
        // Em produção: INSERT INTO conferentes (nome_conferente) VALUES (?);
        System.out.println("Simulando inserção de Conferente: " + nome);
    }
}
