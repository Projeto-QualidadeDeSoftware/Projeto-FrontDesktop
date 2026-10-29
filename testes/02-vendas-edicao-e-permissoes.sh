#!/bin/bash
B=${HML_API:-https://hml.20-151-6-104.sslip.io}
: "${HML_SENHA:?defina HML_SENHA (senha do Administrador da homologação)}"
login() { curl -s -X POST $B/auth/login -H 'Content-Type: application/json' -d "{\"cpf\":\"$1\",\"senha\":\"$2\"}" | sed 's/.*"token":"\([^"]*\)".*/\1/'; }
T=$(login 123.456.789-09 $HML_SENHA)
H=(-H "Authorization: Bearer $T" -H 'Content-Type: application/json')
req() { curl -s -m 30 -w '\n[HTTP %{http_code}]' -X "$1" "${H[@]}" "$B$2" ${3:+-d "$3"}; echo; }
HOJE=$(date +%F)
echo "--- VENDA ok (2 un)"; req POST /venda "{\"dataVenda\":\"$HOJE\",\"clienteId\":1,\"funcionarioId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":2}]}"
echo "--- estoque apos"; req GET /produto
echo "--- VENDA acima do estoque (50)"; req POST /venda "{\"dataVenda\":\"$HOJE\",\"clienteId\":1,\"funcionarioId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":50}]}"
echo "--- VENDA sem itens"; req POST /venda "{\"dataVenda\":\"$HOJE\",\"clienteId\":1,\"funcionarioId\":1,\"itens\":[]}"
echo "--- VENDA quantidade 0"; req POST /venda "{\"dataVenda\":\"$HOJE\",\"clienteId\":1,\"funcionarioId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":0}]}"
echo "--- VENDA data futura"; req POST /venda "{\"dataVenda\":\"2030-01-01\",\"clienteId\":1,\"funcionarioId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"
echo "--- VENDA cliente inexistente"; req POST /venda "{\"dataVenda\":\"$HOJE\",\"clienteId\":999,\"funcionarioId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"
echo "--- vendas"; req GET /venda
echo "--- EDITAR produto"; req PUT /produto/1 '{"nome":"Teste Agua 2","descricao":"Agua editada ok","preco":4.0,"estoque":20}'
echo "--- EDITAR cliente"; req PUT /cliente/1 '{"nome":"Cliente Editado","cpf":"529.982.247-25","email":"novo@teste.com","telefone":"(11) 99999-0000"}'
echo "--- EDITAR func sem trocar senha?"; req PUT /funcionario/2 '{"nome":"Func Editado","cpf":"390.533.447-05","cargo":"Supervisor","senha":"abcd1234","salario":3000}'
echo "--- ASSISTENTE"; req POST /assistente/pergunta '{"pergunta":"Quais produtos tem estoque baixo?"}'
echo "--- ASSISTENTE vazia"; req POST /assistente/pergunta '{"pergunta":""}'
echo "--- AUDITORIA admin"; req GET "/auditoria?pagina=0&tamanho=3" | cut -c1-700
echo "--- login do func comum"; T2=$(login 390.533.447-05 abcd1234); echo "token func: ${T2:0:15}"
H2=(-H "Authorization: Bearer $T2" -H 'Content-Type: application/json')
echo "--- AUDITORIA como Supervisor"; curl -s -m 20 -w '\n[HTTP %{http_code}]\n' "${H2[@]}" "$B/auditoria?pagina=0&tamanho=3" | cut -c1-300
echo "--- EXCLUIR cliente com venda"; req DELETE /cliente/1
echo "--- EXCLUIR produto com venda"; req DELETE /produto/1
echo "--- EXCLUIR cliente 2 e 3 (sem venda)"; req DELETE /cliente/2; req DELETE /cliente/3
echo "--- EXCLUIR func 2"; req DELETE /funcionario/2
echo "--- final"; req GET /funcionario
