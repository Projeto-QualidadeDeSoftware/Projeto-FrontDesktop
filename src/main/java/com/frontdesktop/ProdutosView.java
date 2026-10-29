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

/** Produtos: cartões de totais, lista paginada com busca e cadastro/edição em janela. */
public class ProdutosView {

    private static final String ENDPOINT = "/produto";
    private static final int ESTOQUE_BAIXO = 15;
    private static final int POR_PAGINA = 6;

    private final ApiClient api;
    private final VBox root = new VBox(18);
    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> filtrados = new FilteredList<>(dados, p -> true);
    private final Paginacao<JsonNode> paginacao = new Paginacao<>(filtrados, POR_PAGINA, "produtos", false);
    private final TableView<JsonNode> tabela = new TableView<>(paginacao.visiveis());
    private final TextField busca = Ui.entrada("Buscar por nome, descrição ou código...");
    private final Label status = new Label();
    private final Label totalProdutos = new Label("0");
    private final Label itensEstoque = new Label("0");
    private final Label semEstoque = new Label("0");

    public ProdutosView(ApiClient api) {
        this.api = api;
        montar();
    }

    public Node getView() {
        return root;
    }

    public void recarregar() {
        Async.run("produtos-listar", () -> api.listar(ENDPOINT), registros -> {
            dados.setAll(registros);
            totalProdutos.setText(String.valueOf(registros.size()));
            itensEstoque.setText(String.valueOf(registros.stream().mapToInt(p -> p.path("estoque").asInt(0)).sum()));
            semEstoque.setText(String.valueOf(registros.stream().filter(p -> p.path("estoque").asInt(0) <= 0).count()));
        }, erro -> Ui.mensagem(status, "Erro ao carregar produtos: " + Ui.mensagemDe(erro), true));
    }

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        Button atualizar = Ui.botao("⟳  Atualizar", "secondary-button");
        atualizar.setOnAction(event -> recarregar());
        Button novo = Ui.botao("+  Novo produto", "primary-button");
        novo.setOnAction(event -> abrirDialogo(null));
        HBox topo = Ui.topo(Ui.cabecalho("Estoque / Gerenciamento", "Produtos",
                "Consulte e gerencie os produtos cadastrados no sistema."), atualizar, novo);

        HBox resumo = new HBox(18, Ui.indicador("Produtos cadastrados", totalProdutos, false),
                Ui.indicador("Itens em estoque", itensEstoque, false),
                Ui.indicador("Produtos sem estoque", semEstoque, false));

        busca.getStyleClass().add("search-field");
        busca.setMaxWidth(Double.MAX_VALUE);
        busca.textProperty().addListener((obs, antigo, texto) -> {
            String filtro = texto.trim().toLowerCase();
            filtrados.setPredicate(produto -> filtro.isEmpty()
                    || produto.path("nome").asText("").toLowerCase().contains(filtro)
                    || produto.path("descricao").asText("").toLowerCase().contains(filtro)
                    || Ui.codigoProduto(produto.path("id").asLong()).toLowerCase().contains(filtro)
                    || produto.path("id").asText().equals(filtro));
            paginacao.inicio();
        });

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum produto encontrado."));
        tabela.setSelectionModel(null);
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("CODIGO", p -> "#" + p.path("id").asLong(), 0.8, "cell-id", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaNo("PRODUTO", 3, Ui.ESQUERDA, p -> {
            Label nome = new Label(p.path("nome").asText(""));
            nome.getStyleClass().add("cell-strong");
            Label descricao = new Label(p.path("descricao").asText(""));
            descricao.getStyleClass().add("subtitle");
            return new VBox(2, nome, descricao);
        }));
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("PREÇO", p -> "R$ " + Ui.dinheiro(p.path("preco").asDouble()), 1.2, "cell-strong", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaNo("ESTOQUE", 1.2, Ui.ESQUERDA, p -> {
            int estoque = p.path("estoque").asInt(0);
            Label rotulo = new Label(estoque + " un.");
            rotulo.getStyleClass().add(estoque <= 0 ? "stock-none" : estoque <= ESTOQUE_BAIXO ? "stock-low" : "stock-ok");
            return rotulo;
        }));
        tabela.getColumns().add(Ui.<JsonNode>colunaNo("AÇÕES", 1.2, Ui.DIREITA, p -> {
            HBox acoes = new HBox(4,
                    Ui.botaoIcone("✎", false, () -> abrirDialogo(p)),
                    Ui.botaoIcone("🗑", true, () -> excluir(p)));
            acoes.setAlignment(Pos.CENTER_RIGHT);
            return acoes;
        }));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        status.setWrapText(true);
        status.getStyleClass().add("status-label");

        VBox card = new VBox(14, busca, tabela, paginacao.rodape(), status);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18, 20, 16, 20));
        VBox.setVgrow(card, Priority.ALWAYS);

        root.getChildren().addAll(topo, resumo, card);
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
            Ui.mensagem(status, "Produto excluído com sucesso!", false);
        }, erro -> Ui.mensagem(status, Ui.mensagemDe(erro), true));
    }

    private void abrirDialogo(JsonNode existente) {
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
        Button salvar = Ui.botao(existente == null ? "Cadastrar produto" : "Salvar alterações", "primary-button");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox botoes = new HBox(12, espaco, cancelar, salvar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        Label tituloLabel = new Label(existente == null ? "Novo produto" : "Editar produto");
        tituloLabel.getStyleClass().add("section-title");
        HBox titulo = new HBox(12, Ui.iconeCartao("📦"), tituloLabel);
        titulo.setAlignment(Pos.CENTER_LEFT);

        HBox linha = new HBox(14, Ui.campo("Preço (R$)", preco), Ui.campo("Estoque", estoque));
        linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        VBox card = new VBox(16, titulo, Ui.campo("Nome", nome), Ui.campo("Descrição", descricao), linha, erro, botoes);
        card.setPadding(new Insets(28));
        card.setPrefWidth(480);
        Stage dialogo = Ui.modal(root, tituloLabel.getText(), card);

        cancelar.setOnAction(event -> dialogo.close());
        Long id = existente == null ? null : existente.path("id").asLong();
        salvar.setOnAction(event -> {
            Map<String, Object> corpo = new LinkedHashMap<>();
            try {
                String nomeTexto = nome.getText().trim();
                String descricaoTexto = descricao.getText().trim();
                if (nomeTexto.isEmpty()) {
                    erro.setText("Informe o nome do produto.");
                    return;
                }
                if (descricaoTexto.length() < 5 || descricaoTexto.length() > 255) {
                    erro.setText("A descrição deve ter entre 5 e 255 caracteres.");
                    return;
                }
                double valor = Ui.lerDecimal(preco.getText());
                int quantidade = Integer.parseInt(estoque.getText().trim());
                if (valor <= 0 || Double.isNaN(valor) || Double.isInfinite(valor)) {
                    erro.setText("O preço deve ser maior que zero.");
                    return;
                }
                if (quantidade < 0) {
                    erro.setText("O estoque não pode ser negativo.");
                    return;
                }
                corpo.put("nome", nomeTexto);
                corpo.put("descricao", descricaoTexto);
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
                Ui.mensagem(status, existente == null ? "Produto cadastrado com sucesso!" : "Produto atualizado com sucesso!", false);
            }, falha -> {
                salvar.setDisable(false);
                erro.setText(Ui.mensagemDe(falha));
            });
        });
        dialogo.showAndWait();
    }
}
