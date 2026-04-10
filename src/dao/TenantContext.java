package dao;

/**
 * Contexto do tenant (empresa) atual.
 * 
 * Armazena o empresa_id em ThreadLocal para que todos os DAOs
 * filtrem automaticamente por empresa sem precisar passar o parametro
 * em cada metodo.
 * 
 * USO:
 *   - No login, chamar TenantContext.setEmpresaId(id) 
 *   - Em qualquer DAO, chamar TenantContext.getEmpresaId()
 *   - O valor e lido do db.properties (empresa.id) na inicializacao
 * 
 * DESIGN:
 *   - Desktop: empresa_id fixo, lido de db.properties na inicializacao
 *   - API REST: empresa_id extraido do JWT a cada request
 *   - Ambos usam esta mesma classe
 */
public final class TenantContext {

    private static final ThreadLocal<Integer> currentTenant = new ThreadLocal<>();
    
    /** Empresa padrao (compatibilidade com instalacoes existentes) */
    private static volatile int defaultEmpresaId = 1;

    private TenantContext() {}

    /**
     * Define o empresa_id padrao (lido de db.properties na inicializacao).
     * Chamado uma vez no startup do desktop.
     */
    public static void setDefaultEmpresaId(int empresaId) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("empresa_id deve ser > 0, recebeu: " + empresaId);
        }
        defaultEmpresaId = empresaId;
    }

    /**
     * Define o empresa_id para a thread atual.
     * Usado pela API REST (um request = uma thread = um tenant).
     */
    public static void setEmpresaId(int empresaId) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("empresa_id deve ser > 0, recebeu: " + empresaId);
        }
        currentTenant.set(empresaId);
    }

    /**
     * Retorna o empresa_id da thread atual.
     * Se nao foi definido explicitamente, retorna o default (desktop).
     */
    public static int getEmpresaId() {
        Integer id = currentTenant.get();
        return (id != null) ? id : defaultEmpresaId;
    }

    /**
     * Limpa o tenant da thread atual.
     * DEVE ser chamado no finally de cada request HTTP (API REST)
     * para evitar vazamento de contexto entre requests.
     */
    public static void clear() {
        currentTenant.remove();
    }
}
