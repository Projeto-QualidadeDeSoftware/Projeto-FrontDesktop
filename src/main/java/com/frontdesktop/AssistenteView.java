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

/** Assistente IA: perguntas em texto livre respondidas pela IA a partir do estoque atual. */
public class AssistenteView {

    private static final int LIMITE = 500;
    private static final String[] SUGESTOES = {
            "O que está acabando?",
            "Quais produtos estão com menos de 20 unidades?",
            "Qual o produto mais caro do estoque?"};

    private final ApiClient api;
    private final VBox root = new VBox(18);
    private final VBox conversa = new VBox(12);
    private final ScrollPane rolagem = new ScrollPane(conversa);
    private final TextField pergunta = Ui.entrada("Ex: Quais produtos estão com menos de 20 unidades?");
    private final Button enviar = Ui.botao("➤  Perguntar", "primary-button");
    private VBox vazio;
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

        HBox cabecalhoCard = new HBox(12, Ui.iconeCartao("💬"),
                Ui.tituloCartao("Pergunte sobre o estoque", "Até " + LIMITE + " caracteres por pergunta"));
        cabecalhoCard.setAlignment(Pos.CENTER_LEFT);

        conversa.setPadding(new Insets(4));
        Label dica = new Label("Nenhuma pergunta ainda. Experimente:");
        dica.getStyleClass().add("subtitle");
        HBox sugestoes = new HBox(24);
        sugestoes.setAlignment(Pos.CENTER);
        for (String texto : SUGESTOES) {
            Button sugestao = Ui.botao(texto, "suggestion");
            sugestao.setOnAction(event -> {
                pergunta.setText(texto);
                perguntar();
            });
            sugestoes.getChildren().add(sugestao);
        }
        vazio = new VBox(14, dica, sugestoes);
        vazio.setAlignment(Pos.CENTER);
        vazio.setPadding(new Insets(60, 0, 0, 0));
        conversa.getChildren().add(vazio);

        rolagem.setFitToWidth(true);
        rolagem.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(rolagem, Priority.ALWAYS);

        pergunta.setOnAction(event -> perguntar());
        pergunta.getStyleClass().add("assistant-input");
        HBox.setHgrow(pergunta, Priority.ALWAYS);
        pergunta.setMaxWidth(Double.MAX_VALUE);
        enviar.setOnAction(event -> perguntar());
        HBox entrada = new HBox(12, pergunta, enviar);
        entrada.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16, cabecalhoCard, rolagem, entrada);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22, 26, 22, 26));
        VBox.setVgrow(card, Priority.ALWAYS);

        root.getChildren().addAll(Ui.cabecalho("Inteligência artificial", "Assistente de Estoque",
                "As respostas usam os produtos cadastrados no momento da pergunta."), card);
    }

    private void esconderSugestoes() {
        if (semPerguntas) {
            conversa.getChildren().remove(vazio);
            semPerguntas = false;
        }
    }

    private void perguntar() {
        String texto = pergunta.getText() == null ? "" : pergunta.getText().trim();
        if (texto.isEmpty() || enviar.isDisabled()) {
            return;
        }
        if (texto.length() > LIMITE) {
            esconderSugestoes();
            adicionar("A pergunta pode ter no máximo " + LIMITE + " caracteres.", "status-error");
            return;
        }

        esconderSugestoes();
        adicionarBalao("Você", texto, "bubble-user");
        Label aguardando = adicionar("Consultando o estoque...", "cart-item-muted");

        pergunta.clear();
        enviar.setDisable(true);
        Async.run("assistente", () -> api.perguntarAoAssistente(texto), resposta -> {
            conversa.getChildren().remove(aguardando);
            adicionarBalao("Assistente", resposta, "bubble-bot");
            enviar.setDisable(false);
            pergunta.requestFocus();
        }, erro -> {
            conversa.getChildren().remove(aguardando);
            adicionar(Ui.mensagemDe(erro), "status-error");
            enviar.setDisable(false);
        });
    }

    private void adicionarBalao(String autor, String texto, String estiloBalao) {
        Label quem = new Label(autor);
        quem.getStyleClass().add("cart-item-value");
        Label conteudo = new Label(texto);
        conteudo.setWrapText(true);
        conteudo.getStyleClass().add("cart-item-name");
        VBox balao = new VBox(4, quem, conteudo);
        balao.getStyleClass().add(estiloBalao);
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
