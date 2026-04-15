package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa um funcionario da empresa.
 * Extraido de GestaoFuncionariosController (DM033).
 */
public class Funcionario {
    private int id;
    private String nome, cpf, rg, ctps, telefone, endereco, cargo;
    private BigDecimal salario = BigDecimal.ZERO;
    private LocalDate dataAdmissao, dataNascimento, dataInicioCalculo;
    private boolean recebe13;
    private boolean ativo;
    private boolean isClt;
    private BigDecimal valorInss = BigDecimal.ZERO;
    private boolean descontarInss;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public String getCtps() { return ctps; }
    public void setCtps(String ctps) { this.ctps = ctps; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public BigDecimal getSalario() { return salario != null ? salario : BigDecimal.ZERO; }
    public void setSalario(BigDecimal salario) { this.salario = salario != null ? salario : BigDecimal.ZERO; }
    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate d) { this.dataAdmissao = d; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate d) { this.dataNascimento = d; }
    public LocalDate getDataInicioCalculo() { return dataInicioCalculo; }
    public void setDataInicioCalculo(LocalDate d) { this.dataInicioCalculo = d; }
    public boolean isRecebe13() { return recebe13; }
    public void setRecebe13(boolean v) { this.recebe13 = v; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean v) { this.ativo = v; }
    public boolean isClt() { return isClt; }
    public void setClt(boolean v) { this.isClt = v; }
    public BigDecimal getValorInss() { return valorInss != null ? valorInss : BigDecimal.ZERO; }
    public void setValorInss(BigDecimal v) { this.valorInss = v != null ? v : BigDecimal.ZERO; }
    public boolean isDescontarInss() { return descontarInss; }
    public void setDescontarInss(boolean v) { this.descontarInss = v; }

    @Override
    public String toString() { return nome + (ativo ? "" : " (INATIVO)"); }
}
