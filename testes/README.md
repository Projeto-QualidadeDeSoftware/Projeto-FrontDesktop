# Testes de ponta a ponta (API de homologação)

Scripts em bash/curl que exercitam os mesmos endpoints que o desktop usa. **Criam dados de teste** na homologação; nunca rode contra produção.

```bash
export HML_SENHA='<senha do Administrador da homologação>'
bash testes/01-cadastros-e-validacoes.sh      # cria produto/cliente/funcionário e testa as validações
bash testes/02-vendas-edicao-e-permissoes.sh  # venda, estoque, edição, assistente, auditoria, exclusões
```

O endereço padrão é `https://hml.20-151-6-104.sslip.io` (troque com `HML_API`). Os scripts assumem a homologação vazia (ids 1 e 2).

## Resultado da última execução (29/10/2026)

| Caso | Resultado |
|---|---|
| Login errado / sem token | 401 |
| Produto com preço ≤ 0, estoque < 0, descrição curta | 400 com mensagem |
| Funcionário com senha curta / salário ≤ 0 | 400 com mensagem |
| Cliente com e-mail inválido | 400 |
| Venda correta | 200, estoque baixa de 10 para 8 |
| Venda acima do estoque / sem itens / quantidade 0 / cliente inexistente | 409 / 400 / 400 / 404 |
| Assistente (pergunta válida e vazia) | 200 / 400 |
| Auditoria como Administrador / como Supervisor | 200 / 403 |
| Excluir cliente ou produto com venda | recusado (o desktop mostra mensagem amigável) |

## Problemas conhecidos da API (back-end)
- Aceita CPF duplicado e CPF inválido em clientes (o desktop agora valida o CPF antes de enviar).
- Aceita venda com data futura.
- Devolve 404 com a mensagem técnica do banco quando a exclusão é bloqueada por vínculo.
