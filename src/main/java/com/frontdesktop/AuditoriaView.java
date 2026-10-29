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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Auditoria (só Administrador): quem fez cada operação, quando, de onde e com qual resultado. */
public class AuditoriaView {

    private static final int TAMANHO_PAGINA = 100;
    private static final String TODAS = "Todas as ações";

    /** Ação da API → rótulo e cor da etiqueta (mesmas da web). */
    private record Acao(String rotulo, String cor) {
    }

    private static final Map<String, Acao> ACOES = Map.of(
            "LOGIN", new Acao("Login", "tag-green"),
            "LOGIN_RECUSADO", new Acao("Login recusado", "tag-red"),
            "INCLUSAO", new Acao("Inclusão", "tag-blue"),
            "ALTERACAO", new Acao("Alteração", "tag-amber"),
            "EXCLUSAO", new Acao("Exclusão", "tag-rose"),
            "PERGUNTA_IA", new Acao("Pergunta à IA", "tag-purple"));

    private final ApiClient api;
    private final VBox root = new VBox(18);
    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> filtrados = new FilteredList<>(dados, p -> true);
    private final TableView<JsonNode> tabela = new TableView<>(filtrados);
    private final TextField busca = Ui.entrada("Buscar por funcionário, rota ou IP...");
    private final ComboBox<String> filtroAcao = new ComboBox<>();
    private final Button atualizar = Ui.botao("Atualizar", "outline-button");
    private final Button carregarMais = Ui.botao("Carregar mais", "outline-button");
    private final Label status = new Label();

    private final Label totalRegistros = new Label("0");
    private final Label totalRecusados = new Label("0");
    private final Label totalAlteracoes = new Label("0");
    private final Label totalIa = new Label("0");
    private VBox cartaoRecusados;

    private int pagina;

    public AuditoriaView(ApiClient api) {
        this.api = api;
        montar();
    }

    public Node getView() {
        return root;
    }

    public void recarregar() {
        carregar(0);
    }

    private void carregar(int alvo) {
        Ui.mensagem(status, "Carregando auditoria...", false);
        atualizar.setDisable(true);
        carregarMais.setDisable(true);
        Async.run("auditoria-listar", () -> api.listar("/auditoria?pagina=" + alvo + "&tamanho=" + TAMANHO_PAGINA),
                registros -> {
                    if (alvo == 0) {
                        dados.setAll(registros);
                    } else {
                        dados.addAll(registros);
                    }
                    pagina = alvo;
                    carregarMais.setVisible(registros.size() == TAMANHO_PAGINA);
                    carregarMais.setManaged(registros.size() == TAMANHO_PAGINA);
                    atualizar.setDisable(false);
                    carregarMais.setDisable(false);
                    Ui.mensagem(status, "", false);
                    atualizarResumo();
                }, erro -> {
                    atualizar.setDisable(false);
                    carregarMais.setDisable(false);
                    Ui.mensagem(status, "Não foi possível carregar a auditoria: " + Ui.mensagemDe(erro), true);
                });
    }

    private long contar(String... acoes) {
        List<String> lista = Arrays.asList(acoes);
        return dados.stream().filter(r -> lista.contains(r.path("acao").asText(""))).count();
    }

    private void atualizarResumo() {
        totalRegistros.setText(String.valueOf(dados.size()));
        long recusados = contar("LOGIN_RECUSADO");
        totalRecusados.setText(String.valueOf(recusados));
        totalAlteracoes.setText(String.valueOf(contar("INCLUSAO", "ALTERACAO", "EXCLUSAO")));
        totalIa.setText(String.valueOf(contar("PERGUNTA_IA")));
        cartaoRecusados.getStyleClass().remove("stat-alert");
        if (recusados > 0) {
            cartaoRecusados.getStyleClass().add("stat-alert");
        }
    }

    private void aplicarFiltro() {
        String texto = busca.getText() == null ? "" : busca.getText().trim().toLowerCase();
        String escolhida = filtroAcao.getValue();
        filtrados.setPredicate(r -> {
            if (escolhida != null && !TODAS.equals(escolhida)
                    && !escolhida.equals(rotuloDe(r.path("acao").asText("")))) {
                return false;
            }
            return texto.isEmpty()
                    || r.path("usuario").asText("").toLowerCase().contains(texto)
                    || r.path("recurso").asText("").toLowerCase().contains(texto)
                    || r.path("ip").asText("").toLowerCase().contains(texto);
        });
    }

    private static String rotuloDe(String acao) {
        Acao conhecida = ACOES.get(acao);
        return conhecida != null ? conhecida.rotulo() : acao;
    }

    // ----------------------------------------------------------------- layout

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        atualizar.setPadding(new Insets(10, 22, 10, 22));
        atualizar.setOnAction(event -> recarregar());
        Region espacoTopo = new Region();
        HBox.setHgrow(espacoTopo, Priority.ALWAYS);
        VBox cabecalho = Ui.cabecalho("Segurança", "Auditoria",
                "Quem fez cada operação, quando, de onde e com qual resultado. Consultas não são registradas.");
        HBox topo = new HBox(12, cabecalho, espacoTopo, atualizar);
        topo.setAlignment(Pos.CENTER_LEFT);

        cartaoRecusados = indicador("Logins recusados", totalRecusados);
        HBox resumo = new HBox(18,
                indicador("Registros carregados", totalRegistros),
                cartaoRecusados,
                indicador("Alterações de dados", totalAlteracoes),
                indicador("Perguntas à IA", totalIa));

