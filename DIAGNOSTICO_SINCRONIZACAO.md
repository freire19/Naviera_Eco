# 🔍 GUIA DE DIAGNÓSTICO - SINCRONIZAÇÃO

## ❌ Problemas Identificados

1. **Mensagem "Falha na conexão" persiste** - mesmo com sincronização bem-sucedida
2. **0 registros enviados/recebidos** - indica que todos já estão sincronizados
3. **Não encontra dados no PostgreSQL** - precisa verificar se as tabelas têm dados

---

## 📋 PASSO A PASSO PARA CORRIGIR

### **Etapa 1: Recompilar o Projeto Desktop**

1. Abra o NetBeans
2. Clique com botão direito em **SistemaEmbarcacaoProjeto_Novo**
3. Selecione **Clean and Build** (Shift+F11)
4. Aguarde compilação terminar

**✅ Correção aplicada:** Agora a mensagem de erro será limpa quando a sincronização for bem-sucedida.

---

### **Etapa 2: Verificar Dados no PostgreSQL**

Abra o **pgAdmin 4** e execute os scripts:

#### **Script 1: Verificar se há dados nas tabelas**

```sql
-- Abra o arquivo: database_scripts/verificar_dados_banco.sql
-- Ou copie e cole este comando:

SELECT 
    'passageiros' as tabela, COUNT(*) as total_registros 
FROM passageiros
UNION ALL
SELECT 
    'passagens' as tabela, COUNT(*) as total_registros 
FROM passagens
UNION ALL
SELECT 
    'viagens' as tabela, COUNT(*) as total_registros 
FROM viagens;
```

**❓ O que esperar:**
- Se mostrar **0 registros**: As tabelas estão vazias (você precisa criar dados no sistema desktop)
- Se mostrar **números maiores que 0**: As tabelas têm dados

---

### **Etapa 3: Marcar Registros para Teste**

Se as tabelas **TIVEREM DADOS**, execute este script para criar registros não sincronizados:

```sql
-- Abra o arquivo: database_scripts/002_marcar_registros_para_teste.sql
-- Ou copie e cole:

UPDATE passageiros 
SET sincronizado = FALSE 
WHERE id IN (SELECT id FROM passageiros LIMIT 5);

UPDATE passagens 
SET sincronizado = FALSE 
WHERE id IN (SELECT id FROM passagens LIMIT 10);

UPDATE viagens 
SET sincronizado = FALSE 
WHERE id IN (SELECT id FROM viagens LIMIT 3);
```

**✅ Resultado esperado:** Agora você terá registros pendentes para sincronizar.

---

### **Etapa 4: Testar Sincronização**

1. Execute o sistema desktop (F6)
2. Faça login
3. Clique em **Sincronização** → **Configurar Sincronização**
4. Verifique se a URL está: `http://localhost:8080`
5. Clique em **🔗 Testar Conexão**
   - Deve mostrar: **✅ Conexão bem-sucedida!**
6. Clique em **🔄 Atualizar Contagem**
   - Deve mostrar os números de pendências (ex: 5 passageiros, 10 passagens, 3 viagens)
7. Clique em **🚀 Sincronizar Agora**
   - Agora deve mostrar: **Registros enviados: X** (onde X > 0)
   - A mensagem "❌ Falha na conexão!" **NÃO DEVE** aparecer mais

---

### **Etapa 5: Verificar Sincronização no Banco**

Após sincronizar, execute no pgAdmin:

```sql
-- Ver registros sincronizados
SELECT 
    'passageiros' as tabela,
    COUNT(*) FILTER (WHERE sincronizado = TRUE) as sincronizados,
    COUNT(*) FILTER (WHERE sincronizado = FALSE) as nao_sincronizados
FROM passageiros
UNION ALL
SELECT 
    'passagens' as tabela,
    COUNT(*) FILTER (WHERE sincronizado = TRUE) as sincronizados,
    COUNT(*) FILTER (WHERE sincronizado = FALSE) as nao_sincronizados
FROM passagens;
```

**✅ Resultado esperado:** 
- Os registros que estavam com `sincronizado = FALSE` devem agora estar `sincronizado = TRUE`

---

## 🔧 SOBRE O CHECKBOX "HABILITAR SINCRONIZAÇÃO AUTOMÁTICA"

O checkbox **ESTÁ NA TELA**, mas pode não estar visível por questões de CSS. 

**Localização:** Seção "🔄 Sincronização Automática", logo abaixo da seção "Servidor".

Se não estiver aparecendo:
1. Verifique se a janela está com scroll (role para baixo)
2. Redimensione a janela (ela é redimensionável)
3. Maximize a janela

---

## ❓ PERGUNTAS PARA DIAGNÓSTICO

**Por favor, responda:**

1. Quando você executa o Script 1 (verificar dados), quantos registros aparecem em cada tabela?
2. O checkbox "Habilitar sincronização automática" aparece na tela? (pode estar mais abaixo)
3. Após executar o Script 2 (marcar registros), o botão "Atualizar Contagem" mostra números > 0?

---

## 📁 ARQUIVOS CRIADOS

- `database_scripts/002_marcar_registros_para_teste.sql` - Marca registros como não sincronizados
- `database_scripts/verificar_dados_banco.sql` - Scripts de diagnóstico
- Este arquivo: `DIAGNOSTICO_SINCRONIZACAO.md`

---

## ✅ CORREÇÕES APLICADAS NO CÓDIGO

1. **ConfigurarSincronizacaoController.java:**
   - Mensagem de erro é limpa ao abrir a tela
   - Mensagem de erro é limpa ao iniciar sincronização
   - Mensagem de sucesso é exibida após sincronização bem-sucedida
   - Validação de URL vazia

---

Compile o projeto e siga o passo a passo. Me avise os resultados de cada etapa!
