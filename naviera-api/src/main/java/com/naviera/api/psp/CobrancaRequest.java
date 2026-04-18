package com.naviera.api.psp;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados para criar uma cobranca no PSP.
 *
 * @param empresaId        empresa dona da receita (dona da viagem/frete)
 * @param subcontaId       id da subconta da empresa no PSP (destino do split)
 * @param tipoOrigem       PASSAGEM, ENCOMENDA ou FRETE
 * @param origemId         id do registro na tabela origem
 * @param clienteAppId     id do cliente pagador em clientes_app
 * @param formaPagamento   PIX, CARTAO ou BOLETO (BARCO nao gera cobranca no PSP)
 * @param valorBruto       valor antes de qualquer desconto
 * @param descontoAplicado descontos aplicados (ex. 10% PIX que empresa absorve)
 * @param splitNavieraPct  percentual da Naviera (ex. 1.50 = 1,5%)
 * @param descricao        texto livre exibido no checkout/boleto
 * @param vencimento       data de vencimento (usado em boleto; null para PIX/Cartao)
 * @param cpfCnpjPagador   documento do cliente para preencher cobranca (obrigatorio no Asaas)
 * @param nomePagador      nome do cliente
 * @param emailPagador     email do cliente (opcional)
 */
public record CobrancaRequest(
    Integer empresaId,
    String subcontaId,
    String tipoOrigem,
    Long origemId,
    Long clienteAppId,
    String formaPagamento,
    BigDecimal valorBruto,
    BigDecimal descontoAplicado,
    BigDecimal splitNavieraPct,
    String descricao,
    LocalDate vencimento,
    String cpfCnpjPagador,
    String nomePagador,
    String emailPagador
) {}
