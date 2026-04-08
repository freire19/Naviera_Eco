package com.naviera.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "lojas_parceiras")
public class LojaParceira {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "id_cliente_app") private Long idClienteApp;
    @Column(name = "nome_fantasia") private String nomeFantasia;
    private String segmento;
    private String descricao;
    @Column(name = "telefone_comercial") private String telefoneComercial;
    @Column(name = "email_comercial") private String emailComercial;
    @Column(name = "rotas_atendidas", columnDefinition = "text[]")
    private String[] rotasAtendidas;
    private Boolean verificada = false;
    private Boolean ativa = true;
    @Column(name = "total_entregas") private Integer totalEntregas = 0;
    @Column(name = "nota_media") private BigDecimal notaMedia;
    @Column(name = "data_cadastro") private LocalDateTime dataCadastro;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getIdClienteApp() { return idClienteApp; } public void setIdClienteApp(Long i) { this.idClienteApp = i; }
    public String getNomeFantasia() { return nomeFantasia; } public void setNomeFantasia(String n) { this.nomeFantasia = n; }
    public String getSegmento() { return segmento; } public void setSegmento(String s) { this.segmento = s; }
    public String getDescricao() { return descricao; } public void setDescricao(String d) { this.descricao = d; }
    public String[] getRotasAtendidas() { return rotasAtendidas; } public void setRotasAtendidas(String[] r) { this.rotasAtendidas = r; }
    public Boolean getVerificada() { return verificada; } public void setVerificada(Boolean v) { this.verificada = v; }
    public Boolean getAtiva() { return ativa; }
    public Integer getTotalEntregas() { return totalEntregas; }
    public BigDecimal getNotaMedia() { return notaMedia; }
    public String getTelefoneComercial() { return telefoneComercial; }
    public String getEmailComercial() { return emailComercial; }
}
