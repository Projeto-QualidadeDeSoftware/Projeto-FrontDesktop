# Prontê Conveniência — Front Desktop

Interface desktop em **JavaFX** para o sistema de conveniência. Consome a API REST do projeto (Spring Boot), por padrão em `http://localhost:8080`.

## Telas
- **Login:** CPF (com máscara) e senha de um funcionário, validados pela API (`POST /auth/login`). Só os cargos **Operador de Caixa**, **Gerente** e **Administrador** entram. Se o token vencer (8 horas), o programa volta para o login.
- **Frente de Caixa:** busca de produtos, carrinho, controle de estoque e finalização da venda (atalho F2). O cliente é opcional: por padrão a venda é registrada como **Consumidor Final**, para um fluxo rápido.
- **Gestão de Produtos:** listagem, busca, cadastro, edição e exclusão.
- **Clientes:** cadastro, edição, exclusão e listagem com busca.
- **Gerenciar Funcionários:** cadastro, edição, exclusão e listagem com busca. Ao editar, a senha precisa ser informada novamente, porque a API não devolve a senha.
- **Vendas:** fechamento por mês: total vendido, número de vendas, ticket médio, itens vendidos, lista das vendas do mês e quantidade vendida por produto.
- **Assistente de Estoque:** perguntas em texto livre sobre o estoque (ex.: "o que está acabando?"), respondidas pela IA da API (`POST /assistente/pergunta`).

## Instalação (Windows)
Baixe o instalador `Pronte Conveniencia-1.1.0.exe` na página de [Releases](../../releases) e execute. O Java já vem embutido; o programa é instalado para o usuário atual e cria atalhos no Menu Iniciar e na Área de Trabalho. Para remover, use "Aplicativos instalados" do Windows.

O endereço da API pode ser alterado no arquivo `api.properties` (pasta `app` do programa instalado), pela variável de ambiente `PRONTE_API_URL` ou por `-Dapi.url`. Sem nada configurado, usa `http://localhost:8080`.

## Como usar
1. Suba o MySQL e a API (veja a seção *Banco de dados*).
2. Abra o programa e entre com CPF e senha (Operador de Caixa, Gerente ou Administrador).
3. **Frente de Caixa:** clique nos produtos para adicionar ao carrinho, ajuste as quantidades, confira o vendedor e finalize com **F2**. O cliente é opcional.
4. **Produtos, Clientes e Funcionários:** use o formulário para cadastrar; na lista é possível buscar, editar e excluir.
5. **Vendas:** escolha o mês no seletor para ver o total vendido e a lista de vendas (use *Atualizar* para recarregar).
6. **Assistente de Estoque:** digite a pergunta e pressione Enter. A API precisa estar configurada com a chave da Mistral (`MISTRAL_API_KEY`); sem ela, a tela mostra que o assistente não está configurado.

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

Usuários de teste (senha `1234`): CPF `111.111.111-11` (Operador de Caixa) e `222.222.222-22` (Gerente). Ao iniciar, a API converte essas senhas para o formato protegido; o login continua com `1234`.
Com o banco sem nenhum funcionário, a API cria o usuário inicial CPF `000.000.000-00`, senha `admin123`, cargo Administrador.
O script também cria o cliente **Consumidor Final**, usado nas vendas sem cliente identificado (o front o cria sozinho se não existir).
A API (`application.properties`) usa o banco `projeto` em `localhost:3306`.

## Tecnologias
Java 17 · JavaFX 17 · Maven · Jackson · HTTP REST

## Observações
- Todas as requisições, exceto o login, enviam o token no cabeçalho `Authorization: Bearer <token>`.
- A venda envia cada produto com sua quantidade; a API calcula o total, dá baixa no estoque e recusa a venda se faltar estoque.
- A API exige um cliente em toda venda, por isso o "Consumidor Final".
- Para desligar o login durante o desenvolvimento, use `LOGIN_ATIVO` em `MainApp.java` (a API continuará exigindo o token).
