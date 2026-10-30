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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Funcionários: cartões de totais, lista paginada com busca e cadastro/edição em janela. */
public class FuncionariosView {

    private static final String ENDPOINT = "/funcionario";
    private static final int POR_PAGINA = 6;

    /** Mesma lista do front web e do mobile. */
    private static final List<String> CARGOS = List.of(
            "Administrador", "Gerente", "Operador de Caixa", "Supervisor", "Estoquista", "Repositor");

    private final ApiClient api;
    private final VBox root = new VBox(18);

    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> filtrados = new FilteredList<>(dados, p -> true);
    private final Paginacao<JsonNode> paginacao = new Paginacao<>(filtrados, POR_PAGINA, "funcionários", false);
    private final TableView<JsonNode> tabela = new TableView<>(paginacao.visiveis());
    private final TextField busca = Ui.entrada("Buscar funcionário...");
    private final Label status = new Label();
    private final Label totalFuncionarios = new Label("0");
    private final Label resultados = new Label("0");

    public FuncionariosView(ApiClient api) {
        this.api = api;
        montar();
    }

    public Node getView() {
        return root;
    }

    public void recarregar() {
        Async.run("funcionarios-listar", () -> api.listar(ENDPOINT), registros -> {
            dados.setAll(registros);
            totalFuncionarios.setText(String.valueOf(registros.size()));
        }, erro -> Ui.mensagem(status, "Erro ao carregar funcionários: " + Ui.mensagemDe(erro), true));
    }

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        Button novo = Ui.botao("+  Novo funcionário", "primary-button");
        novo.setOnAction(event -> abrirDialogo(null));
        HBox topo = Ui.topo(Ui.cabecalho("Gerenciamento", "Funcionários",
                "Consulte e gerencie os funcionários cadastrados."), novo);

        HBox resumo = new HBox(18, Ui.indicador("Funcionários cadastrados", totalFuncionarios, false),
                Ui.indicador("Resultados encontrados", resultados, true));
        filtrados.addListener((javafx.collections.ListChangeListener<JsonNode>) mudanca ->
                resultados.setText(String.valueOf(filtrados.size())));

        busca.getStyleClass().add("search-field");
        busca.setPrefWidth(300);
        busca.textProperty().addListener((obs, antigo, texto) -> {
            String filtro = texto.trim().toLowerCase();
            filtrados.setPredicate(f -> filtro.isEmpty()
                    || f.path("nome").asText("").toLowerCase().contains(filtro)
                    || f.path("cargo").asText("").toLowerCase().contains(filtro)
                    || f.path("cpf").asText("").contains(filtro));
            paginacao.inicio();
        });
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox cabecalhoCard = new HBox(12,
                Ui.tituloCartao("Lista de funcionários", "Pesquise por nome, CPF ou cargo."), espaco, busca);
        cabecalhoCard.setAlignment(Pos.CENTER_LEFT);

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum funcionário encontrado."));
        tabela.setSelectionModel(null);
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("CODIGO", f -> "#" + f.path("id").asLong(), 0.8, "cell-id", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaNo("FUNCIONÁRIO", 2.4, Ui.ESQUERDA, f -> {
            Label nome = new Label(f.path("nome").asText(""));
            nome.getStyleClass().add("cell-strong");
            HBox linha = new HBox(10, Ui.avatar(f.path("nome").asText("")), nome);
            linha.setAlignment(Pos.CENTER_LEFT);
            return linha;
        }));
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("CPF", f -> f.path("cpf").asText(""), 1.6, "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("CARGO", f -> f.path("cargo").asText(""), 1.6, "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaTexto("SALÁRIO", f -> "R$ " + Ui.dinheiro(f.path("salario").asDouble()), 1.3, "cell-strong", Ui.ESQUERDA));
        tabela.getColumns().add(Ui.<JsonNode>colunaNo("AÇÕES", 1.4, Ui.DIREITA,
                f -> Ui.acoesTexto(() -> abrirDialogo(f), () -> excluir(f))));
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
        TextField nome = Ui.entrada("Ex: João da Silva Santos");
        TextField cpf = Ui.mascaraCpf(Ui.entrada("000.000.000-00"));
        ComboBox<String> cargo = new ComboBox<>();
        cargo.setPromptText("Selecione o cargo");
        cargo.setMaxWidth(Double.MAX_VALUE);
        cargo.getItems().addAll(CARGOS);
        TextField salario = Ui.entrada("R$ 0,00");
        PasswordField senha = Ui.senha("Digite a senha");
        PasswordField confirmarSenha = Ui.senha("Confirme a senha");
        Label erro = new Label();
        erro.setWrapText(true);
        erro.getStyleClass().addAll("status-label", "status-error");

