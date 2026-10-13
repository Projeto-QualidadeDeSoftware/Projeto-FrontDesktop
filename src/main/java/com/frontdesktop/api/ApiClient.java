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
import java.util.stream.Collectors;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

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

    public String finalizarVenda(long clienteId, long funcionarioId, List<Long> produtosIds, String dataVenda)
            throws IOException, InterruptedException {
        String ids = produtosIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = "/venda?clienteId=" + clienteId + "&funcionarioId=" + funcionarioId + "&produtosIds=" + ids;
        String body = mapper.writeValueAsString(java.util.Map.of("dataVenda", dataVenda));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Erro ao finalizar venda (HTTP " + response.statusCode() + "): " + response.body());
        }
        return response.body();
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
        HttpRequest.Builder builder = HttpRequest.newBuilder()
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path + "/" + id))
                .timeout(Duration.ofSeconds(15))
                .DELETE()
                .build();
        enviar(request, "excluir");
    }

    private void enviar(HttpRequest request, String acao) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Não foi possível " + acao + " (HTTP " + response.statusCode() + "): "
                    + mensagemDeErro(response.body()));
        }
    }

    private String mensagemDeErro(String body) {
        try {
            JsonNode node = mapper.readTree(body);
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
