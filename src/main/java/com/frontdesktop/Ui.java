package com.frontdesktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Locale;

/** Pequenos construtores de componentes e formatadores compartilhados pelas telas. */
public final class Ui {

    private Ui() {
    }

    public static VBox cabecalho(String titulo) {
        Label eyebrow = new Label("PRONTÊ CONVENIÊNCIA");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label(titulo);
        title.getStyleClass().add("title");
        return new VBox(2, eyebrow, title);
    }

    public static VBox campo(String rotulo, TextField entrada) {
        Label label = new Label(rotulo.toUpperCase());
        label.getStyleClass().add("campo-label");
        entrada.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, label, entrada);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    public static VBox campo(String rotulo, javafx.scene.control.Control controle) {
        Label label = new Label(rotulo.toUpperCase());
        label.getStyleClass().add("campo-label");
        controle.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, label, controle);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    public static TextField entrada(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        return field;
    }

    public static PasswordField senha(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        return field;
    }

    public static StackPane iconeCartao(String glyph) {
        Label label = new Label(glyph);
        StackPane chip = new StackPane(label);
        chip.getStyleClass().add("icon-chip");
        return chip;
    }

    public static Button botao(String texto, String estilo) {
        Button button = new Button(texto);
        button.getStyleClass().add(estilo);
        return button;
    }

    public static Button botaoIcone(String glyph, boolean perigo, Runnable acao) {
        Button button = new Button(glyph);
        button.getStyleClass().add("icon-button");
        if (perigo) {
            button.getStyleClass().add("icon-button-danger");
        }
        button.setOnAction(event -> acao.run());
        return button;
    }

    public static void mensagem(Label status, String texto, boolean erro) {
        status.setText(texto);
        status.getStyleClass().remove("status-error");
        if (erro) {
            status.getStyleClass().add("status-error");
        }
    }

    public static boolean confirmar(String texto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public static String dinheiro(double valor) {
        return String.format(new Locale("pt", "BR"), "%,.2f", valor);
    }

    public static double lerDecimal(String texto) {
        String limpo = texto.trim().replace("R$", "").trim();
        if (limpo.contains(",")) {
            // formato brasileiro: ponto é milhar, vírgula é decimal
            limpo = limpo.replace(".", "").replace(',', '.');
        }
        return Double.parseDouble(limpo);
    }

    public static String codigoProduto(long id) {
        return String.format("P%03d", id);
    }

    public static Insets padrao() {
        return new Insets(28, 30, 24, 30);
    }

    public static Pos topoCentro() {
        return Pos.TOP_CENTER;
    }
}
