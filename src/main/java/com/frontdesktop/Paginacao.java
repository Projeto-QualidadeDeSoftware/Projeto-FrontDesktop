package com.frontdesktop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Paginação das tabelas, com o rodapé "1–6 de 12 produtos" e "Página 1 de 2" como na web. */
public class Paginacao<T> {

    private final FilteredList<T> fonte;
    private final int tamanho;
    private final String plural;
    private final boolean estiloMostrando;
    private final ObservableList<T> visiveis = FXCollections.observableArrayList();
    private final Label resumo = new Label();
    private final Label indicador = new Label();
    private final Button anterior = Ui.botao("‹", "icon-button");
    private final Button proximo = Ui.botao("›", "icon-button");
    private final HBox rodape;
    private int pagina;

    /**
     * @param estiloMostrando true: "Mostrando 1 a 6 de 12 clientes" e "1 / 2"; false: "1–6 de 12 produtos" e "Página 1 de 2"
     */
    public Paginacao(FilteredList<T> fonte, int tamanho, String plural, boolean estiloMostrando) {
        this.fonte = fonte;
        this.tamanho = tamanho;
        this.plural = plural;
        this.estiloMostrando = estiloMostrando;

        resumo.getStyleClass().add("subtitle");
        indicador.getStyleClass().add("subtitle");
        anterior.setOnAction(event -> ir(pagina - 1));
        proximo.setOnAction(event -> ir(pagina + 1));

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        rodape = new HBox(8, resumo, espaco, anterior, indicador, proximo);
        rodape.setAlignment(Pos.CENTER_LEFT);
        rodape.setPadding(new Insets(4, 8, 0, 8));

        fonte.addListener((javafx.collections.ListChangeListener<T>) mudanca -> atualizar());
        atualizar();
    }

    public ObservableList<T> visiveis() {
        return visiveis;
    }

    public HBox rodape() {
        return rodape;
    }

    /** Volta para a primeira página (usar quando a busca mudar). */
    public void inicio() {
        pagina = 0;
        atualizar();
    }

    private void ir(int nova) {
        pagina = nova;
        atualizar();
    }

    private int totalPaginas() {
        return Math.max(1, (int) Math.ceil(fonte.size() / (double) tamanho));
    }

    private void atualizar() {
        int total = fonte.size();
        int paginas = totalPaginas();
        pagina = Math.max(0, Math.min(pagina, paginas - 1));
        int de = pagina * tamanho;
        int ate = Math.min(total, de + tamanho);
        visiveis.setAll(fonte.subList(de, ate));

        int primeiro = total == 0 ? 0 : de + 1;
        if (estiloMostrando) {
            resumo.setText("Mostrando " + primeiro + " a " + ate + " de " + total + " " + plural);
            indicador.setText((pagina + 1) + " / " + paginas);
        } else {
            resumo.setText(primeiro + "–" + ate + " de " + total + " " + plural);
            indicador.setText("Página " + (pagina + 1) + " de " + paginas);
        }
        anterior.setDisable(pagina == 0);
        proximo.setDisable(pagina >= paginas - 1);
    }
}
