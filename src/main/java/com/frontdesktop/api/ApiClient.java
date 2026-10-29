package com.frontdesktop.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ApiClient {
    private static final String BASE_URL = resolverUrlBase();

    /**
     * Endereço da API, na ordem: variável de ambiente PRONTE_API_URL, propriedade -Dapi.url,
     * arquivo api.properties (chave api.url) na pasta atual ou ao lado do programa, e por fim localhost.
     */
    private static String resolverUrlBase() {
        String url = System.getenv("PRONTE_API_URL");
        if (url == null || url.isBlank()) {
            url = System.getProperty("api.url");
        }
        if (url == null || url.isBlank()) {
            url = lerDeArquivo();
        }
        if (url == null || url.isBlank()) {
            return "http://localhost:8080";
        }
        url = url.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String lerDeArquivo() {
        List<java.nio.file.Path> candidatos = new ArrayList<>();
        candidatos.add(java.nio.file.Path.of(System.getProperty("user.dir"), "api.properties"));
        try {
            java.nio.file.Path codigo = java.nio.file.Path.of(
                    ApiClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.nio.file.Path base = codigo.getParent();
            if (base != null) {
                candidatos.add(base.resolve("api.properties"));
                if (base.getParent() != null) {
                    candidatos.add(base.getParent().resolve("api.properties"));
                }
            }
        } catch (Exception ignorado) {
            // sem acesso ao local do jar: segue só com a pasta atual
        }
        for (java.nio.file.Path arquivo : candidatos) {
            if (java.nio.file.Files.isRegularFile(arquivo)) {
                try (java.io.InputStream in = java.nio.file.Files.newInputStream(arquivo)) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(in);
                    String valor = props.getProperty("api.url");
                    if (valor != null && !valor.isBlank()) {
                        return valor;
                    }
                } catch (IOException ignorado) {
                    // tenta o próximo candidato
                }
            }
        }
        return null;
    }
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Token recebido no login; vai no cabeçalho Authorization de todas as outras requisições. */
    private volatile String token;
    private volatile Runnable aoExpirarSessao = () -> { };

    /** Chamado quando a API responde 401 (token vencido ou inválido), para voltar à tela de login. */
    public void aoExpirarSessao(Runnable acao) {
        this.aoExpirarSessao = acao == null ? () -> { } : acao;
    }

    public void sair() {
        token = null;
    }

    /**
     * Faz login na API. Tenta o CPF com máscara e, se não encontrar, só com números,
     * porque os cadastros podem ter sido gravados nos dois formatos.
     *
     * @return o funcionário logado, ou null se CPF ou senha estiverem errados
     */
    public Usuario login(String cpfDigitos, String senha) throws IOException, InterruptedException {
        String formatado = cpfDigitos.length() == 11
                ? cpfDigitos.substring(0, 3) + "." + cpfDigitos.substring(3, 6) + "."
                        + cpfDigitos.substring(6, 9) + "-" + cpfDigitos.substring(9)
                : cpfDigitos;

        for (String cpf : List.of(formatado, cpfDigitos)) {
            java.util.Map<String, Object> corpo = new java.util.LinkedHashMap<>();
            corpo.put("cpf", cpf);
            corpo.put("senha", senha);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/login"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                if (cpf.equals(cpfDigitos)) {
                    return null;
                }
                continue;
            }
            if (response.statusCode() >= 400) {
                throw new IOException("Erro ao entrar (HTTP " + response.statusCode() + "): "
                        + mensagemDeErro(response.body()));
            }

            JsonNode resposta = mapper.readTree(response.body());
            token = resposta.path("token").asText(null);
            JsonNode funcionario = resposta.path("funcionario");
            return new Usuario(funcionario.path("id").asLong(), funcionario.path("nome").asText(""),
                    funcionario.path("cargo").asText(""));
        }
        return null;
    }

    /** Pergunta ao assistente de IA sobre o estoque e devolve a resposta em texto. */
    public String perguntarAoAssistente(String pergunta) throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(java.util.Map.of("pergunta", pergunta));
        HttpRequest request = autenticado(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/assistente/pergunta"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)))
                .build();
        HttpResponse<String> response = enviar(request, "consultar o assistente");
        return mapper.readTree(response.body()).path("resposta").asText("");
    }

    private HttpRequest.Builder autenticado(HttpRequest.Builder builder) {
        String atual = token;
        if (atual != null) {
            builder.header("Authorization", "Bearer " + atual);
        }
        return builder;
    }

    public List<Produto> listarProdutos() throws IOException, InterruptedException {
        List<Produto> result = new ArrayList<>();
        for (JsonNode node : getArray("/produto", "produtos")) {
            if (!node.hasNonNull("nome") || !node.hasNonNull("preco")) {
                continue;
            }
            result.add(new Produto(
                    node.path("id").asLong(),
                    node.get("nome").asText(),
                    node.path("descricao").asText(""),
                    node.get("preco").asDouble(),
                    node.path("estoque").asInt(0)));
        }
        return result;
    }

    public List<Pessoa> listarClientes() throws IOException, InterruptedException {
        return listarPessoas("/cliente", "clientes");
    }

    public List<Pessoa> listarFuncionarios() throws IOException, InterruptedException {
        return listarPessoas("/funcionario", "funcionários");
    }

    /**
     * Registra a venda. A API calcula o total e dá baixa no estoque; se faltar estoque
     * de algum item, a venda inteira é recusada.
     *
     * @param itens id do produto → quantidade
     */
    public String finalizarVenda(long clienteId, long funcionarioId, java.util.Map<Long, Integer> itens,
                                 String dataVenda) throws IOException, InterruptedException {
        java.util.Map<String, Object> corpo = new java.util.LinkedHashMap<>();
        corpo.put("dataVenda", dataVenda);
        corpo.put("clienteId", clienteId);
        corpo.put("funcionarioId", funcionarioId);
        corpo.put("itens", itens.entrySet().stream()
                .map(e -> java.util.Map.of("produtoId", e.getKey(), "quantidade", e.getValue()))
                .toList());
        String body = mapper.writeValueAsString(corpo);

        HttpRequest request = autenticado(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/venda"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)))
                .build();

        return enviar(request, "finalizar a venda").body();
    }

    /** Lista os registros de um recurso (ex.: "/cliente") como nós JSON. */
    public List<JsonNode> listar(String path) throws IOException, InterruptedException {
        List<JsonNode> result = new ArrayList<>();
        getArray(path, path.substring(1)).forEach(result::add);
        return result;
    }

    /** POST quando id é nulo, PUT caso contrário. */
    public void salvar(String path, Long id, java.util.Map<String, Object> campos)
            throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(campos);
        HttpRequest.Builder builder = autenticado(HttpRequest.newBuilder())
                .uri(URI.create(BASE_URL + path + (id == null ? "" : "/" + id)))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        HttpRequest request = (id == null
                ? builder.POST(HttpRequest.BodyPublishers.ofString(body))
                : builder.PUT(HttpRequest.BodyPublishers.ofString(body))).build();
        enviar(request, "salvar");
    }

    public void excluir(String path, long id) throws IOException, InterruptedException {
        HttpRequest request = autenticado(HttpRequest.newBuilder())
                .uri(URI.create(BASE_URL + path + "/" + id))
                .timeout(Duration.ofSeconds(15))
                .DELETE()
                .build();
        enviar(request, "excluir");
    }

    private HttpResponse<String> enviar(HttpRequest request, String acao) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        verificarSessao(response);
        if (response.statusCode() >= 400) {
            throw new IOException("Não foi possível " + acao + ": " + mensagemDeErro(response.body()));
        }
        return response;
    }

    /** 401 fora do login = token vencido ou inválido: avisa a tela para voltar ao login. */
    private void verificarSessao(HttpResponse<String> response) throws IOException {
        if (response.statusCode() == 401) {
            token = null;
            aoExpirarSessao.run();
            throw new IOException("Sua sessão expirou. Entre novamente.");
        }
    }

    private String mensagemDeErro(String body) {
        try {
            JsonNode node = mapper.readTree(body);
            if (node != null && node.hasNonNull("mensagem")) {
                return node.get("mensagem").asText();
            }
            if (node != null && node.hasNonNull("message")) {
                return node.get("message").asText();
            }
        } catch (IOException ignorado) {
            // corpo não é JSON; devolve como veio
        }
        return body == null || body.isBlank() ? "sem detalhes" : body;
    }

    private List<Pessoa> listarPessoas(String path, String descricao) throws IOException, InterruptedException {
        List<Pessoa> result = new ArrayList<>();
        for (JsonNode node : getArray(path, descricao)) {
            if (node.hasNonNull("id") && node.hasNonNull("nome")) {
                result.add(new Pessoa(node.get("id").asLong(), node.get("nome").asText()));
            }
        }
        return result;
    }

    private JsonNode getArray(String path, String descricao) throws IOException, InterruptedException {
        HttpRequest request = autenticado(HttpRequest.newBuilder())
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        verificarSessao(response);
        if (response.statusCode() >= 400) {
            throw new IOException("Erro ao buscar " + descricao + " (HTTP " + response.statusCode() + ")");
        }
        JsonNode root = mapper.readTree(response.body());
        if (root == null || !root.isArray()) {
            throw new IOException("Resposta inesperada ao buscar " + descricao);
        }
        return root;
    }

    public record Produto(Long id, String nome, String descricao, double preco, int estoque) {
    }

    /** Funcionário autenticado na sessão atual. */
    public record Usuario(Long id, String nome, String cargo) {
    }

    public record Pessoa(Long id, String nome) {
        @Override
        public String toString() {
            return nome;
        }
    }
}
