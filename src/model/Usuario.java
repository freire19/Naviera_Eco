package model;

public class Usuario {
    private int id;             // Corresponde a id_usuario
    private String nomeCompleto; // Corresponde a nome_completo
    private String email;
    private transient String senhaPlana; // Temporario — nunca persiste, nunca serializa
    private String senhaHash;   // Hash BCrypt armazenado no banco
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
        this.senhaPlana = senha; // Temporario — DAO fará hash antes de persistir
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

    /**
     * Retorna a senha em texto plano (uso temporario pelo DAO para hash).
     * Nunca deve ser logada, serializada ou exibida.
     */
    public String getSenhaPlana() {
        return senhaPlana;
    }

    /**
     * Define senha em texto plano temporariamente. O DAO fara o hash antes de persistir.
     */
    public void setSenhaPlana(String senhaPlana) {
        this.senhaPlana = senhaPlana;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
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

    // DP033: equals/hashCode para collection performance
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario that = (Usuario) o;
        return id == that.id;
    }
    @Override
    public int hashCode() { return Integer.hashCode(id); }
}