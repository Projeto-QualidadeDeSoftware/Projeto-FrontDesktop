package com.frontdesktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.frontdesktop.api.ApiClient;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

/** Vendas: resumo e lista das vendas registradas, por mês, para o fechamento. */
public class VendasView {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR);

    private final ApiClient api;
    private final VBox root = new VBox(18);

    private final ComboBox<YearMonth> mesBox = new ComboBox<>();
    private final Label status = new Label();
    private final Label totalValor = new Label("R$ 0,00");
    private final Label totalVendas = new Label("0");
    private final Label ticketMedio = new Label("R$ 0,00");
    private final Label totalItens = new Label("0");

    private final ObservableList<Venda> vendasDoMes = FXCollections.observableArrayList();
    private final ObservableList<ProdutoVendido> produtosDoMes = FXCollections.observableArrayList();
    private final TableView<Venda> tabelaVendas = new TableView<>(vendasDoMes);
    private final TableView<ProdutoVendido> tabelaProdutos = new TableView<>(produtosDoMes);

    private List<Venda> todas = new ArrayList<>();
    private boolean atualizandoMeses;

    public VendasView(ApiClient api) {
        this.api = api;
        montar();
    }

    public Node getView() {
        return root;
    }

    public void recarregar() {
        Ui.mensagem(status, "Carregando vendas...", false);
        Async.run("vendas-listar", () -> api.listar("/venda"), registros -> {
            todas = registros.stream().map(Venda::de).filter(v -> v.data() != null).toList();
            Ui.mensagem(status, "", false);
            atualizarMeses();
        }, erro -> Ui.mensagem(status, "Erro ao carregar vendas: " + erro.getMessage(), true));
    }

    // ------------------------------------------------------------------ dados

    private void atualizarMeses() {
        YearMonth selecionado = mesBox.getValue();
        TreeSet<YearMonth> meses = new TreeSet<>(Comparator.reverseOrder());
        meses.add(YearMonth.now());
        todas.forEach(v -> meses.add(YearMonth.from(v.data())));

        atualizandoMeses = true;
        mesBox.getItems().setAll(meses);
        mesBox.setValue(selecionado != null && meses.contains(selecionado) ? selecionado : YearMonth.now());
        atualizandoMeses = false;
        atualizarResumo();
    }

    private void atualizarResumo() {
        YearMonth mes = mesBox.getValue();
        if (mes == null) {
            return;
        }
        List<Venda> doMes = todas.stream()
                .filter(v -> YearMonth.from(v.data()).equals(mes))
                .sorted(Comparator.comparing(Venda::data).thenComparing(Venda::id).reversed())
                .toList();

        double total = doMes.stream().mapToDouble(Venda::total).sum();
        int itens = doMes.stream().mapToInt(Venda::itens).sum();
        totalValor.setText("R$ " + Ui.dinheiro(total));
        totalVendas.setText(String.valueOf(doMes.size()));
        ticketMedio.setText("R$ " + Ui.dinheiro(doMes.isEmpty() ? 0 : total / doMes.size()));
        totalItens.setText(String.valueOf(itens));

        Map<String, ProdutoVendido> porProduto = new LinkedHashMap<>();
        for (Venda venda : doMes) {
            for (ProdutoVendido item : venda.produtos()) {
                porProduto.merge(item.nome(), item,
                        (a, b) -> new ProdutoVendido(a.nome(), a.quantidade() + b.quantidade(),
                                a.total() + b.total()));
            }
        }
        List<ProdutoVendido> ranking = new ArrayList<>(porProduto.values());
        ranking.sort(Comparator.comparingInt(ProdutoVendido::quantidade).reversed());

        vendasDoMes.setAll(doMes);
        produtosDoMes.setAll(ranking);
    }

    // ----------------------------------------------------------------- layout

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        mesBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth mes) {
                if (mes == null) {
                    return "";
                }
                String texto = mes.format(MES);
                return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
            }

            @Override
            public YearMonth fromString(String texto) {
                return null;
            }
        });
        mesBox.setPrefWidth(240);
        mesBox.valueProperty().addListener((obs, antigo, novo) -> {
            if (!atualizandoMeses) {
                atualizarResumo();
            }
        });

        Button atualizar = Ui.botao("Atualizar", "outline-button");
        atualizar.setPadding(new Insets(10, 22, 10, 22));
        atualizar.setOnAction(event -> recarregar());

        status.setWrapText(true);
        status.getStyleClass().add("status-label");

        Label rotulo = new Label("Mês");
        rotulo.getStyleClass().add("section-title");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox filtro = new HBox(12, Ui.iconeCartao("📅"), rotulo, mesBox, atualizar, espaco, status);
        filtro.setAlignment(Pos.CENTER_LEFT);

        HBox resumo = new HBox(18,
                indicador("TOTAL VENDIDO", totalValor),
                indicador("VENDAS REALIZADAS", totalVendas),
                indicador("TICKET MÉDIO", ticketMedio),
                indicador("ITENS VENDIDOS", totalItens));

        configurarTabelaVendas();
        configurarTabelaProdutos();
        VBox cardVendas = card("Vendas do mês", tabelaVendas);
        VBox cardProdutos = card("Produtos vendidos", tabelaProdutos);
        cardVendas.setPrefWidth(620);
        cardProdutos.setPrefWidth(380);
        HBox.setHgrow(cardVendas, Priority.ALWAYS);
        HBox.setHgrow(cardProdutos, Priority.ALWAYS);
        HBox listas = new HBox(18, cardVendas, cardProdutos);
        VBox.setVgrow(listas, Priority.ALWAYS);

        VBox cardFiltro = new VBox(filtro);
        cardFiltro.getStyleClass().add("card");
        cardFiltro.setPadding(new Insets(16, 20, 16, 20));

        root.getChildren().addAll(Ui.cabecalho("Vendas"), cardFiltro, resumo, listas);
    }

    private VBox indicador(String titulo, Label valor) {
        Label rotulo = new Label(titulo);
        rotulo.getStyleClass().add("cart-item-muted");
        valor.getStyleClass().add("title");
        VBox caixa = new VBox(6, rotulo, valor);
        caixa.getStyleClass().add("card");
        caixa.setPadding(new Insets(16, 20, 16, 20));
        HBox.setHgrow(caixa, Priority.ALWAYS);
        caixa.setMaxWidth(Double.MAX_VALUE);
        return caixa;
    }

    private VBox card(String titulo, TableView<?> tabela) {
        Label label = new Label(titulo);
        label.getStyleClass().add("section-title");
        VBox.setVgrow(tabela, Priority.ALWAYS);
        VBox card = new VBox(14, label, tabela);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18, 20, 20, 20));
        return card;
    }

    private void configurarTabelaVendas() {
        tabelaVendas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaVendas.setPlaceholder(new Label("Nenhuma venda neste mês."));
        tabelaVendas.setSelectionModel(null);
        tabelaVendas.getColumns().add(coluna("Nº", v -> String.format("%03d", v.id()), 0.6, "cell-id",
                Ui.ESQUERDA));
        tabelaVendas.getColumns().add(coluna("DATA", v -> v.data().format(DATA), 1.1, "cell-muted", Ui.ESQUERDA));
        tabelaVendas.getColumns().add(coluna("CLIENTE", Venda::cliente, 1.8, "cell-strong", Ui.ESQUERDA));
        tabelaVendas.getColumns().add(coluna("VENDEDOR", Venda::vendedor, 1.5, "cell-muted", Ui.ESQUERDA));
        tabelaVendas.getColumns().add(coluna("ITENS", v -> String.valueOf(v.itens()), 0.6, "cell-muted",
                Ui.DIREITA));
        tabelaVendas.getColumns().add(coluna("TOTAL", v -> "R$ " + Ui.dinheiro(v.total()), 1.2, "cell-strong",
                Ui.DIREITA));
    }

    private void configurarTabelaProdutos() {
        tabelaProdutos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaProdutos.setPlaceholder(new Label("Nenhum produto vendido neste mês."));
        tabelaProdutos.setSelectionModel(null);
        tabelaProdutos.getColumns().add(coluna("PRODUTO", ProdutoVendido::nome, 2.2, "cell-strong", Ui.ESQUERDA));
        tabelaProdutos.getColumns().add(coluna("QTD.", p -> String.valueOf(p.quantidade()), 0.7, "cell-muted",
                Ui.DIREITA));
        tabelaProdutos.getColumns().add(coluna("TOTAL", p -> "R$ " + Ui.dinheiro(p.total()), 1.2, "cell-strong",
                Ui.DIREITA));
    }

    private <T> TableColumn<T, String> coluna(String titulo, Function<T, String> valor, double peso,
                                              String estilo, String alinhamento) {
        TableColumn<T, String> coluna = new TableColumn<>();
        Ui.cabecalhoColuna(coluna, titulo, alinhamento);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        coluna.setPrefWidth(peso * 100);
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                Ui.alinharCelula(this, alinhamento);
                setText(vazio ? null : item);
                getStyleClass().removeAll("cell-id", "cell-strong", "cell-muted");
                if (!vazio) {
                    getStyleClass().add(estilo);
                }
            }
        });
        return coluna;
    }

    // ----------------------------------------------------------------- modelo

    private record ProdutoVendido(String nome, int quantidade, double total) {
    }

    private record Venda(long id, LocalDate data, String cliente, String vendedor, double total,
                         List<ProdutoVendido> produtos) {

        int itens() {
            return produtos.stream().mapToInt(ProdutoVendido::quantidade).sum();
        }

        static Venda de(JsonNode no) {
            LocalDate data = null;
            try {
                data = LocalDate.parse(no.path("dataVenda").asText(""));
            } catch (DateTimeParseException ignorado) {
                // venda sem data válida não entra no fechamento mensal
            }
            List<ProdutoVendido> produtos = new ArrayList<>();
            for (JsonNode item : no.path("itens")) {
                produtos.add(new ProdutoVendido(item.path("produto").asText("?"),
                        item.path("quantidade").asInt(0), item.path("subtotal").asDouble(0)));
            }
            return new Venda(no.path("id").asLong(), data, no.path("cliente").path("nome").asText(""),
                    no.path("funcionario").path("nome").asText(""), no.path("valorTotal").asDouble(0), produtos);
        }
    }
}
