package com.naviera.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class CadastrosService {
    private final JdbcTemplate jdbc;

    public CadastrosService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listarUsuarios(Integer empresaId) {
        return jdbc.queryForList(
            "SELECT id, nome, email, funcao, permissao, excluido FROM usuarios WHERE (excluido = FALSE OR excluido IS NULL) AND empresa_id = ? ORDER BY nome",
            empresaId);
    }

    public List<Map<String, Object>> listarConferentes(Integer empresaId) {
        return jdbc.queryForList("SELECT * FROM conferentes WHERE empresa_id = ? ORDER BY nome", empresaId);
    }

    public List<Map<String, Object>> listarCaixas(Integer empresaId) {
        return jdbc.queryForList("SELECT * FROM caixas WHERE empresa_id = ? ORDER BY nome", empresaId);
    }

    public List<Map<String, Object>> listarTarifas(Integer empresaId) {
        return jdbc.queryForList("""
            SELECT t.*, r.origem, r.destino, tp.nome AS nome_tipo_passageiro
            FROM tarifas t
            LEFT JOIN rotas r ON t.id_rota = r.id_rota
            LEFT JOIN tipo_passageiro tp ON t.id_tipo_passageiro = tp.id_tipo_passageiro
            WHERE t.empresa_id = ?
            ORDER BY r.origem, tp.nome""", empresaId);
    }

    public List<Map<String, Object>> listarTiposPassageiro(Integer empresaId) {
        return jdbc.queryForList("SELECT * FROM tipo_passageiro WHERE empresa_id = ? ORDER BY nome", empresaId);
    }

    public Map<String, Object> buscarEmpresa(Integer empresaId) {
        var list = jdbc.queryForList("SELECT * FROM configuracao_empresa WHERE empresa_id = ?", empresaId);
        return list.isEmpty() ? Map.of() : list.get(0);
    }

    public List<Map<String, Object>> listarClientesEncomenda(Integer empresaId) {
        return jdbc.queryForList("SELECT * FROM cad_clientes_encomenda WHERE empresa_id = ? ORDER BY nome_cliente", empresaId);
    }

    public List<Map<String, Object>> listarItensEncomenda(Integer empresaId) {
        return jdbc.queryForList("SELECT * FROM itens_encomenda_padrao WHERE ativo = TRUE AND empresa_id = ? ORDER BY nome_item", empresaId);
    }

    public List<Map<String, Object>> listarItensFrete(Integer empresaId) {
        return jdbc.queryForList("SELECT * FROM itens_frete_padrao WHERE ativo = TRUE AND empresa_id = ? ORDER BY nome_item", empresaId);
    }
}
