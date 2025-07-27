package model;

public class Usuario {
    private int id;             // Corresponde a id_usuario
    private String nomeCompleto; // Corresponde a nome_completo
    private String email;
    private String senha;      // Senha em texto plano (o DAO fará o hash)
    private String funcao;
    private String permissoes; // Corresponde a permissoes
    private String loginUsuario; // Corresponde a login_usuario
    private boolean ativo;

    public Usuario() {
    }

    // Construtor completo pode ser útil
    public Usuario(int id, String nomeCompleto, String loginUsuario, String email, String senha, String funcao, String permissoes, boolean ativo) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.loginUsuario = loginUsuario;
        this.email = email;
        this.senha = senha; // Armazena temporariamente, DAO fará hash
        this.funcao = funcao;
        this.permissoes = permissoes;
        this.ativo = ativo;
    }


    // Getters e Setters para todos os campos

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }
    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getFuncao() {
        return funcao;
    }
    public void setFuncao(String funcao) {
        this.funcao = funcao;
    }

    public String getPermissoes() {
        return permissoes;
    }
    public void setPermissoes(String permissoes) {
        this.permissoes = permissoes;
    }

    public String getLoginUsuario() {
        return loginUsuario;
    }
    public void setLoginUsuario(String loginUsuario) {
        this.loginUsuario = loginUsuario;
    }

    public boolean isAtivo() {
        return ativo;
    }
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return nomeCompleto != null ? nomeCompleto : (loginUsuario != null ? loginUsuario : "ID: " + id) ;
    }
}