        busca.getStyleClass().add("search-field");
        HBox.setHgrow(busca, Priority.ALWAYS);
        busca.setMaxWidth(Double.MAX_VALUE);
        busca.textProperty().addListener((obs, antigo, novo) -> aplicarFiltro());

        filtroAcao.getItems().add(TODAS);
        ACOES.values().stream().map(Acao::rotulo).sorted().forEach(filtroAcao.getItems()::add);
        filtroAcao.setValue(TODAS);
        filtroAcao.setPrefWidth(190);
        filtroAcao.valueProperty().addListener((obs, antigo, novo) -> aplicarFiltro());
        HBox barra = new HBox(12, busca, filtroAcao);
        barra.setAlignment(Pos.CENTER_LEFT);

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum registro encontrado."));
        tabela.setSelectionModel(null);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        tabela.getColumns().add(coluna("DATA E HORA", r -> dataHora(r.path("dataHora").asText("")), 1.5,
                "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(coluna("FUNCIONÁRIO", r -> r.path("usuario").asText("-"), 1.7,
                "cell-strong", Ui.ESQUERDA));
        tabela.getColumns().add(colunaAcao());
        tabela.getColumns().add(coluna("ROTA", r -> r.path("metodo").asText("") + " " + r.path("recurso").asText(""),
                2.8, "cell-mono", Ui.ESQUERDA));
        tabela.getColumns().add(colunaResultado());
        tabela.getColumns().add(coluna("IP", r -> r.path("ip").asText("-"), 1.3, "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(coluna("DURAÇÃO", r -> r.path("duracaoMs").asLong() + " ms", 1.0,
                "cell-muted", Ui.DIREITA));

        carregarMais.setVisible(false);
        carregarMais.setManaged(false);
        carregarMais.setPadding(new Insets(10, 22, 10, 22));
        carregarMais.setOnAction(event -> carregar(pagina + 1));
        HBox rodape = new HBox(12, status, carregarMais);
        rodape.setAlignment(Pos.CENTER);
        status.setWrapText(true);
        status.getStyleClass().add("status-label");

        VBox card = new VBox(14, barra, tabela, rodape);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18, 20, 16, 20));
        VBox.setVgrow(card, Priority.ALWAYS);

        status.setText("");
        root.getChildren().addAll(topo, resumo, card);
    }

    private VBox indicador(String titulo, Label valor) {
        Label rotulo = new Label(titulo);
        rotulo.getStyleClass().add("stat-title");
        valor.getStyleClass().add("stat-value");
        VBox caixa = new VBox(6, rotulo, valor);
        caixa.getStyleClass().add("card");
        caixa.setPadding(new Insets(18, 22, 18, 22));
        HBox.setHgrow(caixa, Priority.ALWAYS);
        caixa.setMaxWidth(Double.MAX_VALUE);
        return caixa;
    }

    /** 2026-10-29T17:29:24 → 29/10/2026 17:29:24 */
    private static String dataHora(String iso) {
        if (iso.length() < 19 || iso.indexOf('T') != 10) {
            return iso.isEmpty() ? "-" : iso;
        }
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4) + " " + iso.substring(11, 19);
    }

    private TableColumn<JsonNode, String> coluna(String titulo, Function<JsonNode, String> valor, double peso,
                                                 String estilo, String alinhamento) {
        TableColumn<JsonNode, String> coluna = new TableColumn<>();
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
                getStyleClass().removeAll("cell-id", "cell-strong", "cell-muted", "cell-mono");
                if (!vazio) {
                    getStyleClass().add(estilo);
                }
            }
        });
        return coluna;
    }

    private TableColumn<JsonNode, JsonNode> colunaAcao() {
        TableColumn<JsonNode, JsonNode> coluna = new TableColumn<>();
        Ui.cabecalhoColuna(coluna, "AÇÃO", Ui.ESQUERDA);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        coluna.setPrefWidth(150);
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(JsonNode registro, boolean vazio) {
                super.updateItem(registro, vazio);
                Ui.alinharCelula(this, Ui.ESQUERDA);
                if (vazio || registro == null) {
                    setGraphic(null);
                    return;
                }
                String codigo = registro.path("acao").asText("");
                Acao acao = ACOES.get(codigo);
                Label etiqueta = new Label(acao != null ? acao.rotulo() : codigo);
                etiqueta.getStyleClass().addAll("tag", acao != null ? acao.cor() : "tag-gray");
                setGraphic(etiqueta);
            }
        });
        return coluna;
    }

    private TableColumn<JsonNode, JsonNode> colunaResultado() {
        TableColumn<JsonNode, JsonNode> coluna = new TableColumn<>();
        Ui.cabecalhoColuna(coluna, "RESULTADO", Ui.ESQUERDA);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        coluna.setPrefWidth(120);
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(JsonNode registro, boolean vazio) {
                super.updateItem(registro, vazio);
                Ui.alinharCelula(this, Ui.ESQUERDA);
                getStyleClass().removeAll("result-ok", "result-fail");
                if (vazio || registro == null) {
                    setText(null);
                    return;
                }
                int codigo = registro.path("status").asInt();
                boolean ok = codigo >= 200 && codigo < 400;
                setText(codigo + (ok ? " sucesso" : " falha"));
                getStyleClass().add(ok ? "result-ok" : "result-fail");
            }
        });
        return coluna;
    }
}
