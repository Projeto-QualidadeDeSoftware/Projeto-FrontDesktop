# Prontê Conveniência — Front Desktop

Interface desktop em **JavaFX** para o sistema de conveniência. Consome a API REST do projeto (Spring Boot) em `http://localhost:8080`.

## Telas
- **Login:** CPF (com máscara) e senha de um funcionário. Só os cargos **Operador de Caixa** e **Gerente** entram.
- **Frente de Caixa:** busca de produtos, carrinho, controle de estoque e finalização da venda (atalho F2).
- **Gestão de Produtos:** listagem, busca, cadastro, edição e exclusão.
- **Clientes:** cadastro, edição, exclusão e listagem com busca.
- **Gerenciar Funcionários:** cadastro, edição, exclusão e listagem com busca.

## Como executar
Pré-requisitos: Java 17 e a API rodando em `localhost:8080` (com o MySQL ligado).

```
mvnw.cmd javafx:run
```
ou execute o `run.bat`.

## Banco de dados
O arquivo [`database/projeto.sql`](database/projeto.sql) recria o banco `projeto` do MySQL com a estrutura e dados fictícios (produtos, clientes e funcionários). Para importar:

```
mysql -u root -p < database/projeto.sql
```

Usuário de teste para o login: CPF `111.111.111-11` e senha `1234` (Operador de Caixa).
A API (`application.properties`) usa o banco `projeto` em `localhost:3306`.

## Tecnologias
Java 17 · JavaFX 17 · Maven · Jackson · HTTP REST

## Observações
- O login valida CPF e senha consultando `GET /funcionario`, pois a API ainda não possui endpoint de autenticação. Para desligar o login, use `LOGIN_ATIVO` em `MainApp.java`.
- Em desenvolvimento.
