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
import javafx.scene.control.PasswordField;
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
import java.util.function.Function;

/** Gerenciar Funcionários: formulário no topo e lista embaixo. */
public class FuncionariosView {

    private static final String ENDPOINT = "/funcionario";

    private final ApiClient api;
    private final VBox root = new VBox(18);

    private final TextField nome = Ui.entrada("Ex: João da Silva Santos");
    private final TextField cpf = Ui.mascaraCpf(Ui.entrada("000.000.000-00"));
    private final ComboBox<String> cargo = new ComboBox<>();
    private final TextField salario = Ui.entrada("R$ 0,00");
    private final PasswordField senha = Ui.senha("Digite a senha");
    private final PasswordField confirmarSenha = Ui.senha("Confirme a senha");
    private final Label formTitulo = new Label("Cadastrar Novo Funcionário");
    private final Button salvar = Ui.botao("Salvar Funcionário", "primary-button");
    private final Label status = new Label();

    private final ObservableList<JsonNode> dados = FXCollections.observableArrayList();
    private final FilteredList<JsonNode> filtrados = new FilteredList<>(dados, p -> true);
    private final TableView<JsonNode> tabela = new TableView<>(filtrados);
    private final TextField busca = Ui.entrada("Buscar funcionário...");

    private Long idEmEdicao;

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
            Ui.mensagem(status, "", false);
        }, erro -> Ui.mensagem(status, "Erro ao carregar funcionários: " + erro.getMessage(), true));
    }

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        cargo.setEditable(true);
        cargo.setPromptText("Selecione o cargo");
        cargo.getItems().addAll("Operador de Caixa", "Gerente", "Supervisor", "Estoquista", "Repositor");

        root.getChildren().addAll(Ui.cabecalho("Gerenciar Funcionários"), montarFormulario(), montarLista());
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
        grade.add(Ui.campo("Nome completo *", nome), 0, 0);
        grade.add(Ui.campo("CPF *", cpf), 1, 0);
        grade.add(Ui.campo("Cargo *", cargo), 0, 1);
        grade.add(Ui.campo("Salário *", salario), 1, 1);
        grade.add(Ui.campo("Senha de acesso *", senha), 0, 2);
        grade.add(Ui.campo("Confirmar senha *", confirmarSenha), 1, 2);

        Button cancelar = Ui.botao("Cancelar", "outline-button");
        cancelar.setPadding(new Insets(10, 22, 10, 22));
        cancelar.setOnAction(event -> limparFormulario());
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
        HBox titulo = new HBox(12, Ui.iconeCartao("👥"), new Label("Funcionários Cadastrados"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        titulo.getChildren().get(1).getStyleClass().add("section-title");

        busca.getStyleClass().add("search-field");
        busca.setPrefWidth(300);
        busca.textProperty().addListener((obs, antigo, texto) -> {
            String filtro = texto.trim().toLowerCase();
            filtrados.setPredicate(f -> filtro.isEmpty()
                    || f.path("nome").asText("").toLowerCase().contains(filtro)
                    || f.path("cargo").asText("").toLowerCase().contains(filtro)
                    || f.path("cpf").asText("").contains(filtro));
        });

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox cabecalho = new HBox(12, titulo, espaco, busca);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum funcionário encontrado."));
        tabela.setSelectionModel(null);
        tabela.getColumns().add(coluna("ID", f -> String.format("%03d", f.path("id").asLong()), 0.7, "cell-id", Ui.ESQUERDA));
        tabela.getColumns().add(coluna("NOME", f -> f.path("nome").asText(""), 2.4, "cell-strong", Ui.ESQUERDA));
        tabela.getColumns().add(coluna("CARGO", f -> f.path("cargo").asText(""), 1.8, "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(coluna("CPF", f -> f.path("cpf").asText(""), 1.6, "cell-muted", Ui.ESQUERDA));
        tabela.getColumns().add(coluna("SALÁRIO (R$)", f -> Ui.dinheiro(f.path("salario").asDouble()), 1.3, "cell-strong", Ui.DIREITA));
        tabela.getColumns().add(colunaAcoes());
        VBox.setVgrow(tabela, Priority.ALWAYS);

        VBox card = new VBox(14, cabecalho, tabela);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18, 20, 20, 20));
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
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
            protected void updateItem(JsonNode funcionario, boolean vazio) {
                super.updateItem(funcionario, vazio);
                Ui.alinharCelula(this, Ui.CENTRO);
                if (vazio || funcionario == null) {
                    setGraphic(null);
                    return;
                }
                HBox acoes = new HBox(4,
                        Ui.botaoIcone("✎", false, () -> editar(funcionario)),
                        Ui.botaoIcone("🗑", true, () -> excluir(funcionario)));
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
        salario.clear();
        senha.clear();
        confirmarSenha.clear();
        cargo.getSelectionModel().clearSelection();
        cargo.getEditor().clear();
        formTitulo.setText("Cadastrar Novo Funcionário");
    }

    private void editar(JsonNode funcionario) {
        idEmEdicao = funcionario.path("id").asLong();
        nome.setText(funcionario.path("nome").asText(""));
        cpf.setText(funcionario.path("cpf").asText(""));
        cargo.setValue(funcionario.path("cargo").asText(""));
        salario.setText(Ui.dinheiro(funcionario.path("salario").asDouble()));
        String atual = funcionario.path("senha").asText("");
        senha.setText(atual);
        confirmarSenha.setText(atual);
        formTitulo.setText("Editar Funcionário #" + idEmEdicao);
        Ui.mensagem(status, "", false);
    }

    private void salvar() {
        String cargoTexto = cargo.getEditor().getText() == null ? "" : cargo.getEditor().getText().trim();
        if (nome.getText().isBlank() || cpf.getText().isBlank() || cargoTexto.isEmpty()
                || salario.getText().isBlank() || senha.getText().isEmpty()) {
            Ui.mensagem(status, "Preencha todos os campos obrigatórios.", true);
            return;
        }
        if (!senha.getText().equals(confirmarSenha.getText())) {
            Ui.mensagem(status, "A confirmação de senha não confere.", true);
            return;
        }

        double valorSalario;
        try {
            valorSalario = Ui.lerDecimal(salario.getText());
        } catch (NumberFormatException e) {
            Ui.mensagem(status, "Salário inválido.", true);
            return;
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", nome.getText().trim());
        corpo.put("cpf", cpf.getText().trim());
        corpo.put("cargo", cargoTexto);
        corpo.put("senha", senha.getText());
        corpo.put("salario", valorSalario);

        Long id = idEmEdicao;
        salvar.setDisable(true);
        Ui.mensagem(status, "Salvando...", false);
        Async.run("funcionarios-salvar", () -> {
            api.salvar(ENDPOINT, id, corpo);
            return Boolean.TRUE;
        }, ok -> {
            salvar.setDisable(false);
            limparFormulario();
            recarregar();
            Ui.mensagem(status, "Funcionário salvo com sucesso.", false);
        }, erro -> {
            salvar.setDisable(false);
            Ui.mensagem(status, erro.getMessage(), true);
        });
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
            if (java.util.Objects.equals(idEmEdicao, id)) {
                limparFormulario();
            }
            recarregar();
            Ui.mensagem(status, "Funcionário excluído.", false);
        }, erro -> Ui.mensagem(status, erro.getMessage(), true));
    }
}
