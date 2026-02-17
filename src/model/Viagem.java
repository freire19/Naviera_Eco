package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Viagem {
    private Long id;
    private LocalDate dataViagem;
    private Long idHorarioSaida;
    private String descricaoHorarioSaida;
    private LocalDate dataChegada;
    private String descricao;
    private boolean ativa; 
    private boolean isAtual; 

    private Long idEmbarcacao;
    private Long idRota;
    private String nomeEmbarcacao;
    private String nomeRotaConcatenado;
    private String origem;
    private String destino;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDate getDataViagem() { return dataViagem; }
    public void setDataViagem(LocalDate dataViagem) { this.dataViagem = dataViagem; }
    
    public Long getIdHorarioSaida() { return idHorarioSaida; }
    public void setIdHorarioSaida(Long idHorarioSaida) { this.idHorarioSaida = idHorarioSaida; }
    
    public String getDescricaoHorarioSaida() { return descricaoHorarioSaida; }
    public void setDescricaoHorarioSaida(String descricaoHorarioSaida) { this.descricaoHorarioSaida = descricaoHorarioSaida; }
    
    public LocalDate getDataChegada() { return dataChegada; }
    public void setDataChegada(LocalDate dataChegada) { this.dataChegada = dataChegada; }

    // --- CORREÇÃO DE COMPATIBILIDADE (SOLICITADO) ---
    public LocalDate getPrevisaoChegada() { 
        return dataChegada; 
    }
    // ------------------------------------------------
    
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    public boolean getIsAtual() { return isAtual; }
    public void setIsAtual(boolean isAtual) { this.isAtual = isAtual; }

    public Long getIdEmbarcacao() { return idEmbarcacao; }
    public void setIdEmbarcacao(Long idEmbarcacao) { this.idEmbarcacao = idEmbarcacao; }
    
    public Long getIdRota() { return idRota; }
    public void setIdRota(Long idRota) { this.idRota = idRota; }
    
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    
    public String getNomeEmbarcacao() { return nomeEmbarcacao; }
    public void setNomeEmbarcacao(String nomeEmbarcacao) { this.nomeEmbarcacao = nomeEmbarcacao; }
    
    public String getNomeRotaConcatenado() { return nomeRotaConcatenado; }
    public void setNomeRotaConcatenado(String nomeRotaConcatenado) { this.nomeRotaConcatenado = nomeRotaConcatenado; }

    public String getHorarioSaidaStr() { 
        return descricaoHorarioSaida != null ? descricaoHorarioSaida : "N/A";
    }

    public String getDataViagemStr() {
        return dataViagem != null ? dataViagem.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    public String getDataChegadaStr() {
        return dataChegada != null ? dataChegada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    @Override
    public String toString() {
        String dataStr = getDataViagemStr();
        String rotaStrPart = (origem != null && destino != null) ? origem + " - " + destino : "N/A";
        return String.format("%d - %s (%s)", id, dataStr, rotaStrPart);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Viagem viagem = (Viagem) o;
        return Objects.equals(id, viagem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}