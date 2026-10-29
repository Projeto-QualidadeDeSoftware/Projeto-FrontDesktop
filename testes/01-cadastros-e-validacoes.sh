#!/bin/bash
B=${HML_API:-https://hml.20-151-6-104.sslip.io}
: "${HML_SENHA:?defina HML_SENHA (senha do Administrador da homologação)}"
T=$(curl -s -X POST $B/auth/login -H 'Content-Type: application/json' -d "{\"cpf\":\"123.456.789-09\",\"senha\":\"$HML_SENHA\"}" | sed 's/.*"token":"\([^"]*\)".*/\1/')
H=(-H "Authorization: Bearer $T" -H 'Content-Type: application/json')
req() { # metodo path [corpo]
  curl -s -m 20 -w '\n[HTTP %{http_code}]' -X "$1" "${H[@]}" "$B$2" ${3:+-d "$3"}; echo
}
echo "--- login errado"; curl -s -m 15 -w ' [HTTP %{http_code}]\n' -X POST $B/auth/login -H 'Content-Type: application/json' -d '{"cpf":"123.456.789-09","senha":"errada"}'
echo "--- sem token"; curl -s -m 15 -o /dev/null -w '[HTTP %{http_code}]\n' $B/produto

echo "--- PRODUTO criar valido"; req POST /produto '{"nome":"Teste Agua","descricao":"Agua mineral teste","preco":3.5,"estoque":10}'
echo "--- PRODUTO preco negativo"; req POST /produto '{"nome":"Ruim","descricao":"descricao ok","preco":-1,"estoque":10}'
echo "--- PRODUTO estoque negativo"; req POST /produto '{"nome":"Ruim","descricao":"descricao ok","preco":1,"estoque":-5}'
echo "--- PRODUTO descricao curta"; req POST /produto '{"nome":"Ruim","descricao":"ab","preco":1,"estoque":1}'
echo "--- PRODUTO lista"; req GET /produto

echo "--- CLIENTE criar"; req POST /cliente '{"nome":"Cliente Teste","cpf":"529.982.247-25","email":"teste@teste.com","telefone":"(11) 91234-5678"}'
echo "--- CLIENTE cpf duplicado"; req POST /cliente '{"nome":"Dup","cpf":"529.982.247-25","email":"dup@teste.com","telefone":"(11) 91234-5678"}'
echo "--- CLIENTE cpf invalido"; req POST /cliente '{"nome":"Inv","cpf":"111.111.111-11","email":"inv@teste.com","telefone":"(11) 91234-5678"}'
echo "--- CLIENTE email invalido"; req POST /cliente '{"nome":"Inv","cpf":"390.533.447-05","email":"naoemail","telefone":"(11) 91234-5678"}'

echo "--- FUNC criar"; req POST /funcionario '{"nome":"Func Teste","cpf":"390.533.447-05","cargo":"Gerente","senha":"abcd1234","salario":2500}'
echo "--- FUNC senha curta"; req POST /funcionario '{"nome":"Func Curta","cpf":"168.995.350-09","cargo":"Gerente","senha":"ab","salario":2500}'
echo "--- FUNC salario negativo"; req POST /funcionario '{"nome":"Func Neg","cpf":"168.995.350-09","cargo":"Gerente","senha":"abcd1234","salario":-5}'
echo "--- listas"; req GET /cliente; req GET /funcionario
