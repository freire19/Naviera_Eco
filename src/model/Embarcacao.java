package model;

import java.math.BigDecimal; // Para capacidade de carga
import java.util.Objects; // Adicionado para Objects.equals e hashCode

public class Embarcacao {
    private Long id; // Alterado de int para Long
    private String nome;
    private String registroCapitania;
    private Integer capacidadePassageiros;
    private BigDecimal capacidadeCargaToneladas;
    private String observacoes;

    public Embarcacao() { // Construtor vazio
    }

    public Embarcacao(String nome) { // Construtor que estava faltando
        this.nome = nome;
    }
    
    // Construtor completo (opcional, mas pode ser útil)
    public Embarcacao(Long id, String nome, String registroCapitania, Integer capacidadePassageiros, BigDecimal capacidadeCargaToneladas, String observacoes) {
        this.id = id;
        this.nome = nome;
        this.registroCapitania = registroCapitania;
        this.capacidadePassageiros = capacidadePassageiros;
        this.capacidadeCargaToneladas = capacidadeCargaToneladas;
        this.observacoes = observacoes;
    }

    // Getters e Setters
    public Long getId() { return id; } // Getter também reflete a mudança para Long
    public void setId(Long id) { this.id = id; } // Setter também reflete a mudança para Long

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRegistroCapitania() { return registroCapitania; }
    public void setRegistroCapitania(String registroCapitania) { this.registroCapitania = registroCapitania; } // CORRIGIDO AQUI: digitação

    public Integer getCapacidadePassageiros() { return capacidadePassageiros; }
    public void setCapacidadePassageiros(Integer capacidadePassageiros) { this.capacidadePassageiros = capacidadePassageiros; }

    public BigDecimal getCapacidadeCargaToneladas() { return capacidadeCargaToneladas; }
    public void setCapacidadeCargaToneladas(BigDecimal capacidadeCargaToneladas) { this.capacidadeCargaToneladas = capacidadeCargaToneladas; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    @Override
    public String toString() {
        return nome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Embarcacao that = (Embarcacao) o;
        // Usar Objects.equals para comparar Longs, pois podem ser nulos.
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Usar Objects.hash para calcular hashCode de Long
        return Objects.hash(id);
    }
}