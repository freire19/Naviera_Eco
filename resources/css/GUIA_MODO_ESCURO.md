# Guia de Desenvolvimento - Modo Claro e Escuro

## ✅ Regras Automáticas Implementadas

O sistema agora possui CSS robusto que automaticamente aplica as cores corretas em **TODOS** os componentes JavaFX, tanto no modo claro quanto escuro.

## 🎨 Paleta de Cores

### Modo Claro
- **Fundo Geral**: #eceff1 (Cinza Claro)
- **Painéis/Cards**: #FFFFFF (Branco)
- **Campos de Input**: #FFFFFF (Branco)
- **Bordas**: #B0BEC5 (Cinza Médio)
- **Texto Principal**: #263238 (Cinza Escuro)
- **Destaque**: #0d47a1 (Azul Primário)

### Modo Escuro (Navy Blue)
- **Fundo Geral**: #121212 (Preto Suave)
- **Painéis/Cards**: #121212 (Preto)
- **Campos de Input**: #0a2456 (Azul Noite Profundo)
- **Bordas**: #1a3c7d (Azul Médio)
- **Texto Principal**: #FFFFFF (Branco Puro)
- **Texto Secundário**: #e0e0e0 (Cinza Claro)
- **Destaque**: #0d56df (Azul Vibrante)

## 📝 Boas Práticas ao Criar Novas Telas

### ✅ O QUE FAZER

1. **Não defina cores inline nos arquivos FXML**
   ```xml
   <!-- ❌ EVITAR -->
   <TextField style="-fx-background-color: white;" />
   
   <!-- ✅ CORRETO -->
   <TextField />
   ```

2. **Use classes CSS em vez de estilos inline**
   ```xml
   <!-- ✅ CORRETO -->
   <Button styleClass="button-primary" text="Salvar" />
   ```

3. **Para botões específicos, use as classes predefinidas:**
   - `.button-primary` - Ação principal (Azul)
   - `.button-success` - Salvar/Confirmar (Verde)
   - `.button-danger` - Excluir/Cancelar (Vermelho)
   - `.button-cancel` - Fechar/Sair (Cinza)

4. **Deixe o CSS fazer o trabalho**
   - O dark.css já sobrescreve automaticamente:
     - Todos os TextField, TextArea, ComboBox
     - Todos os Labels e Texto
     - Todos os Painéis (VBox, HBox, etc)
     - Tabelas, Listas, TreeViews
     - Menus e ContextMenus

### ❌ O QUE EVITAR

1. **Nunca use cores inline para fundos brancos**
   ```xml
   <!-- ❌ NÃO FAÇA ISSO -->
   <VBox style="-fx-background-color: white;">
   ```

2. **Nunca force texto preto**
   ```xml
   <!-- ❌ NÃO FAÇA ISSO -->
   <Label style="-fx-text-fill: black;" />
   ```

3. **Evite estilos inline em geral**
   - O CSS cuida de tudo automaticamente
   - Estilos inline podem interferir com a troca de tema

## 🔄 Como o Sistema Funciona

### Troca de Tema
O sistema carrega automaticamente o CSS correto baseado na preferência do usuário:
- **Modo Claro**: `resources/css/main.css`
- **Modo Escuro**: `resources/css/dark.css`

### Hierarquia de Aplicação
1. CSS base (main.css ou dark.css)
2. Classes específicas do componente
3. Regras de sobrescrita com `!important`

### Componentes Cobertos Automaticamente
- ✅ TextField, TextArea, PasswordField
- ✅ ComboBox (incluindo dropdown)
- ✅ DatePicker, Spinner, ChoiceBox
- ✅ TableView (linhas zebradas automáticas)
- ✅ ListView, TreeView
- ✅ Button (todos os tipos)
- ✅ Label, Text
- ✅ MenuBar, MenuItem, ContextMenu
- ✅ RadioButton, CheckBox
- ✅ ScrollPane, ScrollBar
- ✅ TabPane, Accordion
- ✅ ProgressBar, ProgressIndicator
- ✅ Tooltip
- ✅ Dialog

## 🎯 Exemplos de Uso Correto

### Tela de Cadastro Simples
```xml
<VBox spacing="10">
    <Label text="Nome:" styleClass="label-small-bold" />
    <TextField fx:id="txtNome" promptText="Digite o nome..." />
    
    <Label text="Email:" styleClass="label-small-bold" />
    <TextField fx:id="txtEmail" promptText="Digite o email..." />
    
    <HBox spacing="10" alignment="CENTER">
        <Button text="Salvar" styleClass="button-success" />
        <Button text="Cancelar" styleClass="button-cancel" />
    </HBox>
</VBox>
```

### Tabela com Filtros
```xml
<VBox>
    <HBox spacing="10" styleClass="filter-panel">
        <Label text="Buscar:" />
        <TextField fx:id="txtBusca" />
        <Button text="Filtrar" styleClass="button-primary" />
    </HBox>
    
    <TableView fx:id="tabela">
        <!-- Colunas automáticas com estilo zebrado -->
    </TableView>
</VBox>
```

## 🔍 Resolução de Problemas

### Campos ainda aparecem brancos no modo escuro?
1. Verifique se há estilos inline no FXML
2. Remova qualquer `-fx-background-color: white`
3. Reinicie a aplicação para recarregar o CSS

### Texto não está visível?
- O CSS força automaticamente:
  - Branco (#ffffff) no modo escuro
  - Cinza escuro (#263238) no modo claro

### Botão não mudou de cor?
- Use `styleClass` em vez de `style`
- Evite cores inline no FXML

## 📚 Classes CSS Disponíveis

### Labels
- `.label-highlight` - Título destacado
- `.label-bold` - Texto em negrito
- `.label-small-bold` - Pequeno e negrito
- `.label-subtitle` - Subtítulo secundário

### Botões
- `.button-primary` - Azul primário
- `.button-success` - Verde sucesso
- `.button-danger` - Vermelho perigo
- `.button-cancel` - Cinza cancelar
- `.button-acesso-rapido` - Botões da tela principal

### Painéis
- `.sidebar-panel` - Painel lateral
- `.footer-panel` - Rodapé
- `.filter-panel` - Painel de filtros

### Tabelas
- `.table-view` - Automático com zebrado

## 🚀 Resultado

Com essas configurações:
- ✅ Novas telas automaticamente funcionam nos dois modos
- ✅ Não é necessário ajustar manualmente após criar
- ✅ CSS robusto com cobertura completa
- ✅ Consistência visual em todo o sistema
- ✅ Manutenção facilitada

---

**Última atualização**: Dezembro 2025
**Versão CSS**: 2.0 - Navy Blue Dark Mode
