package com.frontdesktop;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.Locale;
import java.util.function.Function;

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

    /** Cabeçalho no estilo da web: chamada laranja, título e uma linha de descrição. */
    public static VBox cabecalho(String chamada, String titulo, String descricao) {
        Label eyebrow = new Label(chamada);
        eyebrow.getStyleClass().add("eyebrow-orange");
        Label title = new Label(titulo);
        title.getStyleClass().add("title");
        Label sub = new Label(descricao);
        sub.getStyleClass().add("subtitle");
        return new VBox(2, eyebrow, title, sub);
    }

    /** Cabeçalho à esquerda e ações (botões) à direita, alinhados ao pé do título. */
    public static HBox topo(VBox cabecalho, Node... acoes) {
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linha = new HBox(12, cabecalho, espaco);
        linha.getChildren().addAll(acoes);
        linha.setAlignment(Pos.CENTER_LEFT);
        return linha;
    }

    /** Cartão de número do topo das telas (título cinza em cima, valor grande embaixo). */
    public static VBox indicador(String titulo, Label valor, boolean destaque) {
        Label rotulo = new Label(titulo);
        rotulo.getStyleClass().add("stat-title");
        valor.getStyleClass().add("stat-value");
        if (destaque) {
            valor.getStyleClass().add("stat-orange");
        }
        VBox caixa = new VBox(6, rotulo, valor);
        caixa.getStyleClass().add("card");
        caixa.setPadding(new Insets(18, 22, 18, 22));
        HBox.setHgrow(caixa, Priority.ALWAYS);
        caixa.setMaxWidth(Double.MAX_VALUE);
        return caixa;
    }

    /** Título e subtítulo de um cartão (ex.: "Lista de clientes" + descrição). */
    public static VBox tituloCartao(String titulo, String descricao) {
        Label t = new Label(titulo);
        t.getStyleClass().add("section-title");
        VBox box = new VBox(2, t);
        if (descricao != null) {
            Label d = new Label(descricao);
            d.getStyleClass().add("subtitle");
            box.getChildren().add(d);
        }
        return box;
    }

    /** Círculo com a inicial do nome, usado nas listas de pessoas. */
    public static StackPane avatar(String nome) {
        String inicial = nome == null || nome.isBlank() ? "?" : nome.trim().substring(0, 1).toUpperCase();
        Label letra = new Label(inicial);
        letra.getStyleClass().add("avatar-letra");
        StackPane circulo = new StackPane(letra);
        circulo.getStyleClass().add("avatar");
        return circulo;
    }

    /** Janela modal com o cartão pronto; quem chama usa showAndWait(). */
    public static Stage modal(Node dono, String titulo, Region card) {
        Stage janela = new Stage();
        janela.initModality(Modality.APPLICATION_MODAL);
        janela.initOwner(dono.getScene().getWindow());
        janela.setTitle(titulo);
        card.getStyleClass().addAll("card", "main-panel");
        Scene scene = new Scene(card);
        scene.getStylesheets().addAll(dono.getScene().getStylesheets());
        janela.setScene(scene);
        janela.setResizable(false);
        return janela;
    }

    /** Logo da Prontê (mesma imagem da web) na largura pedida. */
    public static ImageView logo(double largura) {
        ImageView view = new ImageView(new Image(Ui.class.getResource("/img/logo.png").toExternalForm()));
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(largura);
        return view;
    }

    public static final String ESQUERDA = "CENTER-LEFT";
    public static final String CENTRO = "CENTER";
    public static final String DIREITA = "CENTER-RIGHT";

    /** Título da coluna alinhado do mesmo lado que o conteúdo das células. */
    public static void cabecalhoColuna(TableColumn<?, ?> coluna, String titulo, String alinhamento) {
        Label label = new Label(titulo);
        label.setStyle("-fx-alignment: " + alinhamento + ";");
        label.setMaxWidth(Double.MAX_VALUE);
        label.prefWidthProperty().bind(Bindings.max(0, coluna.widthProperty().subtract(18)));
        coluna.setText("");
        coluna.setGraphic(label);
    }

    /** Coluna de texto simples com o estilo de célula e o alinhamento pedidos. */
    public static <T> TableColumn<T, String> colunaTexto(String titulo, Function<T, String> valor, double peso,
                                                         String estilo, String alinhamento) {
        TableColumn<T, String> coluna = new TableColumn<>();
        cabecalhoColuna(coluna, titulo, alinhamento);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        coluna.setPrefWidth(peso * 100);
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                alinharCelula(this, alinhamento);
                setText(vazio ? null : item);
                getStyleClass().removeAll("cell-id", "cell-strong", "cell-muted", "cell-mono");
                if (!vazio) {
                    getStyleClass().add(estilo);
                }
            }
        });
        return coluna;
    }

    /** Coluna cuja célula é um componente montado a partir do registro (avatar, botões, etiquetas). */
    public static <T> TableColumn<T, T> colunaNo(String titulo, double peso, String alinhamento,
                                                 Function<T, Node> construtor) {
        TableColumn<T, T> coluna = new TableColumn<>();
        cabecalhoColuna(coluna, titulo, alinhamento);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        coluna.setPrefWidth(peso * 100);
        coluna.setSortable(false);
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T registro, boolean vazio) {
                super.updateItem(registro, vazio);
                alinharCelula(this, alinhamento);
                setGraphic(vazio || registro == null ? null : construtor.apply(registro));
            }
        });
        return coluna;
    }

    /** "Editar" e "Excluir" em texto, como na web. */
    public static HBox acoesTexto(Runnable editar, Runnable excluir) {
        Button e = botao("Editar", "link-edit");
        e.setOnAction(event -> editar.run());
        Button x = botao("Excluir", "link-delete");
        x.setOnAction(event -> excluir.run());
        HBox caixa = new HBox(10, e, x);
        caixa.setAlignment(Pos.CENTER_RIGHT);
        return caixa;
    }

    /** Alinhamento do conteúdo da célula (estilo inline vence o CSS da aplicação). */
    public static void alinharCelula(TableCell<?, ?> celula, String alinhamento) {
        celula.setStyle("-fx-alignment: " + alinhamento + ";");
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

    /** Texto do erro para o usuário; falhas de conexão chegam sem mensagem. */
    public static String mensagemDe(Throwable erro) {
        String texto = erro == null ? null : erro.getMessage();
        if (texto == null || texto.isBlank()) {
            return "API indisponível, verifique a conexão.";
        }
        if (texto.contains("foreign key") || texto.contains("could not execute statement")) {
            return "Não é possível excluir: este registro já está em uso (possui vendas).";
        }
        return texto;
    }

    /** CPF com 11 dígitos e dígitos verificadores corretos (rejeita 111.111.111-11 e similares). */
    public static boolean cpfValido(String texto) {
        String d = texto == null ? "" : texto.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        for (int t = 9; t < 11; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) {
                soma += (d.charAt(i) - '0') * (t + 1 - i);
            }
            int dv = (soma * 10) % 11 % 10;
            if (dv != d.charAt(t) - '0') {
                return false;
            }
        }
        return true;
    }

    public static boolean emailValido(String texto) {
        return texto != null && texto.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    public static boolean confirmar(String texto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /** Formata enquanto digita: só números, no máximo 11, no padrão 000.000.000-00. */
    public static TextField mascaraCpf(TextField campo) {
        campo.textProperty().addListener((obs, antigo, novo) -> {
            String formatado = formatarCpf(novo);
            if (!formatado.equals(novo)) {
                campo.setText(formatado);
                campo.positionCaret(formatado.length());
            }
        });
        return campo;
    }

    public static String formatarCpf(String texto) {
        String digitos = texto == null ? "" : texto.replaceAll("\\D", "");
        if (digitos.length() > 11) {
            digitos = digitos.substring(0, 11);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i == 3 || i == 6) {
                sb.append('.');
            } else if (i == 9) {
                sb.append('-');
            }
            sb.append(digitos.charAt(i));
        }
        return sb.toString();
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
