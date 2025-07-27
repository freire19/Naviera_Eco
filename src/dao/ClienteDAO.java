package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO mínimo de Clientes (Destinatários). O controller utiliza listarNomes() e inserir().
 * Se já tiver a tabela “clientes” no seu BD PostgreSQL, ajuste para SELECT/INSERT reais.
 */
public class ClienteDAO {

    public List<String> listarNomes() {
        List<String> lista = new ArrayList<>();
        // Exemplo (ajuste para seu schema PostgreSQL):
        // String sql = "SELECT DISTINCT nome_cliente FROM clientes ORDER BY nome_cliente";
        // try (Connection conn = ConexaoBD.getConnection();
        //      Statement stmt = conn.createStatement();
        //      ResultSet rs = stmt.executeQuery(sql)) {
        //    while (rs.next()) {
        //        lista.add(rs.getString("nome_cliente"));
        //    }
        // }
        return lista;
    }

    public void inserir(String nome) {
        // Em produção: 
        // String sql = "INSERT INTO clientes (nome_cliente) VALUES (?)";
        // try (Connection conn = ConexaoBD.getConnection();
        //      PreparedStatement ps = conn.prepareStatement(sql)) {
        //     ps.setString(1, nome);
        //     ps.executeUpdate();
        // }
        System.out.println("Simulando inserção de Cliente: " + nome);
    }
}