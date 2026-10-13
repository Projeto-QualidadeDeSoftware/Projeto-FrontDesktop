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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Gestão de Produtos: tabela com busca e diálogo de cadastro/edição. */
public class ProdutosView {

    private static final String ENDPOINT = "/produto";
    private static final int ESTOQUE_BAIXO = 15;

    private final ApiClient api;
    private final VBox root = new VBox(18);
    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> filtrados = new FilteredList<>(dados, p -> true);
    private final TableView<JsonNode> tabela = new TableView<>(filtrados);
    private final TextField busca = Ui.entrada("Buscar por ID ou nome do produto...");
    private final Label status = new Label();

    public ProdutosView(ApiClient api) {
        this.api = api;
        montar();
    }

    public Node getView() {
        return root;
    }

    public void recarregar() {
        Ui.mensagem(status, "Carregando...", false);
        Async.run("produtos-listar", () -> api.listar(ENDPOINT), registros -> {
            dados.setAll(registros);
            Ui.mensagem(status, registros.size() + " produto(s).", false);
        }, erro -> Ui.mensagem(status, "Erro ao carregar produtos: " + erro.getMessage(), true));
    }

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        busca.getStyleClass().add("search-field");
        busca.setPrefWidth(440);
        busca.textProperty().addListener((obs, antigo, texto) -> {
            String filtro = texto.trim().toLowerCase();
            filtrados.setPredicate(produto -> filtro.isEmpty()
                    || produto.path("nome").asText("").toLowerCase().contains(filtro)
                    || Ui.codigoProduto(produto.path("id").asLong()).toLowerCase().contains(filtro)
                    || produto.path("id").asText().equals(filtro));
        });

        Button novo = Ui.botao("+  Novo Produto", "primary-button");
        novo.setOnAction(event -> abrirDialogo(null));

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox barra = new HBox(busca, espaco, novo);
        barra.setAlignment(Pos.CENTER_LEFT);

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum produto encontrado."));
        tabela.setSelectionModel(null);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        tabela.getColumns().add(coluna("ID", p -> Ui.codigoProduto(p.path("id").asLong()), 0.8, "cell-id"));
        tabela.getColumns().add(coluna("NOME", p -> p.path("nome").asText(""), 2.2, "cell-strong"));
        tabela.getColumns().add(coluna("DESCRIÇÃO", p -> p.path("descricao").asText(""), 3, "cell-muted"));
        TableColumn<JsonNode, String> preco = coluna("PREÇO (R$)", p -> Ui.dinheiro(p.path("preco").asDouble()), 1.2, "cell-strong");
        tabela.getColumns().add(preco);
        tabela.getColumns().add(colunaEstoque());
        tabela.getColumns().add(colunaAcoes());

