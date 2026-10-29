# Prontê Conveniência — Front Desktop

Interface desktop em **JavaFX** para o sistema de conveniência. Consome a API REST do projeto (Spring Boot), por padrão em `http://localhost:8080`.

## Telas
- **Login:** CPF (com máscara) e senha de um funcionário, validados pela API (`POST /auth/login`). Só os cargos **Operador de Caixa**, **Gerente** e **Administrador** entram. Se o token vencer (8 horas), o programa volta para o login.
- **Frente de Caixa:** busca de produtos, carrinho, controle de estoque e finalização da venda (atalho F2). É preciso escolher o cliente, o funcionário responsável e a data da venda (hoje por padrão), como na versão web.
- **Produtos:** listagem, busca, cadastro, edição e exclusão.
- **Clientes:** cadastro, edição, exclusão e listagem com busca.
- **Funcionários:** cadastro, edição, exclusão e listagem com busca. Ao editar, a senha precisa ser informada novamente, porque a API não devolve a senha.
- **Vendas:** fechamento por mês: total vendido, número de vendas, ticket médio, itens vendidos, lista das vendas do mês e quantidade vendida por produto.
- **Assistente IA:** perguntas em texto livre sobre o estoque (ex.: "o que está acabando?"), respondidas pela IA da API (`POST /assistente/pergunta`).
- **Auditoria** (só Administrador): quem fez cada operação, quando, de onde e com qual resultado, com busca, filtro por ação e totais (`GET /auditoria`).

## Instalação (Windows)
Baixe o instalador `Pronte Conveniencia-<versão>.exe` da versão mais recente (*Latest*) na página de [Releases](../../releases) e execute. O Java já vem embutido; o programa é instalado para o usuário atual e cria atalhos no Menu Iniciar e na Área de Trabalho. Para remover, use "Aplicativos instalados" do Windows.

O instalador oficial já vem configurado com a API de produção (`https://api.20-151-6-104.sslip.io`). Os instaladores marcados como *Pre-release* (`Pronte Conveniencia Homologacao`) apontam para a API de homologação e servem para os testes de aceitação; os dois podem ficar instalados ao mesmo tempo.

O endereço da API fica no arquivo `api.properties` (pasta `app` do programa instalado) e também pode ser trocado pela variável de ambiente `PRONTE_API_URL` ou por `-Dapi.url`. Sem nada configurado, usa `http://localhost:8080`.

## Como usar
1. Suba o MySQL e a API (veja a seção *Banco de dados*).
2. Abra o programa e entre com CPF e senha (Operador de Caixa, Gerente ou Administrador).
3. **Frente de Caixa:** clique nos produtos para adicionar ao carrinho, ajuste as quantidades, confira o vendedor e finalize com **F2**. Escolha o cliente.
4. **Produtos, Clientes e Funcionários:** use o formulário para cadastrar; na lista é possível buscar, editar e excluir.
5. **Vendas:** escolha o mês no seletor para ver o total vendido e a lista de vendas (use *Atualizar* para recarregar).
6. **Assistente de Estoque:** digite a pergunta e pressione Enter. A API precisa estar configurada com a chave da Mistral (`MISTRAL_API_KEY`); sem ela, a tela mostra que o assistente não está configurado.

## Como executar (desenvolvimento)
Pré-requisitos: Java 17 e a API rodando em `localhost:8080` (com o MySQL ligado).

```
mvnw.cmd javafx:run
```
ou execute o `run.bat`.

## CI/CD
- **CI** (`.github/workflows/ci.yml`): em todo push e pull request para a `main`, compila e empacota o programa (`mvnw package`). Se não compilar, o PR não pode ser mesclado.
- **CD** (`.github/workflows/cd.yml`): a cada alteração na `main`, gera no Windows o instalador de homologação (apontando para a API de homologação) e publica como *pre-release*; depois da aprovação de um integrante no environment `producao`, gera o instalador de produção e publica como a versão oficial (*latest*). O endereço da API de cada ambiente vem da variable `API_URL` do environment no GitHub, e a versão é `1.3.<número da execução>`.

## Banco de dados
O arquivo [`database/projeto.sql`](database/projeto.sql) recria o banco `projeto` do MySQL com a estrutura e dados fictícios (produtos, clientes e funcionários). Para importar:

```
mysql -u root -p < database/projeto.sql
```

Usuários de teste (senha `1234`): CPF `111.111.111-11` (Operador de Caixa) e `222.222.222-22` (Gerente). Ao iniciar, a API converte essas senhas para o formato protegido; o login continua com `1234`.
Com o banco sem nenhum funcionário, a API cria o usuário inicial CPF `000.000.000-00`, senha `admin123`, cargo Administrador.
A API (`application.properties`) usa o banco `projeto` em `localhost:3306`.

## Tecnologias
Java 17 · JavaFX 17 · Maven · Jackson · HTTP REST

## Observações
- Todas as requisições, exceto o login, enviam o token no cabeçalho `Authorization: Bearer <token>`.
- A venda envia cada produto com sua quantidade; a API calcula o total, dá baixa no estoque e recusa a venda se faltar estoque.
- A API exige um cliente em toda venda; cadastre ao menos um cliente antes de vender.
- Para desligar o login durante o desenvolvimento, use `LOGIN_ATIVO` em `MainApp.java` (a API continuará exigindo o token).
