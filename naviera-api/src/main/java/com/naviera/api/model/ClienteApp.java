package com.naviera.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "clientes_app")
public class ClienteApp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String documento;
    @Column(name = "tipo_documento", length = 4)
    private String tipoDocumento = "CPF";
    @Column(nullable = false, length = 200)
    private String nome;
    private String email;
    private String telefone;
    private String cidade;
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;
    @Column(name = "foto_url") private String fotoUrl;
    private Boolean ativo = true;
    @Column(name = "data_cadastro") private LocalDateTime dataCadastro;
    @Column(name = "ultimo_acesso") private LocalDateTime ultimoAcesso;
    @Column(name = "cnpj_matriz") private String cnpjMatriz;
    @Column(name = "responsavel_nome") private String responsavelNome;

    // Getters e Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getDocumento() { return documento; } public void setDocumento(String d) { this.documento = d; }
    public String getTipoDocumento() { return tipoDocumento; } public void setTipoDocumento(String t) { this.tipoDocumento = t; }
    public String getNome() { return nome; } public void setNome(String n) { this.nome = n; }
    public String getEmail() { return email; } public void setEmail(String e) { this.email = e; }
    public String getTelefone() { return telefone; } public void setTelefone(String t) { this.telefone = t; }
    public String getCidade() { return cidade; } public void setCidade(String c) { this.cidade = c; }
    public String getSenhaHash() { return senhaHash; } public void setSenhaHash(String s) { this.senhaHash = s; }
    public String getFotoUrl() { return fotoUrl; } public void setFotoUrl(String f) { this.fotoUrl = f; }
    public Boolean getAtivo() { return ativo; } public void setAtivo(Boolean a) { this.ativo = a; }
    public LocalDateTime getUltimoAcesso() { return ultimoAcesso; } public void setUltimoAcesso(LocalDateTime u) { this.ultimoAcesso = u; }
    public String getCnpjMatriz() { return cnpjMatriz; } public void setCnpjMatriz(String c) { this.cnpjMatriz = c; }
    public String getResponsavelNome() { return responsavelNome; } public void setResponsavelNome(String r) { this.responsavelNome = r; }
}
