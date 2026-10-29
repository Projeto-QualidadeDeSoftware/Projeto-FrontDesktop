package com.frontdesktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.frontdesktop.api.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

/** Clientes: cartões de totais, lista paginada com busca e cadastro/edição em janela. */
public class ClienteView {

    private static final String ENDPOINT = "/cliente";
    private static final int POR_PAGINA = 6;

    private final ApiClient api;
    private final VBox root = new VBox(18);

    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> filtrados = new FilteredList<>(dados, p -> true);
    private final Paginacao<JsonNode> paginacao = new Paginacao<>(filtrados, POR_PAGINA, "clientes", true);
    private final TableView<JsonNode> tabela = new TableView<>(paginacao.visiveis());
    private final TextField busca = Ui.entrada("Buscar cliente...");
    private final Label status = new Label();
    private final Label totalClientes = new Label("0");
    private final Label resultados = new Label("0");

    public ClienteView(ApiClient api) {
        this.api = api;
        montar();
    }

    public Node getView() {
        return root;
    }

    public void recarregar() {
        Async.run("clientes-listar", () -> api.listar(ENDPOINT), registros -> {
            dados.setAll(registros);
            totalClientes.setText(String.valueOf(registros.size()));
        }, erro -> Ui.mensagem(status, "Erro ao carregar clientes: " + Ui.mensagemDe(erro), true));
    }

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        Button novo = Ui.botao("+  Novo cliente", "primary-button");
        novo.setOnAction(event -> abrirDialogo(null));
        HBox topo = Ui.topo(Ui.cabecalho("Gerenciamento", "Clientes", "Cadastre e gerencie os clientes da PRONTE."),
                novo);

        HBox resumo = new HBox(18, Ui.indicador("Total de clientes", totalClientes, false),
                Ui.indicador("Resultados encontrados", resultados, true));
        filtrados.addListener((javafx.collections.ListChangeListener<JsonNode>) mudanca ->
                resultados.setText(String.valueOf(filtrados.size())));

        busca.getStyleClass().add("search-field");
        busca.setPrefWidth(300);
        busca.textProperty().addListener((obs, antigo, texto) -> {
            String filtro = texto.trim().toLowerCase();
            filtrados.setPredicate(c -> filtro.isEmpty()
                    || c.path("nome").asText("").toLowerCase().contains(filtro)
                    || c.path("email").asText("").toLowerCase().contains(filtro)
                    || c.path("cpf").asText("").contains(filtro));
            paginacao.inicio();
        });
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox cabecalhoCard = new HBox(12,
                Ui.tituloCartao("Lista de clientes", "Consulte os dados dos clientes cadastrados."), espaco, busca);
        cabecalhoCard.setAlignment(Pos.CENTER_LEFT);

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum cliente encontrado."));
        tabela.setSelectionModel(null);
        tabela.getColumns().add(Ui.<JsonNode>colunaNo("CLIENTE", 2.4, Ui.ESQUERDA, c -> {
            Label nome = new Label(c.path("nome").asText(""));
            nome.getStyleClass().add("cell-strong");
            HBox linha = new HBox(10, Ui.avatar(c.path("nome").asText("")), nome);
            linha.setAlignment(Pos.CENTER_LEFT);
            return linha;
        }));
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("CPF", c -> c.path("cpf").asText(""), 1.6, "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("TELEFONE", c -> c.path("telefone").asText(""), 1.5, "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("E-MAIL", c -> c.path("email").asText(""), 2.4, "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaNo("AÇÕES", 1.4, Ui.DIREITA,
                c -> Ui.acoesTexto(() -> abrirDialogo(c), () -> excluir(c))));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        status.setWrapText(true);
        status.getStyleClass().add("status-label");

        VBox card = new VBox(14, cabecalhoCard, tabela, paginacao.rodape(), status);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18, 20, 16, 20));
        VBox.setVgrow(card, Priority.ALWAYS);

        root.getChildren().addAll(topo, resumo, card);
    }

    private void abrirDialogo(JsonNode existente) {
        TextField nome = Ui.entrada("Ex: Maria Fernandes da Silva");
        TextField cpf = Ui.mascaraCpf(Ui.entrada("000.000.000-00"));
        TextField telefone = Ui.entrada("(00) 00000-0000");
        TextField email = Ui.entrada("cliente@email.com");
        if (existente != null) {
            nome.setText(existente.path("nome").asText(""));
            cpf.setText(existente.path("cpf").asText(""));
            telefone.setText(existente.path("telefone").asText(""));
            email.setText(existente.path("email").asText(""));
        }

        Label erro = new Label();
        erro.setWrapText(true);
        erro.getStyleClass().addAll("status-label", "status-error");

        Button cancelar = Ui.botao("Cancelar", "secondary-button");
        Button salvar = Ui.botao(existente == null ? "Cadastrar cliente" : "Salvar alterações", "primary-button");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox botoes = new HBox(12, espaco, cancelar, salvar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        Label titulo = new Label(existente == null ? "Novo cliente" : "Editar cliente");
        titulo.getStyleClass().add("section-title");
        HBox cabecalho = new HBox(12, Ui.iconeCartao("👤"), titulo);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16, cabecalho, Ui.campo("Nome completo *", nome), Ui.campo("CPF *", cpf),
                Ui.campo("E-mail *", email), Ui.campo("Telefone *", telefone), erro, botoes);
        card.setPadding(new Insets(28));
        card.setPrefWidth(460);
        Stage janela = Ui.modal(root, titulo.getText(), card);

        cancelar.setOnAction(event -> janela.close());
        Long id = existente == null ? null : existente.path("id").asLong();
        boolean edicao = id != null;
        salvar.setOnAction(event -> {
            if (nome.getText().isBlank() || cpf.getText().isBlank()
                    || telefone.getText().isBlank() || email.getText().isBlank()) {
                erro.setText("Preencha todos os campos obrigatórios.");
                return;
            }
            if (!Ui.cpfValido(cpf.getText())) {
                erro.setText("CPF inválido.");
                return;
            }
            if (!Ui.emailValido(email.getText().trim())) {
                erro.setText("E-mail com formato inválido.");
                return;
            }
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("nome", nome.getText().trim());
            corpo.put("cpf", cpf.getText().trim());
            corpo.put("email", email.getText().trim());
            corpo.put("telefone", telefone.getText().trim());

            salvar.setDisable(true);
            erro.setText("");
            Async.run("cliente-salvar", () -> {
                api.salvar(ENDPOINT, id, corpo);
                return Boolean.TRUE;
            }, ok -> {
                janela.close();
                recarregar();
                Ui.mensagem(status, edicao ? "Cliente atualizado com sucesso!" : "Cliente cadastrado com sucesso!", false);
            }, falha -> {
                salvar.setDisable(false);
                erro.setText(Ui.mensagemDe(falha));
            });
        });
        janela.showAndWait();
    }

    private void excluir(JsonNode cliente) {
        long id = cliente.path("id").asLong();
        if (!Ui.confirmar("Excluir o cliente \"" + cliente.path("nome").asText() + "\"?")) {
            return;
        }
        Async.run("cliente-excluir", () -> {
            api.excluir(ENDPOINT, id);
            return Boolean.TRUE;
        }, ok -> {
            recarregar();
            Ui.mensagem(status, "Cliente excluído com sucesso!", false);
        }, erro -> Ui.mensagem(status, Ui.mensagemDe(erro), true));
    }
}
