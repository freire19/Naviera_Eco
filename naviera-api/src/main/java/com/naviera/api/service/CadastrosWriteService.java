package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class CadastrosWriteService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public CadastrosWriteService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    // --- ROTAS ---
    @Transactional
    public Map<String, Object> criarRota(Integer empresaId, Map<String, Object> dados) {
        jdbc.update("INSERT INTO rotas (origem, destino, empresa_id) VALUES (?, ?, ?)",
            dados.get("origem"), dados.get("destino"), empresaId);
        return Map.of("mensagem", "Rota criada");
    }

    @Transactional
    public Map<String, Object> atualizarRota(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("UPDATE rotas SET origem = ?, destino = ? WHERE id_rota = ? AND empresa_id = ?",
            dados.get("origem"), dados.get("destino"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Rota nao encontrada");
        return Map.of("mensagem", "Rota atualizada");
    }

    // --- EMBARCACOES ---
    @Transactional
    public Map<String, Object> criarEmbarcacao(Integer empresaId, Map<String, Object> dados) {
        jdbc.update("""
            INSERT INTO embarcacoes (nome, registro_capitania, capacidade_passageiros, observacoes, empresa_id)
            VALUES (?, ?, ?, ?, ?)""",
            dados.get("nome"), dados.get("registro_capitania"),
            dados.get("capacidade_passageiros"), dados.get("observacoes"), empresaId);
        return Map.of("mensagem", "Embarcacao criada");
    }

    @Transactional
    public Map<String, Object> atualizarEmbarcacao(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE embarcacoes SET nome = ?, registro_capitania = ?, capacidade_passageiros = ?, observacoes = ?
            WHERE id_embarcacao = ? AND empresa_id = ?""",
            dados.get("nome"), dados.get("registro_capitania"),
            dados.get("capacidade_passageiros"), dados.get("observacoes"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Embarcacao nao encontrada");
        return Map.of("mensagem", "Embarcacao atualizada");
    }

    // --- CONFERENTES ---
    @Transactional
    public Map<String, Object> criarConferente(Integer empresaId, Map<String, Object> dados) {
        jdbc.update("INSERT INTO conferentes (nome, empresa_id) VALUES (?, ?)",
            dados.get("nome"), empresaId);
        return Map.of("mensagem", "Conferente criado");
    }

    @Transactional
    public Map<String, Object> atualizarConferente(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("UPDATE conferentes SET nome = ? WHERE id = ? AND empresa_id = ?",
            dados.get("nome"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Conferente nao encontrado");
        return Map.of("mensagem", "Conferente atualizado");
    }

    // --- CAIXAS ---
    @Transactional
    public Map<String, Object> criarCaixa(Integer empresaId, Map<String, Object> dados) {
        jdbc.update("INSERT INTO caixas (nome, empresa_id) VALUES (?, ?)",
            dados.get("nome"), empresaId);
        return Map.of("mensagem", "Caixa criado");
    }

    @Transactional
    public Map<String, Object> atualizarCaixa(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("UPDATE caixas SET nome = ? WHERE id_caixa = ? AND empresa_id = ?",
            dados.get("nome"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Caixa nao encontrado");
        return Map.of("mensagem", "Caixa atualizado");
    }

    // --- CLIENTES ENCOMENDA ---
    @Transactional
    public Map<String, Object> criarClienteEncomenda(Integer empresaId, Map<String, Object> dados) {
        jdbc.update("""
            INSERT INTO cad_clientes_encomenda (nome_cliente, telefone, endereco, empresa_id)
            VALUES (?, ?, ?, ?)""",
            dados.get("nome_cliente"), dados.get("telefone"), dados.get("endereco"), empresaId);
        return Map.of("mensagem", "Cliente criado");
    }

    @Transactional
    public Map<String, Object> atualizarClienteEncomenda(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE cad_clientes_encomenda SET nome_cliente = ?, telefone = ?, endereco = ?
            WHERE id_cliente = ? AND empresa_id = ?""",
            dados.get("nome_cliente"), dados.get("telefone"), dados.get("endereco"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Cliente nao encontrado");
        return Map.of("mensagem", "Cliente atualizado");
    }

    // --- USUARIOS ---
    @Transactional
    public Map<String, Object> criarUsuario(Integer empresaId, Map<String, Object> dados) {
        jdbc.update("""
            INSERT INTO usuarios (nome, email, senha, funcao, permissao, empresa_id)
            VALUES (?, ?, ?, ?, ?, ?)""",
            dados.get("nome"), dados.get("email"), passwordEncoder.encode((String) dados.get("senha")),
            dados.get("funcao"), dados.get("permissao"), empresaId);
        return Map.of("mensagem", "Usuario criado");
    }

    @Transactional
    public Map<String, Object> atualizarUsuario(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE usuarios SET nome = ?, email = ?, funcao = ?, permissao = ?
            WHERE id = ? AND empresa_id = ?""",
            dados.get("nome"), dados.get("email"), dados.get("funcao"), dados.get("permissao"),
            id, empresaId);
        if (rows == 0) throw ApiException.notFound("Usuario nao encontrado");
        return Map.of("mensagem", "Usuario atualizado");
    }

    // --- TARIFAS ---
    @Transactional
    public Map<String, Object> criarTarifa(Integer empresaId, Map<String, Object> dados) {
        jdbc.update("""
            INSERT INTO tarifas (id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao,
                valor_cargas, valor_desconto, empresa_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)""",
            dados.get("id_rota"), dados.get("id_tipo_passagem"),
            dados.get("valor_transporte"), dados.get("valor_alimentacao"),
            dados.get("valor_cargas"), dados.get("valor_desconto"), empresaId);
        return Map.of("mensagem", "Tarifa criada");
    }

    @Transactional
    public Map<String, Object> atualizarTarifa(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE tarifas SET id_rota = ?, id_tipo_passagem = ?, valor_transporte = ?,
                valor_alimentacao = ?, valor_cargas = ?, valor_desconto = ?
            WHERE id_tarifa = ? AND empresa_id = ?""",
            dados.get("id_rota"), dados.get("id_tipo_passagem"),
            dados.get("valor_transporte"), dados.get("valor_alimentacao"),
            dados.get("valor_cargas"), dados.get("valor_desconto"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Tarifa nao encontrada");
        return Map.of("mensagem", "Tarifa atualizada");
    }
}
