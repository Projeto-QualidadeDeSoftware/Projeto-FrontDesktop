package com.frontdesktop;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Executa trabalho fora da thread do JavaFX e devolve o resultado nela. */
public final class Async {

    private Async() {
    }

    public static <T> void run(String nome, Callable<T> trabalho, Consumer<T> aoConcluir,
                               Consumer<Throwable> aoFalhar) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabalho.call();
            }
        };
        task.setOnSucceeded(event -> aoConcluir.accept(task.getValue()));
        task.setOnFailed(event -> aoFalhar.accept(task.getException()));

        Thread thread = new Thread(task, nome);
        thread.setDaemon(true);
        thread.start();
    }
}
