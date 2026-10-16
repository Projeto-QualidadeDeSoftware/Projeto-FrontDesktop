package com.frontdesktop;

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

import java.text.Normalizer;
import java.util.Set;
import java.util.function.Consumer;

/** Tela de login: CPF + senha de um funcionário cadastrado. */
public class LoginView {

    private final ApiClient api;
    private final Consumer<ApiClient.Usuario> aoEntrar;

    private final StackPane root = new StackPane();
    private final TextField cpf = Ui.mascaraCpf(Ui.entrada("000.000.000-00"));
    private final PasswordField senha = Ui.senha("Digite sua senha");
    private final Button entrar = new Button("ENTRAR");
    private final Label status = new Label();

    public LoginView(ApiClient api, Consumer<ApiClient.Usuario> aoEntrar, String avisoInicial) {
        this.api = api;
        this.aoEntrar = aoEntrar;
        montar();
        if (avisoInicial != null && !avisoInicial.isBlank()) {
            Ui.mensagem(status, avisoInicial, true);
        }
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

        status.setWrapText(true);
        status.getStyleClass().add("status-label");

        VBox card = new VBox(16, icone, marca, subtitulo,
                Ui.campo("CPF", cpf), Ui.campo("Senha", senha), entrar, status);
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
        Ui.mensagem(status, "Verificando...", false);

        Async.run("login", () -> api.login(cpfDigitado, senhaDigitada), usuario -> {
            entrar.setDisable(false);
            if (usuario == null) {
                senha.clear();
                Ui.mensagem(status, "CPF ou senha incorretos.", true);
                return;
            }
            if (!cargoPermitido(usuario.cargo())) {
                api.sair();
                senha.clear();
                Ui.mensagem(status, "Seu cargo (" + usuario.cargo() + ") não tem permissão para acessar o caixa.", true);
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

    /**
     * Só estes cargos podem entrar no sistema (comparação sem acento e sem diferença de maiúsculas).
     * "Administrador" é o usuário que a API cria quando o banco não tem nenhum funcionário.
     */
    private static final Set<String> CARGOS_PERMITIDOS = Set.of("operador de caixa", "gerente", "administrador");

    private static boolean cargoPermitido(String cargo) {
        String normalizado = Normalizer.normalize(cargo == null ? "" : cargo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase();
        return CARGOS_PERMITIDOS.contains(normalizado);
    }

    private static String apenasDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }
}
