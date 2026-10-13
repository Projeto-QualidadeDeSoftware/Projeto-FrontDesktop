package com.frontdesktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.frontdesktop.api.ApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** Tela de login: CPF + senha de um funcionário cadastrado. */
public class LoginView {

    private final ApiClient api;
    private final Consumer<ApiClient.Usuario> aoEntrar;
    private final Runnable aoPrimeiroAcesso;

    private final StackPane root = new StackPane();
    private final TextField cpf = Ui.entrada("000.000.000-00");
    private final PasswordField senha = Ui.senha("Digite sua senha");
    private final Button entrar = new Button("ENTRAR");
    private final Button primeiroAcesso = Ui.botao("Cadastrar primeiro funcionário", "outline-button");
    private final Label status = new Label();

    public LoginView(ApiClient api, Consumer<ApiClient.Usuario> aoEntrar, Runnable aoPrimeiroAcesso) {
        this.api = api;
        this.aoEntrar = aoEntrar;
        this.aoPrimeiroAcesso = aoPrimeiroAcesso;
        montar();
    }

    public Node getView() {
        return root;
    }

    private void montar() {
        root.getStyleClass().add("main-panel");

        Label glyph = new Label("🏪");
        glyph.getStyleClass().add("brand-icon-glyph");
        StackPane icone = new StackPane(glyph);
        icone.getStyleClass().add("brand-icon");

        Label marca = new Label("Prontê Conveniência");
        marca.getStyleClass().add("title");
        Label subtitulo = new Label("Entre com seu CPF e senha para abrir o caixa");
        subtitulo.getStyleClass().add("cart-item-muted");

        entrar.getStyleClass().add("finish-button");
        entrar.setMaxWidth(Double.MAX_VALUE);
        entrar.setDefaultButton(true);
        entrar.setOnAction(event -> autenticar());

        primeiroAcesso.setMaxWidth(Double.MAX_VALUE);
        primeiroAcesso.setPadding(new Insets(10));
        primeiroAcesso.setVisible(false);
        primeiroAcesso.managedProperty().bind(primeiroAcesso.visibleProperty());
        primeiroAcesso.setOnAction(event -> aoPrimeiroAcesso.run());

        status.setWrapText(true);
        status.getStyleClass().add("status-label");

        VBox card = new VBox(16, icone, marca, subtitulo,
                Ui.campo("CPF", cpf), Ui.campo("Senha", senha), entrar, primeiroAcesso, status);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(36));
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        root.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
    }

    private void autenticar() {
        String cpfDigitado = apenasDigitos(cpf.getText());
        String senhaDigitada = senha.getText();
        if (cpfDigitado.isEmpty() || senhaDigitada.isEmpty()) {
            Ui.mensagem(status, "Informe CPF e senha.", true);
            return;
        }

        entrar.setDisable(true);
        primeiroAcesso.setVisible(false);
        Ui.mensagem(status, "Verificando...", false);

        Async.run("login", () -> api.listar("/funcionario"), funcionarios -> {
            entrar.setDisable(false);
            if (funcionarios.isEmpty()) {
                Ui.mensagem(status, "Nenhum funcionário cadastrado ainda.", true);
                primeiroAcesso.setVisible(true);
                return;
            }
            ApiClient.Usuario usuario = encontrar(funcionarios, cpfDigitado, senhaDigitada);
            if (usuario == null) {
                senha.clear();
                Ui.mensagem(status, "CPF ou senha incorretos.", true);
                return;
            }
            Ui.mensagem(status, "", false);
            senha.clear();
            aoEntrar.accept(usuario);
        }, erro -> {
            entrar.setDisable(false);
            Ui.mensagem(status, "Não foi possível conectar à API: " + erro.getMessage(), true);
        });
    }

    private ApiClient.Usuario encontrar(List<JsonNode> funcionarios, String cpfDigitado, String senhaDigitada) {
        for (JsonNode f : funcionarios) {
            boolean cpfOk = apenasDigitos(f.path("cpf").asText("")).equals(cpfDigitado);
            boolean senhaOk = f.path("senha").asText("").equals(senhaDigitada);
            if (cpfOk && senhaOk) {
                return new ApiClient.Usuario(f.path("id").asLong(), f.path("nome").asText(""),
                        f.path("cargo").asText(""));
            }
        }
        return null;
    }

    private static String apenasDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }
}