        if (existente != null) {
            nome.setText(existente.path("nome").asText(""));
            cpf.setText(existente.path("cpf").asText(""));
            String cargoAtual = existente.path("cargo").asText("").trim();
            if (!cargoAtual.isEmpty() && !CARGOS.contains(cargoAtual)) {
                // funcionário antigo cadastrado com outro cargo: continua aparecendo na lista
                cargo.getItems().add(cargoAtual);
            }
            cargo.setValue(cargoAtual.isEmpty() ? null : cargoAtual);
            salario.setText(Ui.dinheiro(existente.path("salario").asDouble()));
            // a API não devolve a senha (ela fica guardada protegida): é preciso informar a senha de novo
            erro.setText("Informe a senha do funcionário para salvar (pode ser a mesma).");
            erro.getStyleClass().remove("status-error");
        }

        Button cancelar = Ui.botao("Cancelar", "secondary-button");
        Button salvar = Ui.botao(existente == null ? "Cadastrar funcionário" : "Salvar alterações", "primary-button");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox botoes = new HBox(12, espaco, cancelar, salvar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        Label titulo = new Label(existente == null ? "Novo funcionário" : "Editar funcionário");
        titulo.getStyleClass().add("section-title");
        HBox cabecalho = new HBox(12, Ui.iconeCartao("👥"), titulo);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        HBox linhaCpf = new HBox(14, Ui.campo("CPF *", cpf), Ui.campo("Cargo *", cargo));
        HBox linhaSenha = new HBox(14, Ui.campo("Senha de acesso *", senha), Ui.campo("Confirmar senha *", confirmarSenha));
        for (HBox linha : new HBox[]{linhaCpf, linhaSenha}) {
            linha.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        }
        VBox card = new VBox(16, cabecalho, Ui.campo("Nome completo *", nome), linhaCpf,
                Ui.campo("Salário *", salario), linhaSenha, erro, botoes);
        card.setPadding(new Insets(28));
        card.setPrefWidth(560);
        Stage janela = Ui.modal(root, titulo.getText(), card);

        cancelar.setOnAction(event -> janela.close());
        Long id = existente == null ? null : existente.path("id").asLong();
        salvar.setOnAction(event -> {
            erro.getStyleClass().remove("status-error");
            erro.getStyleClass().add("status-error");
            String cargoTexto = cargo.getValue() == null ? "" : cargo.getValue().trim();
            if (nome.getText().isBlank() || cpf.getText().isBlank() || cargoTexto.isEmpty()
                    || salario.getText().isBlank() || senha.getText().isEmpty()) {
                erro.setText("Preencha todos os campos.");
                return;
            }
            if (senha.getText().length() < 4) {
                erro.setText("A senha deve ter pelo menos 4 caracteres.");
                return;
            }
            if (!senha.getText().equals(confirmarSenha.getText())) {
                erro.setText("A confirmação de senha não confere.");
                return;
            }
            double valorSalario;
            try {
                valorSalario = Ui.lerDecimal(salario.getText());
            } catch (NumberFormatException e) {
                erro.setText("Salário inválido.");
                return;
            }
            if (valorSalario <= 0) {
                erro.setText("O salário deve ser maior que zero.");
                return;
            }
            if (!Ui.cpfValido(cpf.getText())) {
                erro.setText("CPF inválido.");
                return;
            }

            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("nome", nome.getText().trim());
            corpo.put("cpf", cpf.getText().trim());
            corpo.put("cargo", cargoTexto);
            corpo.put("senha", senha.getText());
            corpo.put("salario", valorSalario);

            salvar.setDisable(true);
            erro.setText("");
            Async.run("funcionarios-salvar", () -> {
                api.salvar(ENDPOINT, id, corpo);
                return Boolean.TRUE;
            }, ok -> {
                janela.close();
                recarregar();
                Ui.mensagem(status, "Funcionário salvo com sucesso!", false);
            }, falha -> {
                salvar.setDisable(false);
                erro.setText(Ui.mensagemDe(falha));
            });
        });
        janela.showAndWait();
    }

    private void excluir(JsonNode funcionario) {
        long id = funcionario.path("id").asLong();
        if (!Ui.confirmar("Excluir o funcionário \"" + funcionario.path("nome").asText() + "\"?")) {
            return;
        }
        Async.run("funcionarios-excluir", () -> {
            api.excluir(ENDPOINT, id);
            return Boolean.TRUE;
        }, ok -> {
            recarregar();
            Ui.mensagem(status, "Funcionário excluído com sucesso!", false);
        }, erro -> Ui.mensagem(status, Ui.mensagemDe(erro), true));
    }
}
