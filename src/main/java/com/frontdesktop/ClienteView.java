package com.frontdesktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.frontdesktop.api.ApiClient;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Clientes: formulário de cadastro/edição no topo e lista com busca, edição e exclusão embaixo. */
public class ClienteView {

    private static final String ENDPOINT = "/cliente";

    private final ApiClient api;
    private final VBox root = new VBox(18);

    private final TextField nome = Ui.entrada("Ex: Maria Fernandes da Silva");
    private final TextField cpf = Ui.mascaraCpf(Ui.entrada("000.000.000-00"));
    private final TextField telefone = Ui.entrada("(00) 00000-0000");
    private final TextField email = Ui.entrada("cliente@email.com");
    private final Label formTitulo = new Label("Cadastrar Novo Cliente");
    private final Button salvar = Ui.botao("Salvar Cliente", "primary-button");
    private final Label status = new Label();

    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> filtrados = new FilteredList<>(dados, p -> true);
    private final TableView<JsonNode> tabela = new TableView<>(filtrados);
    private final TextField busca = Ui.entrada("Buscar cliente...");

    private Long idEmEdicao;

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
            if (status.getText().isEmpty() || status.getText().startsWith("Erro")) {
                Ui.mensagem(status, "", false);
            }
        }, erro -> Ui.mensagem(status, "Erro ao carregar clientes: " + erro.getMessage(), true));
    }

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");
        root.getChildren().addAll(Ui.cabecalho("Clientes"), montarFormulario(), montarLista());
    }

    private VBox montarFormulario() {
        HBox titulo = new HBox(12, Ui.iconeCartao("👤"), formTitulo);
        titulo.setAlignment(Pos.CENTER_LEFT);
        formTitulo.getStyleClass().add("section-title");

        GridPane grade = new GridPane();
        grade.setHgap(18);
        grade.setVgap(14);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints coluna = new ColumnConstraints();
            coluna.setPercentWidth(100.0 / 3);
            grade.getColumnConstraints().add(coluna);
        }
        grade.add(Ui.campo("Nome completo *", nome), 0, 0, 2, 1);
        grade.add(Ui.campo("CPF *", cpf), 2, 0);
        grade.add(Ui.campo("E-mail *", email), 0, 1, 2, 1);
        grade.add(Ui.campo("Telefone *", telefone), 2, 1);

        Button cancelar = Ui.botao("Cancelar", "outline-button");
        cancelar.setPadding(new Insets(10, 22, 10, 22));
        cancelar.setOnAction(event -> {
            limparFormulario();
            Ui.mensagem(status, "", false);
        });
        salvar.setOnAction(event -> salvar());

        status.setWrapText(true);
        status.getStyleClass().add("status-label");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox rodape = new HBox(12, status, espaco, cancelar, salvar);
        rodape.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(16, titulo, grade, rodape);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22, 26, 22, 26));
        return card;
    }

    private VBox montarLista() {
        HBox titulo = new HBox(12, Ui.iconeCartao("👥"), new Label("Clientes Cadastrados"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        titulo.getChildren().get(1).getStyleClass().add("section-title");

        busca.getStyleClass().add("search-field");
        busca.setPrefWidth(300);
        busca.textProperty().addListener((obs, antigo, texto) -> {
            String filtro = texto.trim().toLowerCase();
            filtrados.setPredicate(c -> filtro.isEmpty()
                    || c.path("nome").asText("").toLowerCase().contains(filtro)
                    || c.path("email").asText("").toLowerCase().contains(filtro)
                    || c.path("cpf").asText("").contains(filtro));
        });

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox cabecalho = new HBox(12, titulo, espaco, busca);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum cliente encontrado."));
        tabela.setSelectionModel(null);
        tabela.getColumns().add(coluna("ID", c -> String.format("%03d", c.path("id").asLong()), 0.7, "cell-id"));
        tabela.getColumns().add(coluna("NOME", c -> c.path("nome").asText(""), 2.4, "cell-strong"));
        tabela.getColumns().add(coluna("CPF", c -> c.path("cpf").asText(""), 1.6, "cell-muted"));
        tabela.getColumns().add(coluna("TELEFONE", c -> c.path("telefone").asText(""), 1.5, "cell-muted"));
        tabela.getColumns().add(coluna("E-MAIL", c -> c.path("email").asText(""), 2.4, "cell-muted"));
        // todas as colunas de texto são alinhadas à esquerda (título e conteúdo)
        tabela.getColumns().add(colunaAcoes());
        VBox.setVgrow(tabela, Priority.ALWAYS);

        VBox card = new VBox(14, cabecalho, tabela);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18, 20, 20, 20));
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private TableColumn<JsonNode, String> coluna(String titulo, Function<JsonNode, String> valor, double peso,
                                                 String estilo) {
        TableColumn<JsonNode, String> coluna = new TableColumn<>();
        Ui.cabecalhoColuna(coluna, titulo, Ui.ESQUERDA);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        coluna.setPrefWidth(peso * 100);
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                Ui.alinharCelula(this, Ui.ESQUERDA);
                setText(vazio ? null : item);
                getStyleClass().removeAll("cell-id", "cell-strong", "cell-muted");
                if (!vazio) {
                    getStyleClass().add(estilo);
                }
            }
        });
        return coluna;
    }

    private TableColumn<JsonNode, JsonNode> colunaAcoes() {
        TableColumn<JsonNode, JsonNode> coluna = new TableColumn<>();
        Ui.cabecalhoColuna(coluna, "AÇÕES", Ui.CENTRO);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(JsonNode cliente, boolean vazio) {
                super.updateItem(cliente, vazio);
                Ui.alinharCelula(this, Ui.CENTRO);
                if (vazio || cliente == null) {
                    setGraphic(null);
                    return;
                }
                HBox acoes = new HBox(4,
                        Ui.botaoIcone("✎", false, () -> editar(cliente)),
                        Ui.botaoIcone("🗑", true, () -> excluir(cliente)));
                acoes.setAlignment(Pos.CENTER);
                setGraphic(acoes);
                setAlignment(Pos.CENTER);
            }
        });
        return coluna;
    }

    private void limparFormulario() {
        idEmEdicao = null;
        nome.clear();
        cpf.clear();
        telefone.clear();
        email.clear();
        formTitulo.setText("Cadastrar Novo Cliente");
    }

    private void editar(JsonNode cliente) {
        idEmEdicao = cliente.path("id").asLong();
        nome.setText(cliente.path("nome").asText(""));
        cpf.setText(cliente.path("cpf").asText(""));
        telefone.setText(cliente.path("telefone").asText(""));
        email.setText(cliente.path("email").asText(""));
        formTitulo.setText("Editar Cliente #" + idEmEdicao);
        Ui.mensagem(status, "", false);
    }

    private void salvar() {
        if (nome.getText().isBlank() || cpf.getText().isBlank()
                || telefone.getText().isBlank() || email.getText().isBlank()) {
            Ui.mensagem(status, "Preencha todos os campos obrigatórios.", true);
            return;
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", nome.getText().trim());
        corpo.put("cpf", cpf.getText().trim());
        corpo.put("email", email.getText().trim());
        corpo.put("telefone", telefone.getText().trim());

        Long id = idEmEdicao;
        boolean edicao = id != null;
        salvar.setDisable(true);
        Ui.mensagem(status, "Salvando...", false);
        Async.run("cliente-salvar", () -> {
            api.salvar(ENDPOINT, id, corpo);
            return Boolean.TRUE;
        }, ok -> {
            salvar.setDisable(false);
            limparFormulario();
            recarregar();
            Ui.mensagem(status, edicao ? "Cliente atualizado com sucesso." : "Cliente cadastrado com sucesso.", false);
        }, erro -> {
            salvar.setDisable(false);
            Ui.mensagem(status, erro.getMessage(), true);
        });
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
            if (Objects.equals(idEmEdicao, id)) {
                limparFormulario();
            }
            recarregar();
            Ui.mensagem(status, "Cliente excluído.", false);
        }, erro -> Ui.mensagem(status, erro.getMessage(), true));
    }
}
