package com.eventportal.util;

import javafx.concurrent.Task;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class FxTaskRunner {
    private FxTaskRunner() {
    }

    public static <T> void run(
            String taskName,
            Callable<T> operation,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure
    ) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onFailure, "onFailure");

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(task.getException()));

        Thread thread = new Thread(task, taskName);
        thread.setDaemon(true);
        thread.start();
    }
}
