package dao;

import java.util.ArrayList;
import java.util.List;

public class CidadeDAO {

    public List<String> buscarTodasCidades() {
        // Exemplo simples: retorna lista fixa.
        // Ajuste conforme sua tabela real, se desejar.
        List<String> lista = new ArrayList<>();
        lista.add("Manaus");
        lista.add("Jutaí");
        lista.add("Fonte Boa");
        return lista;
    }
}
