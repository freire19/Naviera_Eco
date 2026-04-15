package model;

public class Usuario {
    private int id;             // Corresponde a id (PK)
    private String nomeCompleto; // Corresponde a nome (usado tambem como login)
    private String email;
    // DS4-037 fix: char[] em vez de String — pode ser zerado apos uso (String fica no heap ate GC)
    private transient char[] senhaPlana; // Temporario — nunca persiste, nunca serializa
    private String senhaHash;   // Hash BCrypt — coluna 'senha' no banco
    private String funcao;
    private String permissoes; // Corresponde a permissao (singular no banco)
    private String loginUsuario; // Mapeado para 'nome' no banco (nao existe coluna login_usuario)
    private boolean ativo;      // Inverso de 'excluido' no banco (ativo=true → excluido=false)
    private boolean deveTrocarSenha; // Flag de troca obrigatoria no primeiro login

    public Usuario() {
    }

    // Construtor completo pode ser útil
    public Usuario(int id, String nomeCompleto, String loginUsuario, String email, String senha, String funcao, String permissoes, boolean ativo) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.loginUsuario = loginUsuario;
        this.email = email;
        this.senhaPlana = senha != null ? senha.toCharArray() : null; // Temporario — DAO fará hash
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
     * Retorna a senha em texto plano como String (uso temporario pelo DAO para hash).
     * DS4-037: armazenada internamente como char[] para permitir zeragem.
     */
    public String getSenhaPlana() {
        return senhaPlana != null ? new String(senhaPlana) : null;
    }

    /**
     * Define senha em texto plano temporariamente. O DAO fara o hash antes de persistir.
     */
    public void setSenhaPlana(String senhaPlana) {
        this.senhaPlana = senhaPlana != null ? senhaPlana.toCharArray() : null;
    }

    /** DS4-037 fix: zerar senha da memoria apos uso (chamar apos hash pelo DAO) */
    public void limparSenha() {
        if (senhaPlana != null) java.util.Arrays.fill(senhaPlana, '\0');
        senhaPlana = null;
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

    public boolean isDeveTrocarSenha() {
        return deveTrocarSenha;
    }
    public void setDeveTrocarSenha(boolean deveTrocarSenha) {
        this.deveTrocarSenha = deveTrocarSenha;
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