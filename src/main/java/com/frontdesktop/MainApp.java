package com.frontdesktop;

import com.frontdesktop.api.ApiClient;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class MainApp extends Application {

    /** Exige login (CPF + senha) ao abrir o app; só Operador de Caixa e Gerente entram. */
    private static final boolean LOGIN_ATIVO = true;
    private static final String SEM_VENDEDOR = "Selecione o vendedor";
    private static final int COLUNAS_PRODUTOS = 3;

    private final ApiClient apiClient = new ApiClient();
    private final Map<Long, Produto> produtosMap = new LinkedHashMap<>();
    private final Map<Long, CartItem> carrinho = new LinkedHashMap<>();
    private final List<Button> navButtons = new ArrayList<>();

    private Stage stage;
    private ApiClient.Usuario usuarioLogado;
    private BorderPane root;
    private Node caixaView;

    private GridPane productsGrid;
    private VBox carrinhoBox;
    private Label totalValueLabel;
    private ComboBox<ApiClient.Pessoa> clienteBox;
    private ComboBox<ApiClient.Pessoa> funcionarioBox;
    private TextField searchField;
    private Button finalizarButton;
    private Label userLabel;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Prontê Conveniência");
        if (LOGIN_ATIVO) {
            mostrarLogin();
        } else {
            abrirSistema(null, false);
        }
        stage.show();
    }

    private Scene novaCena(Node conteudo) {
        Scene scene = new Scene((javafx.scene.Parent) conteudo, 1400, 850);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());
        return scene;
    }

    private void mostrarLogin() {
        usuarioLogado = null;
        carrinho.clear();
        produtosMap.clear();
        navButtons.clear();

        LoginView login = new LoginView(apiClient, usuario -> abrirSistema(usuario, false),
                () -> abrirSistema(null, true));
        stage.setScene(novaCena(login.getView()));
    }

    /**
     * @param usuario        funcionário autenticado (nulo apenas no primeiro acesso)
     * @param primeiroAcesso abre direto em Gerenciar Funcionários, para cadastrar o primeiro usuário
     */
    private void abrirSistema(ApiClient.Usuario usuario, boolean primeiroAcesso) {
        usuarioLogado = usuario;
        navButtons.clear();

        root = new BorderPane();
        root.setLeft(createSidebar());
        caixaView = createCaixa();
        root.setCenter(caixaView);

        Scene scene = novaCena(root);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), () -> {
            if (root.getCenter() == caixaView) {
                finalizarVenda();
            }
        });
        stage.setScene(scene);

        if (primeiroAcesso) {
            navButtons.get(3).fire();
        } else {
            carregarProdutos();
            carregarPessoas();
        }
    }

    // ------------------------------------------------------------------ infra

    private <T> void executarEmBackground(String nome, Callable<T> trabalho, Consumer<T> aoConcluir,
                                          Consumer<Throwable> aoFalhar) {
        Async.run(nome, trabalho, aoConcluir, aoFalhar);
    }

    private void mostrarErro(String cabecalho, Throwable erro) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(cabecalho);
        alert.setContentText(erro.getMessage());
        alert.show();
    }

    // --------------------------------------------------------------- sidebar

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(256);
        sidebar.setMinWidth(256);

        Label glyph = new Label("🏪");
        glyph.getStyleClass().add("brand-icon-glyph");
        StackPane icon = new StackPane(glyph);
        icon.getStyleClass().add("brand-icon");

        Label nome = new Label("Prontê");
        nome.getStyleClass().add("brand-name");
        Label sub = new Label("Conveniência");
        sub.getStyleClass().add("brand-sub");
        HBox logoBox = new HBox(12, icon, new VBox(0, nome, sub));
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(24, 20, 20, 24));

        Button caixa = createNavButton("🛒", "Frente de Caixa", true);
        Button produtos = createNavButton("📦", "Gestão de Produtos", false);
        Button clientes = createNavButton("👤", "Clientes", false);
        Button funcionarios = createNavButton("👥", "Gerenciar Funcionários", false);
        navButtons.addAll(List.of(caixa, produtos, clientes, funcionarios));

        ProdutosView produtosView = new ProdutosView(apiClient);
        ClienteView clienteView = new ClienteView(apiClient);
        FuncionariosView funcionariosView = new FuncionariosView(apiClient);

        caixa.setOnAction(event -> {
            marcarAtivo(caixa);
            root.setCenter(caixaView);
            carregarProdutos();
            carregarPessoas();
        });
        produtos.setOnAction(event -> {
            marcarAtivo(produtos);
            root.setCenter(produtosView.getView());
            produtosView.recarregar();
        });
        clientes.setOnAction(event -> {
            marcarAtivo(clientes);
            root.setCenter(clienteView.getView());
            clienteView.recarregar();
        });
        funcionarios.setOnAction(event -> {
            marcarAtivo(funcionarios);
            root.setCenter(funcionariosView.getView());
            funcionariosView.recarregar();
        });

        VBox nav = new VBox(8, caixa, produtos, clientes, funcionarios);
        nav.setPadding(new Insets(8, 16, 16, 16));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        userLabel = new Label(usuarioLogado == null ? "Operador" : usuarioLogado.nome());
        userLabel.getStyleClass().add("user-name");
        String cargo = usuarioLogado == null || usuarioLogado.cargo().isBlank()
                ? "Operador de caixa" : usuarioLogado.cargo();
        Label role = new Label(cargo);
        role.getStyleClass().add("user-role");
        VBox userBox = new VBox(2, userLabel, role);
        if (LOGIN_ATIVO) {
            Button sair = Ui.botao("Sair", "link-button");
            sair.setPadding(new Insets(4, 0, 0, 0));
            sair.setOnAction(event -> mostrarLogin());
            userBox.getChildren().add(sair);
        }
        userBox.getStyleClass().add("user-box");
        VBox userWrap = new VBox(userBox);
        userWrap.setPadding(new Insets(0, 16, 16, 16));

        sidebar.getChildren().addAll(logoBox, nav, spacer, userWrap);
        return sidebar;
    }

    private Button createNavButton(String icone, String texto, boolean ativo) {
        Button button = new Button(texto, new Label(icone));
        button.getStyleClass().add("nav-button");
        if (ativo) {
            button.getStyleClass().add("active");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private void marcarAtivo(Button ativo) {
        for (Button botao : navButtons) {
            botao.getStyleClass().remove("active");
        }
        ativo.getStyleClass().add("active");
    }

    // --------------------------------------------------------- frente de caixa

    private Node createCaixa() {
        searchField = Ui.entrada("Buscar produto por nome ou código...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, antigo, novo) -> renderizarProdutos());

        productsGrid = new GridPane();
        productsGrid.setHgap(18);
        productsGrid.setVgap(18);
        for (int i = 0; i < COLUNAS_PRODUTOS; i++) {
            javafx.scene.layout.ColumnConstraints coluna = new javafx.scene.layout.ColumnConstraints();
            coluna.setPercentWidth(100.0 / COLUNAS_PRODUTOS);
            productsGrid.getColumnConstraints().add(coluna);
        }

        ScrollPane scroll = new ScrollPane(productsGrid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPadding(new Insets(4));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox esquerda = new VBox(16, searchField, scroll);
        HBox.setHgrow(esquerda, Priority.ALWAYS);

        VBox direita = createCartPanel();

        HBox centro = new HBox(24, esquerda, direita);
        VBox.setVgrow(centro, Priority.ALWAYS);

        VBox content = new VBox(18, Ui.cabecalho("Frente de Caixa"), centro);
        content.setPadding(Ui.padrao());
        content.getStyleClass().add("main-panel");
        return content;
    }

    private VBox createCartPanel() {
        Label cartTitle = new Label("Carrinho de compras");
        cartTitle.getStyleClass().add("cart-title");

        carrinhoBox = new VBox();
        ScrollPane scroll = new ScrollPane(carrinhoBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefHeight(240);
        scroll.setMinHeight(160);

        Button limpar = Ui.botao("Limpar carrinho", "link-button");
        limpar.setOnAction(event -> {
            carrinho.clear();
            atualizarCarrinho();
        });
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox tituloLinha = new HBox(cartTitle, espaco, limpar);
        tituloLinha.setAlignment(Pos.CENTER_LEFT);

        VBox cartCard = new VBox(10, tituloLinha, linhaCabecalhoCarrinho(), scroll);
        cartCard.getStyleClass().add("card");
        cartCard.setPadding(new Insets(16));

        Label totalText = new Label("TOTAL");
        totalText.getStyleClass().add("total-text");
        totalValueLabel = new Label("R$ 0,00");
        totalValueLabel.getStyleClass().add("total-value");
        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);
        HBox totalBox = new HBox(totalText, totalSpacer, totalValueLabel);
        totalBox.setAlignment(Pos.CENTER_LEFT);
        totalBox.getStyleClass().add("total-box");

        clienteBox = new ComboBox<>();
        clienteBox.setPromptText("Selecione o cliente");
        funcionarioBox = new ComboBox<>();
        funcionarioBox.setPromptText(SEM_VENDEDOR);

        finalizarButton = new Button("FINALIZAR VENDA (F2)");
        finalizarButton.getStyleClass().add("finish-button");
        finalizarButton.setMaxWidth(Double.MAX_VALUE);
        finalizarButton.setOnAction(event -> finalizarVenda());

        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.getStyleClass().add("status-label");

        VBox dadosCard = new VBox(12, Ui.campo("Cliente", clienteBox), Ui.campo("Vendedor", funcionarioBox),
                finalizarButton, statusLabel);
        dadosCard.getStyleClass().add("card");
        dadosCard.setPadding(new Insets(16));

        VBox painel = new VBox(16, cartCard, totalBox, dadosCard);
        painel.setPrefWidth(350);
        painel.setMinWidth(350);
        painel.setMaxWidth(350);
        return painel;
    }

    private HBox linhaCabecalhoCarrinho() {
        HBox head = new HBox(6,
                celula("ITEM", 0, "cart-head", Pos.CENTER_LEFT, true),
                celula("QTD", 78, "cart-head", Pos.CENTER, false),
                celula("PREÇO", 48, "cart-head", Pos.CENTER_RIGHT, false),
                celula("SUBTOTAL", 62, "cart-head", Pos.CENTER_RIGHT, false));
        head.setAlignment(Pos.CENTER_LEFT);
        return head;
    }

    private Label celula(String texto, double largura, String estilo, Pos alinhamento, boolean cresce) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        label.setAlignment(alinhamento);
        if (cresce) {
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
        } else {
            label.setMinWidth(largura);
            label.setPrefWidth(largura);
        }
        return label;
    }

    // --------------------------------------------------------------- produtos

    private void carregarProdutos() {
        executarEmBackground("carregar-produtos", apiClient::listarProdutos, produtos -> {
            produtosMap.clear();
            for (ApiClient.Produto produto : produtos) {
                produtosMap.put(produto.id(), new Produto(produto.id(), produto.nome(), produto.preco(), produto.estoque()));
            }
            // ajusta o carrinho ao estoque atual
            carrinho.values().removeIf(item -> !produtosMap.containsKey(item.id()));
            carrinho.values().forEach(item ->
                    item.setQuantidade(Math.min(item.quantidade(), produtosMap.get(item.id()).estoque())));
            carrinho.values().removeIf(item -> item.quantidade() <= 0);
            renderizarProdutos();
            atualizarCarrinho();
        }, erro -> mostrarErro("Não foi possível carregar os produtos", erro));
    }

    private void carregarPessoas() {
        executarEmBackground("carregar-clientes", apiClient::listarClientes, clientes -> {
            ApiClient.Pessoa atual = clienteBox.getValue();
            clienteBox.getItems().setAll(clientes);
            if (atual != null) {
                clientes.stream().filter(c -> c.id().equals(atual.id())).findFirst().ifPresent(clienteBox::setValue);
            }
        }, erro -> Ui.mensagem(statusLabel, "Erro ao carregar clientes: " + erro.getMessage(), true));

        executarEmBackground("carregar-funcionarios", apiClient::listarFuncionarios, funcionarios -> {
            ApiClient.Pessoa atual = funcionarioBox.getValue();
            funcionarioBox.getItems().setAll(funcionarios);
            // mantém a escolha atual; na primeira carga usa o funcionário logado
            Long preferido = atual != null ? atual.id() : usuarioLogado == null ? null : usuarioLogado.id();
            if (preferido != null) {
                funcionarios.stream().filter(f -> f.id().equals(preferido)).findFirst()
                        .ifPresent(funcionarioBox::setValue);
            }
        }, erro -> Ui.mensagem(statusLabel, "Erro ao carregar funcionários: " + erro.getMessage(), true));
    }

    private void renderizarProdutos() {
        if (productsGrid == null) {
            return;
        }

        productsGrid.getChildren().clear();
        int index = 0;
        String filtro = searchField == null ? "" : searchField.getText().trim().toLowerCase();

        for (Produto produto : produtosMap.values()) {
            boolean combina = filtro.isEmpty()
                    || produto.nome().toLowerCase().contains(filtro)
                    || String.valueOf(produto.id()).equals(filtro)
                    || Ui.codigoProduto(produto.id()).toLowerCase().equals(filtro);
            if (!combina) {
                continue;
            }
            VBox card = createProductCard(produto);
            productsGrid.add(card, index % COLUNAS_PRODUTOS, index / COLUNAS_PRODUTOS);
            index++;
        }
    }

    private VBox createProductCard(Produto produto) {
        Label nome = new Label(produto.nome());
        nome.getStyleClass().add("product-name");
        nome.setWrapText(true);

        Label preco = new Label("R$ " + Ui.dinheiro(produto.preco()));
        preco.getStyleClass().add("product-price");

        Label estoque = new Label(produto.estoque() > 0 ? "Estoque: " + produto.estoque() : "Sem estoque");
        estoque.getStyleClass().add("product-stock");

        Button addButton = new Button("+  Adicionar");
        addButton.getStyleClass().add("add-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setPrefHeight(38);
        addButton.setDisable(produto.estoque() <= 0);
        addButton.setOnAction(event -> adicionarAoCarrinho(produto));

        VBox card = new VBox(8, Ui.iconeCartao(iconeDe(produto.nome())), nome, preco, estoque, addButton);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private String iconeDe(String nome) {
        String n = nome.toLowerCase();
        if (n.contains("café") || n.contains("cafe") || n.contains("cappuccino")) {
            return "☕";
        }
        if (n.contains("refri") || n.contains("água") || n.contains("agua") || n.contains("suco")) {
            return "🥤";
        }
        if (n.contains("sorvete") || n.contains("picolé") || n.contains("picole")) {
            return "🍦";
        }
        if (n.contains("chocolate") || n.contains("bala") || n.contains("doce")) {
            return "🍫";
        }
        if (n.contains("pão") || n.contains("pao") || n.contains("coxinha") || n.contains("esfirra")) {
            return "🥐";
        }
        return "🛍";
    }

    // --------------------------------------------------------------- carrinho

    private void adicionarAoCarrinho(Produto produto) {
        CartItem existente = carrinho.get(produto.id());
        int atual = existente == null ? 0 : existente.quantidade();
        if (atual + 1 > produto.estoque()) {
            Ui.mensagem(statusLabel, "Estoque insuficiente para \"" + produto.nome()
                    + "\" (disponível: " + produto.estoque() + ").", true);
            return;
        }
        Ui.mensagem(statusLabel, "", false);
        if (existente == null) {
            carrinho.put(produto.id(), new CartItem(produto.id(), produto.nome(), produto.preco(), 1));
        } else {
            existente.setQuantidade(atual + 1);
        }
        atualizarCarrinho();
    }

    private void alterarQuantidade(CartItem item, int delta) {
        if (delta > 0) {
            Produto produto = produtosMap.get(item.id());
            if (produto != null) {
                adicionarAoCarrinho(produto);
            }
            return;
        }
        int nova = item.quantidade() + delta;
        if (nova <= 0) {
            carrinho.remove(item.id());
        } else {
            item.setQuantidade(nova);
        }
        atualizarCarrinho();
    }

    private void atualizarCarrinho() {
        carrinhoBox.getChildren().clear();
        double total = 0;

        for (CartItem item : carrinho.values()) {
            Button diminuir = Ui.botao("−", "qty-button");
            diminuir.setOnAction(event -> alterarQuantidade(item, -1));
            Button aumentar = Ui.botao("+", "qty-button");
            aumentar.setOnAction(event -> alterarQuantidade(item, 1));
            Label qtd = new Label(String.valueOf(item.quantidade()));
            qtd.getStyleClass().add("cart-item-name");
            qtd.setMinWidth(18);
            qtd.setAlignment(Pos.CENTER);
            HBox stepper = new HBox(3, diminuir, qtd, aumentar);
            stepper.setAlignment(Pos.CENTER);
            stepper.setMinWidth(78);
            stepper.setPrefWidth(78);

            Label nome = celula(item.nome(), 0, "cart-item-name", Pos.CENTER_LEFT, true);
            nome.setWrapText(true);

            HBox row = new HBox(6, nome, stepper,
                    celula(Ui.dinheiro(item.preco()), 48, "cart-item-muted", Pos.CENTER_RIGHT, false),
                    celula(Ui.dinheiro(item.preco() * item.quantidade()), 62, "cart-item-value", Pos.CENTER_RIGHT, false));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("cart-row");
            carrinhoBox.getChildren().add(row);

            total += item.preco() * item.quantidade();
        }

        if (carrinho.isEmpty()) {
            Label vazio = new Label("Nenhum item no carrinho.");
            vazio.getStyleClass().add("cart-item-muted");
            vazio.setPadding(new Insets(14, 0, 0, 0));
            carrinhoBox.getChildren().add(vazio);
        }

        totalValueLabel.setText("R$ " + Ui.dinheiro(total));
    }

    private void finalizarVenda() {
        if (carrinho.isEmpty()) {
            Ui.mensagem(statusLabel, "Selecione pelo menos um produto para vender.", true);
            return;
        }

        ApiClient.Pessoa cliente = clienteBox.getValue();
        ApiClient.Pessoa funcionario = funcionarioBox.getValue();
        if (cliente == null || funcionario == null) {
            Ui.mensagem(statusLabel, "Selecione o cliente e o vendedor.", true);
            return;
        }

        List<Long> productIds = new ArrayList<>();
        for (CartItem item : carrinho.values()) {
            for (int i = 0; i < item.quantidade(); i++) {
                productIds.add(item.id());
            }
        }

        finalizarButton.setDisable(true);
        Ui.mensagem(statusLabel, "Enviando venda...", false);

        executarEmBackground("finalizar-venda",
                () -> apiClient.finalizarVenda(cliente.id(), funcionario.id(), productIds, LocalDate.now().toString()),
                resposta -> {
                    finalizarButton.setDisable(false);
                    Ui.mensagem(statusLabel, "Venda registrada com sucesso para " + cliente.nome() + ".", false);
                    carrinho.clear();
                    atualizarCarrinho();
                    carregarProdutos();
                },
                erro -> {
                    finalizarButton.setDisable(false);
                    Ui.mensagem(statusLabel, "Erro ao enviar venda: " + erro.getMessage(), true);
                });
    }

    // ----------------------------------------------------------------- modelo

    public record Produto(Long id, String nome, double preco, int estoque) {
    }

    public static class CartItem {
        private final Long id;
        private final String nome;
        private final double preco;
        private int quantidade;

        public CartItem(Long id, String nome, double preco, int quantidade) {
            this.id = id;
            this.nome = nome;
            this.preco = preco;
            this.quantidade = quantidade;
        }

        public Long id() {
            return id;
        }

        public String nome() {
            return nome;
        }

        public double preco() {
            return preco;
        }

        public int quantidade() {
            return quantidade;
        }

        public void setQuantidade(int quantidade) {
            this.quantidade = quantidade;
        }
    }
}
