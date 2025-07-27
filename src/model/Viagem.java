package model;

import java.time.LocalDate;
// import java.time.format.DateTimeFormatter; // Não é necessário aqui, já que o toString() e getters formatados o fazem localmente ou você tem um utilitário.

/**
 * Modelo para Viagem.
 */
public class Viagem {
    private Long id; // Alterado de long para Long para permitir null
    private LocalDate dataViagem;
    private Long idHorarioSaida;      // NOVO: Chave estrangeira para aux_horarios_saida - Alterado para Long para permitir null
    private String descricaoHorarioSaida; // NOVO: Para exibir o nome do horário (ex: "12:00 HRS")

    private LocalDate dataChegada;
    private String descricao;
    private boolean ativa;
    private Long idEmbarcacao; // Alterado de long para Long para permitir null
    private Long idRota;       // Alterado de long para Long para permitir null

    private String nomeEmbarcacao;
    private String nomeRotaConcatenado;


    public Long getId() { // Getter também reflete a mudança para Long
        return id;
    }
    public void setId(Long id) { // Setter também reflete a mudança para Long
        this.id = id;
    }

    public LocalDate getDataViagem() {
        return dataViagem;
    }
    public void setDataViagem(LocalDate dataViagem) {
        this.dataViagem = dataViagem;
    }

    // NOVO: Getters e Setters para idHorarioSaida e descricaoHorarioSaida
    public Long getIdHorarioSaida() { // Getter também reflete a mudança para Long
        return idHorarioSaida;
    }
    public void setIdHorarioSaida(Long idHorarioSaida) { // Setter também reflete a mudança para Long
        this.idHorarioSaida = idHorarioSaida;
    }

    public String getDescricaoHorarioSaida() {
        return descricaoHorarioSaida;
    }
    public void setDescricaoHorarioSaida(String descricaoHorarioSaida) {
        this.descricaoHorarioSaida = descricaoHorarioSaida;
    }


    public LocalDate getDataChegada() {
        return dataChegada;
    }
    public void setDataChegada(LocalDate dataChegada) {
        this.dataChegada = dataChegada;
    }

    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isAtiva() {
        return ativa;
    }
    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Long getIdEmbarcacao() { // Getter também reflete a mudança para Long
        return idEmbarcacao;
    }
    public void setIdEmbarcacao(Long idEmbarcacao) { // Setter também reflete a mudança para Long
        this.idEmbarcacao = idEmbarcacao;
    }

    public Long getIdRota() { // Getter também reflete a mudança para Long
        return idRota;
    }
    public void setIdRota(Long idRota) { // Setter também reflete a mudança para Long
        this.idRota = idRota;
    }

    // Nota: Esses métodos de formatação devem usar DateTimeFormatter.ofPattern("dd/MM/yyyy")
    // Se eles usarem um formatter que você já definiu em outro lugar (ex: gui.util.Formato), melhor.
    public String getDataViagemStr() {
        return dataViagem != null ? dataViagem.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }
    public String getDataChegadaStr() {
        return dataChegada != null ? dataChegada.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    public String getHorarioSaidaStr() { // Mantém o nome do método para compatibilidade com FXML
        return descricaoHorarioSaida != null ? descricaoHorarioSaida : "N/A";
    }
    // Adicionado setter para HorarioSaidaStr se for usado em alguma tela de edição, embora seja redundante com setDescricaoHorarioSaida
    public void setHorarioSaidaStr(String horarioSaidaStr) {
        this.descricaoHorarioSaida = horarioSaidaStr;
    }


    public String getNomeEmbarcacao() {
        // Se idEmbarcacao é Long, pode ser null. Ajustar fallback.
        return nomeEmbarcacao != null ? nomeEmbarcacao : "Embarcação ID: " + (idEmbarcacao != null ? idEmbarcacao : "N/A");
    }
    public void setNomeEmbarcacao(String nomeEmbarcacao) {
        this.nomeEmbarcacao = nomeEmbarcacao;
    }

    public String getNomeRotaConcatenado() {
        // Se idRota é Long, pode ser null. Ajustar fallback.
        return nomeRotaConcatenado != null ? nomeRotaConcatenado : "Rota ID: " + (idRota != null ? idRota : "N/A");
    }
    public void setNomeRotaConcatenado(String nomeRotaConcatenado) {
        this.nomeRotaConcatenado = nomeRotaConcatenado;
    }

    @Override
    public String toString() {
        // Formato para ComboBox: "DD/MM/YYYY - HorárioCadastrado - Origem - Destino - Nome Embarcação"
        // Ex: "11/06/2025 - 12:00 HRS - MANAUS - JUTAI - F/B DEUS DE ALIANÇA V"
        String dataStr = dataViagem != null ? dataViagem.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A";
        String horaDescStr = descricaoHorarioSaida != null ? descricaoHorarioSaida : "N/A";
        String rotaStr = nomeRotaConcatenado != null ? nomeRotaConcatenado : "ID_Rota: " + (idRota != null ? idRota : "N/A");
        String embarcacaoStr = nomeEmbarcacao != null ? nomeEmbarcacao : "ID_Embarc: " + (idEmbarcacao != null ? idEmbarcacao : "N/A");

        return String.format("%s - %s - %s - %s", dataStr, horaDescStr, rotaStr, embarcacaoStr);
    }
}