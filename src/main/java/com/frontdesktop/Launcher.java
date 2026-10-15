package com.frontdesktop;

import javafx.application.Application;

/**
 * Ponto de entrada do jar empacotado. Fica separado do MainApp porque uma classe que
 * estende Application não inicia a partir do classpath sem o módulo JavaFX.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
