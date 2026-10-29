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
import javafx.scene.control.DatePicker;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private static final int COLUNAS_PRODUTOS = 2;
    private static final int ESTOQUE_BAIXO = 15;

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
    private Label produtosDisponiveisLabel;
    private Label itensCarrinhoLabel;
    private Label totalVendaLabel;
    private Label qtdItensLabel;
    private HBox cabecalhoCarrinho;
    private DatePicker dataVenda;
    private boolean enviandoVenda;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Prontê Conveniência");
        // token vencido: volta para o login, uma vez só, na thread da interface
        apiClient.aoExpirarSessao(() -> javafx.application.Platform.runLater(() -> {
            if (usuarioLogado != null) {
                mostrarLogin("Sua sessão expirou. Entre novamente.");
            }
        }));
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
        mostrarLogin(null);
    }

    private void mostrarLogin(String aviso) {
        apiClient.sair();
        usuarioLogado = null;
        carrinho.clear();
        produtosMap.clear();
        navButtons.clear();

        LoginView login = new LoginView(apiClient, usuario -> abrirSistema(usuario, false), aviso);
        stage.setScene(novaCena(login.getView()));
    }

    /**
     * @param usuario        funcionário autenticado
     * @param primeiroAcesso abre direto em Gerenciar Funcionários
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
        alert.setContentText(Ui.mensagemDe(erro));
        alert.show();
    }

    // --------------------------------------------------------------- sidebar

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(256);
        sidebar.setMinWidth(256);

        HBox logoBox = new HBox(Ui.logo(210));
        logoBox.setAlignment(Pos.CENTER);
        logoBox.getStyleClass().add("logo-box");

        Button caixa = createNavButton("🛒", "Frente de Caixa", true);
        Button produtos = createNavButton("📦", "Produtos", false);
        Button clientes = createNavButton("👤", "Clientes", false);
        Button funcionarios = createNavButton("👥", "Funcionários", false);
        Button vendas = createNavButton("📊", "Vendas", false);
        Button assistente = createNavButton("💬", "Assistente IA", false);
        navButtons.addAll(List.of(caixa, produtos, clientes, funcionarios, vendas, assistente));

        ProdutosView produtosView = new ProdutosView(apiClient);
        ClienteView clienteView = new ClienteView(apiClient);
        FuncionariosView funcionariosView = new FuncionariosView(apiClient);
        AssistenteView assistenteView = new AssistenteView(apiClient);
        VendasView vendasView = new VendasView(apiClient);
        AuditoriaView auditoriaView = new AuditoriaView(apiClient);

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

        vendas.setOnAction(event -> {
            marcarAtivo(vendas);
            root.setCenter(vendasView.getView());
            vendasView.recarregar();
        });

        assistente.setOnAction(event -> {
            marcarAtivo(assistente);
            root.setCenter(assistenteView.getView());
            assistenteView.focar();
        });

        VBox nav = new VBox(8, caixa, produtos, clientes, funcionarios, vendas, assistente);
        nav.setPadding(new Insets(8, 16, 16, 16));
        // como na web, a Auditoria só aparece para o Administrador
        if (usuarioLogado != null && "administrador".equalsIgnoreCase(usuarioLogado.cargo().trim())) {
            Button auditoria = createNavButton("🛡", "Auditoria", false);
            navButtons.add(auditoria);
            auditoria.setOnAction(event -> {
                marcarAtivo(auditoria);
                root.setCenter(auditoriaView.getView());
                auditoriaView.recarregar();
            });
            nav.getChildren().add(auditoria);
        }

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
            Button sair = Ui.botao("⇥  Sair", "link-button");
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
        searchField = Ui.entrada("Buscar produto pelo nome...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, antigo, novo) -> renderizarProdutos());

        productsGrid = new GridPane();
        productsGrid.setHgap(14);
        productsGrid.setVgap(14);
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

        VBox esquerda = new VBox(14, Ui.tituloCartao("Produtos", "Clique em um produto para adicioná-lo."),
                searchField, scroll);
        esquerda.getStyleClass().add("card");
        esquerda.setPadding(new Insets(20));
        HBox.setHgrow(esquerda, Priority.ALWAYS);

        VBox direita = createCartPanel();

        HBox centro = new HBox(18, esquerda, direita);
        VBox.setVgrow(centro, Priority.ALWAYS);

        produtosDisponiveisLabel = new Label("0");
        itensCarrinhoLabel = new Label("0");
        totalVendaLabel = new Label("R$ 0,00");
        HBox resumo = new HBox(18,
                Ui.indicador("Produtos disponíveis", produtosDisponiveisLabel, false),
                Ui.indicador("Itens no carrinho", itensCarrinhoLabel, false),
                Ui.indicador("Total da venda", totalVendaLabel, true));

        VBox content = new VBox(18, Ui.cabecalho("Vendas", "Frente de Caixa",
                "Selecione os produtos, confira o carrinho e finalize a venda."), resumo, centro);
        content.setPadding(Ui.padrao());
        content.getStyleClass().add("main-panel");
        return content;
    }

    private VBox createCartPanel() {
        carrinhoBox = new VBox();
        ScrollPane scroll = new ScrollPane(carrinhoBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefHeight(150);
        scroll.setMinHeight(110);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        cabecalhoCarrinho = linhaCabecalhoCarrinho();

        clienteBox = new ComboBox<>();
        clienteBox.setPromptText("Selecione um cliente");
        funcionarioBox = new ComboBox<>();
        funcionarioBox.setPromptText("Selecione um funcionário");

        dataVenda = new DatePicker(LocalDate.now());
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dataVenda.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate data) {
                return data == null ? "" : data.format(formato);
            }

            @Override
            public LocalDate fromString(String texto) {
                try {
                    return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.trim(), formato);
                } catch (java.time.format.DateTimeParseException e) {
                    return null;
                }
            }
        });
        dataVenda.setMaxWidth(Double.MAX_VALUE);

        qtdItensLabel = new Label("0");
        qtdItensLabel.getStyleClass().add("total-card-label");
        Label qtdTexto = new Label("Quantidade de itens");
        qtdTexto.getStyleClass().add("total-card-label");
        Label totalTexto = new Label("Total a pagar");
        totalTexto.getStyleClass().add("total-card-label");
        totalValueLabel = new Label("R$ 0,00");
        totalValueLabel.getStyleClass().add("total-card-value");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        HBox linhaQtd = new HBox(qtdTexto, sp1, qtdItensLabel);
        HBox linhaTotal = new HBox(totalTexto, sp2, totalValueLabel);
        linhaTotal.setAlignment(Pos.CENTER_LEFT);
        VBox totalBox = new VBox(6, linhaQtd, linhaTotal);
        totalBox.getStyleClass().add("total-card");

        finalizarButton = new Button("Finalizar venda");
        finalizarButton.getStyleClass().add("finish-button");
        finalizarButton.setMaxWidth(Double.MAX_VALUE);
        finalizarButton.setTooltip(new javafx.scene.control.Tooltip("Atalho: F2"));
        finalizarButton.setOnAction(event -> finalizarVenda());
        finalizarButton.setDisable(true);

        Button limpar = Ui.botao("Limpar venda", "secondary-button");
        limpar.setMaxWidth(Double.MAX_VALUE);
        limpar.setOnAction(event -> {
            carrinho.clear();
            atualizarCarrinho();
        });

        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.getStyleClass().add("status-label");

        VBox painel = new VBox(10,
                Ui.tituloCartao("Carrinho da venda", "Confira as quantidades e o valor dos itens."),
                cabecalhoCarrinho, scroll,
                Ui.campo("Cliente", clienteBox), Ui.campo("Funcionário responsável", funcionarioBox),
                Ui.campo("Data da venda", dataVenda), totalBox, finalizarButton, limpar, statusLabel);
        painel.getStyleClass().add("card");
        painel.setPadding(new Insets(20));
        painel.setPrefWidth(400);
        painel.setMinWidth(400);
        painel.setMaxWidth(400);
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
            produtosDisponiveisLabel.setText(String.valueOf(
                    produtosMap.values().stream().filter(p -> p.estoque() > 0).count()));
            renderizarProdutos();
            atualizarCarrinho();
        }, erro -> mostrarErro("Não foi possível carregar os produtos", erro));
    }

    private void carregarPessoas() {
        executarEmBackground("carregar-clientes", apiClient::listarClientes, clientes -> {
            ApiClient.Pessoa atual = clienteBox.getValue();
            clienteBox.getItems().setAll(clientes);
            // mantém a escolha atual; a venda exige escolher o cliente
            if (atual != null) {
                clientes.stream().filter(c -> c.id().equals(atual.id())).findFirst().ifPresent(clienteBox::setValue);
            }
        }, erro -> Ui.mensagem(statusLabel, "Erro ao carregar clientes: " + Ui.mensagemDe(erro), true));

        executarEmBackground("carregar-funcionarios", apiClient::listarFuncionarios, funcionarios -> {
            ApiClient.Pessoa atual = funcionarioBox.getValue();
            funcionarioBox.getItems().setAll(funcionarios);
            // mantém a escolha atual; na primeira carga usa o funcionário logado
            Long preferido = atual != null ? atual.id() : usuarioLogado == null ? null : usuarioLogado.id();
            if (preferido != null) {
                funcionarios.stream().filter(f -> f.id().equals(preferido)).findFirst()
                        .ifPresent(funcionarioBox::setValue);
            }
        }, erro -> Ui.mensagem(statusLabel, "Erro ao carregar funcionários: " + Ui.mensagemDe(erro), true));
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

    /** Cartão clicável: "+" à esquerda, estoque à direita, nome e preço em laranja. */
    private VBox createProductCard(Produto produto) {
        Label mais = new Label("+");
        mais.getStyleClass().add("plus-chip");
        Label estoque = new Label(produto.estoque() > 0 ? "Estoque: " + produto.estoque() : "Sem estoque");
        estoque.getStyleClass().add(produto.estoque() <= 0 ? "stock-none"
                : produto.estoque() <= ESTOQUE_BAIXO ? "stock-low" : "stock-ok");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(mais, espaco, estoque);
        topo.setAlignment(Pos.CENTER_LEFT);

        Label nome = new Label(produto.nome());
        nome.getStyleClass().add("product-name");
        nome.setWrapText(true);
        Label preco = new Label("R$ " + Ui.dinheiro(produto.preco()));
        preco.getStyleClass().add("price-orange");

        VBox card = new VBox(10, topo, nome, preco);
        card.getStyleClass().add("product-tile");
        card.setPadding(new Insets(16));
        card.setMaxWidth(Double.MAX_VALUE);
        if (produto.estoque() > 0) {
            card.setOnMouseClicked(event -> adicionarAoCarrinho(produto));
        } else {
            card.getStyleClass().add("product-tile-disabled");
        }
        return card;
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
        int quantidade = 0;

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
            quantidade += item.quantidade();
        }

        if (carrinho.isEmpty()) {
            Label mais = new Label("+");
            mais.getStyleClass().add("plus-chip");
            Label titulo = new Label("Carrinho vazio");
            titulo.getStyleClass().add("empty-title");
            Label dica = new Label("Adicione produtos para iniciar a venda.");
            dica.getStyleClass().add("subtitle");
            VBox vazio = new VBox(4, mais, titulo, dica);
            vazio.setAlignment(Pos.CENTER);
            vazio.setPadding(new Insets(14, 0, 0, 0));
            carrinhoBox.getChildren().add(vazio);
        }
        cabecalhoCarrinho.setVisible(!carrinho.isEmpty());
        cabecalhoCarrinho.setManaged(!carrinho.isEmpty());

        String totalTexto = "R$ " + Ui.dinheiro(total);
        totalValueLabel.setText(totalTexto);
        totalVendaLabel.setText(totalTexto);
        qtdItensLabel.setText(String.valueOf(quantidade));
        itensCarrinhoLabel.setText(String.valueOf(quantidade));
        atualizarBotaoFinalizar();
    }

    private void atualizarBotaoFinalizar() {
        finalizarButton.setDisable(enviandoVenda || carrinho.isEmpty());
    }

    private void finalizarVenda() {
        // o atalho F2 não passa pelo botão desabilitado: sem isso, dois F2 seguidos registram a venda duas vezes
        if (enviandoVenda) {
            return;
        }
        if (carrinho.isEmpty()) {
            Ui.mensagem(statusLabel, "Selecione pelo menos um produto para vender.", true);
            return;
        }

        ApiClient.Pessoa cliente = clienteBox.getValue();
        ApiClient.Pessoa funcionario = funcionarioBox.getValue();
        if (cliente == null) {
            Ui.mensagem(statusLabel, clienteBox.getItems().isEmpty()
                    ? "Cadastre um cliente antes de realizar a venda." : "Selecione um cliente.", true);
            return;
        }
        if (funcionario == null) {
            Ui.mensagem(statusLabel, "Selecione um funcionário.", true);
            return;
        }
        LocalDate data = dataVenda.getValue() != null ? dataVenda.getValue() : LocalDate.now();

        Map<Long, Integer> itens = new LinkedHashMap<>();
        for (CartItem item : carrinho.values()) {
            itens.put(item.id(), item.quantidade());
        }

        enviandoVenda = true;
        atualizarBotaoFinalizar();
        Ui.mensagem(statusLabel, "Enviando venda...", false);

        executarEmBackground("finalizar-venda",
                () -> apiClient.finalizarVenda(cliente.id(), funcionario.id(), itens, data.toString()),
                resposta -> {
                    enviandoVenda = false;
                    carrinho.clear();
                    atualizarCarrinho();
                    Ui.mensagem(statusLabel, "Venda registrada com sucesso (" + cliente.nome() + ").", false);
                    carregarProdutos();
                },
                erro -> {
                    enviandoVenda = false;
                    atualizarBotaoFinalizar();
                    Ui.mensagem(statusLabel, Ui.mensagemDe(erro), true);
                    // o estoque pode ter mudado em outro caixa: recarrega para mostrar o valor atual
                    carregarProdutos();
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
