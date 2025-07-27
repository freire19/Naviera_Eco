package model;

import java.util.Objects; // Adicionado para Objects.equals e hashCode

/**
 * Modelo mínimo para Rota, contendo apenas atributos usados no Controller.
 */
public class Rota {
    private Long id; // Alterado de long para Long
    private String origem;
    private String destino;

    public Long getId() { // Getter também reflete a mudança para Long
        return id;
    }
    public void setId(Long id) { // Setter também reflete a mudança para Long
        this.id = id;
    }

    public String getOrigem() {
        return origem;
    }
    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDestino() {
        return destino;
    }
    public void setDestino(String destino) {
        this.destino = destino;
    }

    @Override
    public String toString() {
        if (origem != null && destino != null) {
            return origem + " - " + destino;
        } else if (origem != null) { // Caso o destino seja nulo/vazio no DB
            return origem;
        }
        return ""; // Se ambos forem nulos, retorna vazio ou "N/A"
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rota rota = (Rota) o;
        // Usar Objects.equals para comparar Longs
        return Objects.equals(id, rota.id);
    }

    @Override
    public int hashCode() {
        // Usar Objects.hash para calcular hashCode de Long
        return Objects.hash(id);
    }
}