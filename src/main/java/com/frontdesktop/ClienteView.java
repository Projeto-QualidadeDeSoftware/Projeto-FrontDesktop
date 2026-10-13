package com.frontdesktop;

import com.frontdesktop.api.ApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/** Cadastro de Cliente: cartão central com o formulário. */
public class ClienteView {

    private final ApiClient api;
    private final VBox root = new VBox();
    private final TextField nome = Ui.entrada("Ex: Maria Fernandes da Silva");
    private final TextField cpf = Ui.entrada("000.000.000-00");
    private final TextField telefone = Ui.entrada("(00) 00000-0000");
    private final TextField email = Ui.entrada("cliente@email.com");
    private final Label status = new Label();
    private final Button salvar = Ui.botao("Salvar Cliente", "primary-button");

    public ClienteView(ApiClient api) {
        this.api = api;
        montar();
    }

    public Node getView() {
        return root;
    }

    public void limpar() {
        nome.clear();
        cpf.clear();
        telefone.clear();
        email.clear();
        Ui.mensagem(status, "", false);
    }

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        HBox titulo = new HBox(12, Ui.iconeCartao("👤"), new Label("Cadastrar Novo Cliente"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        titulo.getChildren().get(1).getStyleClass().add("section-title");

        HBox linha = new HBox(16, Ui.campo("CPF", cpf), Ui.campo("Telefone", telefone));
        HBox.setHgrow(linha.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(linha.getChildren().get(1), Priority.ALWAYS);

        Button cancelar = Ui.botao("Cancelar", "secondary-button");
        cancelar.setOnAction(event -> limpar());
        salvar.setOnAction(event -> salvar());

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox botoes = new HBox(12, espaco, cancelar, salvar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        status.setWrapText(true);
        status.getStyleClass().add("status-label");

        VBox card = new VBox(18, titulo, Ui.campo("Nome completo", nome), linha, Ui.campo("E-mail", email),
                status, botoes);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(32));
        card.setMaxWidth(520);
        card.setMinWidth(520);

        VBox centro = new VBox(card);
        centro.setAlignment(Pos.TOP_CENTER);
        centro.setPadding(new Insets(60, 0, 0, 0));
        root.getChildren().addAll(Ui.cabecalho("Cadastro de Cliente"), centro);
    }

    private void salvar() {
        if (nome.getText().isBlank() || cpf.getText().isBlank()
                || telefone.getText().isBlank() || email.getText().isBlank()) {
            Ui.mensagem(status, "Preencha todos os campos.", true);
            return;
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("nome", nome.getText().trim());
        corpo.put("cpf", cpf.getText().trim());
        corpo.put("email", email.getText().trim());
        corpo.put("telefone", telefone.getText().trim());

        salvar.setDisable(true);
        Ui.mensagem(status, "Salvando...", false);
        Async.run("cliente-salvar", () -> {
            api.salvar("/cliente", null, corpo);
            return Boolean.TRUE;
        }, ok -> {
            salvar.setDisable(false);
            limpar();
            Ui.mensagem(status, "Cliente cadastrado com sucesso.", false);
        }, erro -> {
            salvar.setDisable(false);
            Ui.mensagem(status, erro.getMessage(), true);
        });
    }
}
