# Prontê Conveniência — Front Desktop

Interface desktop em **JavaFX** para o sistema de conveniência. Consome a API REST do projeto (Spring Boot) em `http://localhost:8080`.

## Telas
- **Frente de Caixa:** busca de produtos, carrinho, controle de estoque e finalização da venda (atalho F2).
- **Gestão de Produtos:** listagem, busca, cadastro, edição e exclusão.
- **Cadastro de Cliente:** formulário de cadastro.
- **Gerenciar Funcionários:** cadastro, edição e exclusão.

## Como executar
Pré-requisitos: Java 17 e a API rodando em `localhost:8080`.

```
mvnw.cmd javafx:run
```
ou execute o `run.bat`.

## Tecnologias
Java 17 · JavaFX 17 · Maven · Jackson · HTTP REST

## Status
Em desenvolvimento. O login existe no código, mas está desativado (`LOGIN_ATIVO` em `MainApp.java`) até a API ter um endpoint de autenticação.