        status.getStyleClass().add("status-label");
        root.getChildren().addAll(Ui.cabecalho("Gestão de Produtos"), barra, tabela, status);
    }

    private TableColumn<JsonNode, String> coluna(String titulo, Function<JsonNode, String> valor, double peso,
                                                 String estilo) {
        TableColumn<JsonNode, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        coluna.setPrefWidth(peso * 100);
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : item);
                getStyleClass().removeAll("cell-id", "cell-strong", "cell-muted");
                if (!vazio) {
                    getStyleClass().add(estilo);
                }
            }
        });
        return coluna;
    }

    private TableColumn<JsonNode, JsonNode> colunaEstoque() {
        TableColumn<JsonNode, JsonNode> coluna = new TableColumn<>("ESTOQUE");
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(JsonNode produto, boolean vazio) {
                super.updateItem(produto, vazio);
                if (vazio || produto == null) {
                    setGraphic(null);
                    return;
                }
                int estoque = produto.path("estoque").asInt(0);
                Label badge = new Label(String.valueOf(estoque));
                badge.getStyleClass().addAll("badge", estoque <= ESTOQUE_BAIXO ? "badge-low" : "badge-ok");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });
        return coluna;
    }

    private TableColumn<JsonNode, JsonNode> colunaAcoes() {
        TableColumn<JsonNode, JsonNode> coluna = new TableColumn<>("AÇÕES");
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(JsonNode produto, boolean vazio) {
                super.updateItem(produto, vazio);
                if (vazio || produto == null) {
                    setGraphic(null);
                    return;
                }
                HBox acoes = new HBox(4,
                        Ui.botaoIcone("✎", false, () -> abrirDialogo(produto)),
                        Ui.botaoIcone("🗑", true, () -> excluir(produto)));
                acoes.setAlignment(Pos.CENTER);
                setGraphic(acoes);
                setAlignment(Pos.CENTER);
            }
        });
        return coluna;
    }

    private void excluir(JsonNode produto) {
        long id = produto.path("id").asLong();
        if (!Ui.confirmar("Excluir o produto \"" + produto.path("nome").asText() + "\"?")) {
            return;
        }
        Ui.mensagem(status, "Excluindo...", false);
        Async.run("produtos-excluir", () -> {
            api.excluir(ENDPOINT, id);
            return Boolean.TRUE;
        }, ok -> {
            recarregar();
            Ui.mensagem(status, "Produto excluído.", false);
        }, erro -> Ui.mensagem(status, erro.getMessage(), true));
    }

    private void abrirDialogo(JsonNode existente) {
        Stage dialogo = new Stage();
        dialogo.initModality(Modality.APPLICATION_MODAL);
        dialogo.initOwner(root.getScene().getWindow());
        dialogo.setTitle(existente == null ? "Novo Produto" : "Editar Produto");

        TextField nome = Ui.entrada("Ex: Coxinha de Frango");
        TextField descricao = Ui.entrada("Ex: Salgado assado, unidade 90g");
        TextField preco = Ui.entrada("0,00");
        TextField estoque = Ui.entrada("0");
        if (existente != null) {
            nome.setText(existente.path("nome").asText(""));
            descricao.setText(existente.path("descricao").asText(""));
            preco.setText(Ui.dinheiro(existente.path("preco").asDouble()));
            estoque.setText(existente.path("estoque").asText("0"));
        }

        Label erro = new Label();
        erro.setWrapText(true);
        erro.getStyleClass().addAll("status-label", "status-error");

        Button cancelar = Ui.botao("Cancelar", "secondary-button");
        cancelar.setOnAction(event -> dialogo.close());
        Button salvar = Ui.botao(existente == null ? "Salvar Produto" : "Salvar Alterações", "primary-button");

        Long id = existente == null ? null : existente.path("id").asLong();
        salvar.setOnAction(event -> {
            Map<String, Object> corpo = new LinkedHashMap<>();
            try {
                if (nome.getText().isBlank() || descricao.getText().isBlank()) {
                    erro.setText("Informe nome e descrição.");
                    return;
                }
                double valor = Ui.lerDecimal(preco.getText());
                int quantidade = Integer.parseInt(estoque.getText().trim());
                corpo.put("nome", nome.getText().trim());
                corpo.put("descricao", descricao.getText().trim());
                corpo.put("preco", valor);
                corpo.put("estoque", quantidade);
            } catch (NumberFormatException e) {
                erro.setText("Preço ou estoque inválido.");
                return;
            }

            salvar.setDisable(true);
            erro.setText("");
            Async.run("produtos-salvar", () -> {
                api.salvar(ENDPOINT, id, corpo);
                return Boolean.TRUE;
            }, ok -> {
                dialogo.close();
                recarregar();
            }, falha -> {
                salvar.setDisable(false);
                erro.setText(falha.getMessage());
            });
        });

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox botoes = new HBox(12, espaco, cancelar, salvar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        HBox titulo = new HBox(12, Ui.iconeCartao("📦"),
                new Label(existente == null ? "Cadastrar Novo Produto" : "Editar Produto"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        titulo.getChildren().get(1).getStyleClass().add("section-title");

        HBox linha = new HBox(14, Ui.campo("Preço (R$)", preco), Ui.campo("Estoque", estoque));
        HBox.setHgrow(linha.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(linha.getChildren().get(1), Priority.ALWAYS);

        VBox card = new VBox(16, titulo, Ui.campo("Nome", nome), Ui.campo("Descrição", descricao), linha, erro, botoes);
        card.getStyleClass().addAll("card", "main-panel");
        card.setPadding(new Insets(28));
        card.setPrefWidth(480);

        Scene scene = new Scene(card);
        scene.getStylesheets().addAll(root.getScene().getStylesheets());
        dialogo.setScene(scene);
        dialogo.setResizable(false);
        dialogo.showAndWait();
    }
}
