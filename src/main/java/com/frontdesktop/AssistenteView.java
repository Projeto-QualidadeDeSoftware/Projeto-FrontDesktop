package com.frontdesktop;

import com.frontdesktop.api.ApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Assistente de Estoque: perguntas em texto livre respondidas pela IA a partir do estoque atual. */
public class AssistenteView {

    private final ApiClient api;
    private final VBox root = new VBox(18);
    private final VBox conversa = new VBox(12);
    private final ScrollPane rolagem = new ScrollPane(conversa);
    private final TextField pergunta = Ui.entrada("Ex: Quais produtos estão com menos de 20 unidades?");
    private final Button enviar = Ui.botao("Perguntar", "primary-button");
    private boolean semPerguntas = true;

    public AssistenteView(ApiClient api) {
        this.api = api;
        montar();
    }

    public Node getView() {
        return root;
    }

    public void focar() {
        pergunta.requestFocus();
    }

    private void montar() {
        root.setPadding(Ui.padrao());
        root.getStyleClass().add("main-panel");

        Label titulo = new Label("Pergunte sobre o estoque");
        titulo.getStyleClass().add("section-title");
        Label dica = new Label("As respostas usam os produtos cadastrados no momento da pergunta.");
        dica.getStyleClass().add("cart-item-muted");
        HBox cabecalhoCard = new HBox(12, Ui.iconeCartao("💬"), new VBox(2, titulo, dica));
        cabecalhoCard.setAlignment(Pos.CENTER_LEFT);

        conversa.setPadding(new Insets(4));
        Label vazio = new Label("Nenhuma pergunta ainda. Experimente: \"O que está acabando?\"");
        vazio.getStyleClass().add("cart-item-muted");
        conversa.getChildren().add(vazio);

        rolagem.setFitToWidth(true);
        rolagem.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(rolagem, Priority.ALWAYS);

        pergunta.setOnAction(event -> perguntar());
        HBox.setHgrow(pergunta, Priority.ALWAYS);
        pergunta.setMaxWidth(Double.MAX_VALUE);
        enviar.setOnAction(event -> perguntar());
        enviar.setPadding(new Insets(10, 22, 10, 22));
        HBox entrada = new HBox(12, pergunta, enviar);
        entrada.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16, cabecalhoCard, rolagem, entrada);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22, 26, 22, 26));
        VBox.setVgrow(card, Priority.ALWAYS);

        root.getChildren().addAll(Ui.cabecalho("Assistente de Estoque"), card);
    }

    private void perguntar() {
        String texto = pergunta.getText() == null ? "" : pergunta.getText().trim();
        if (texto.isEmpty() || enviar.isDisabled()) {
            return;
        }
        if (texto.length() > 500) {
            if (semPerguntas) {
                conversa.getChildren().clear();
                semPerguntas = false;
            }
            adicionar("A pergunta pode ter no máximo 500 caracteres.", "status-error");
            return;
        }

        if (semPerguntas) {
            conversa.getChildren().clear();
            semPerguntas = false;
        }
        adicionarBalao("Você", texto, "cart-item-name");
        Label aguardando = adicionar("Consultando o estoque...", "cart-item-muted");

        pergunta.clear();
        enviar.setDisable(true);
        Async.run("assistente", () -> api.perguntarAoAssistente(texto), resposta -> {
            conversa.getChildren().remove(aguardando);
            adicionarBalao("Assistente", resposta, "cart-item-value");
            enviar.setDisable(false);
            pergunta.requestFocus();
        }, erro -> {
            conversa.getChildren().remove(aguardando);
            adicionar(erro.getMessage(), "status-error");
            enviar.setDisable(false);
        });
    }

    private void adicionarBalao(String autor, String texto, String estiloAutor) {
        Label quem = new Label(autor);
        quem.getStyleClass().add(estiloAutor);
        Label conteudo = new Label(texto);
        conteudo.setWrapText(true);
        conteudo.getStyleClass().add("cart-item-name");
        VBox balao = new VBox(4, quem, conteudo);
        balao.getStyleClass().add("cart-row");
        balao.setPadding(new Insets(8, 4, 8, 4));
        conversa.getChildren().add(balao);
        rolarParaOFim();
    }

    private Label adicionar(String texto, String estilo) {
        Label label = new Label(texto);
        label.setWrapText(true);
        label.getStyleClass().addAll("status-label", estilo);
        conversa.getChildren().add(label);
        rolarParaOFim();
        return label;
    }

    private void rolarParaOFim() {
        rolagem.layout();
        rolagem.setVvalue(1.0);
    }
}
