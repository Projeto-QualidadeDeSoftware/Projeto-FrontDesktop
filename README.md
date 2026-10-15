# Prontê Conveniência — Front Desktop

Interface desktop em **JavaFX** para o sistema de conveniência. Consome a API REST do projeto (Spring Boot), por padrão em `http://localhost:8080`.

## Telas
- **Login:** CPF (com máscara) e senha de um funcionário. Só os cargos **Operador de Caixa** e **Gerente** entram.
- **Frente de Caixa:** busca de produtos, carrinho, controle de estoque e finalização da venda (atalho F2). O cliente é opcional: por padrão a venda é registrada como **Consumidor Final**, para um fluxo rápido.
- **Gestão de Produtos:** listagem, busca, cadastro, edição e exclusão.
- **Clientes:** cadastro, edição, exclusão e listagem com busca.
- **Gerenciar Funcionários:** cadastro, edição, exclusão e listagem com busca.

## Instalação (Windows)
Baixe o instalador `Pronte Conveniencia-1.1.0.exe` na página de [Releases](../../releases) e execute. O Java já vem embutido; o programa é instalado para o usuário atual e cria atalhos no Menu Iniciar e na Área de Trabalho. Para remover, use "Aplicativos instalados" do Windows.

O endereço da API pode ser alterado no arquivo `api.properties` (pasta `app` do programa instalado), pela variável de ambiente `PRONTE_API_URL` ou por `-Dapi.url`. Sem nada configurado, usa `http://localhost:8080`.

## Como usar
1. Suba o MySQL e a API (veja a seção *Banco de dados*).
2. Abra o programa e entre com CPF e senha (só Operador de Caixa e Gerente).
3. **Frente de Caixa:** clique nos produtos para adicionar ao carrinho, ajuste as quantidades, confira o vendedor e finalize com **F2**. O cliente é opcional.
4. **Produtos, Clientes e Funcionários:** use o formulário para cadastrar; na lista é possível buscar, editar e excluir.

## Como executar (desenvolvimento)
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

Usuários de teste (senha `1234`): CPF `111.111.111-11` (Operador de Caixa) e `222.222.222-22` (Gerente).
O script também cria o cliente **Consumidor Final**, usado nas vendas sem cliente identificado (o front o cria sozinho se não existir).
A API (`application.properties`) usa o banco `projeto` em `localhost:3306`.

## Tecnologias
Java 17 · JavaFX 17 · Maven · Jackson · HTTP REST

## Observações
- O login valida CPF e senha consultando `GET /funcionario`, pois a API ainda não possui endpoint de autenticação. Para desligar o login, use `LOGIN_ATIVO` em `MainApp.java`.
- Limitações que dependem da API:
  - não existe endpoint de login: a validação é feita no cliente e a senha trafega em `GET /funcionario`;
  - a API exige um cliente em toda venda, por isso o "Consumidor Final";
  - a venda não considera quantidades (produtos repetidos são agrupados em `findAllById`) e não desconta o estoque.
- Em desenvolvimento.
