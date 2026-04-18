package com.naviera.api.psp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Body do POST /psp/onboarding. Dados coletados pela tela Recebimento
 * no web admin pra criar a subconta Asaas da empresa logada.
 */
public record OnboardingRequest(
    @NotBlank(message = "Razao social obrigatoria")
    String razaoSocial,

    @NotBlank(message = "CNPJ obrigatorio")
    String cnpj,

    @NotBlank(message = "Email obrigatorio")
    @Email
    String email,

    String telefone,
    String mobilePhone,
    String responsavelNome,
    String responsavelCpf,
    String birthDate,
    String companyType,
    BigDecimal incomeValue,
    String endereco,
    String addressNumber,
    String complemento,
    String bairro,
    String cep,
    String cidade,
    String estado
) {}
