/**
 * Convencao de nomes para metodos DAO:
 *
 * - buscarPor*(criteria) — retorna um unico objeto filtrado por criterio (ex: buscarPorId, buscarPorNumero)
 * - listar*() — retorna uma colecao de objetos (ex: listarTodos, listarPorViagem, listarNomes)
 * - obter*(params) — retorna valor derivado/computado (ex: obterIdAuxiliar, obterProximoNumero)
 * - inserir/atualizar/excluir — operacoes de escrita
 * - salvar* — INSERT ou UPDATE combinado
 *
 * Novos metodos DEVEM seguir esta convencao.
 * Metodos existentes serao renomeados gradualmente.
 */
package dao;