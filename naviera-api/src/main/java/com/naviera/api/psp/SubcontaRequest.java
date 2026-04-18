package com.naviera.api.psp;

import java.math.BigDecimal;

/**
 * Dados para criar subconta (marketplace) no PSP.
 * Campos necessarios para Asaas /accounts:
 *   name, email, loginEmail, cpfCnpj, mobilePhone, incomeValue,
 *   address, addressNumber, province (bairro), postalCode, companyType (se PJ), birthDate (se PF).
 */
public record SubcontaRequest(
    Integer empresaId,
    String razaoSocial,
    String cnpj,
    String email,
    String telefone,
    String mobilePhone,
    String responsavelNome,
    String responsavelCpf,
    String birthDate,           // ISO yyyy-MM-dd (obrigatorio se pessoa fisica)
    String companyType,         // LIMITED | INDIVIDUAL | ASSOCIATION | MEI
    BigDecimal incomeValue,     // faturamento mensal estimado
    String endereco,
    String addressNumber,
    String complemento,
    String bairro,
    String cep,
    String cidade,
    String estado
) {}
