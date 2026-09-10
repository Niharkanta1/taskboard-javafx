package com.example.taskboard.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/** Logs unexpected failures and presents one generic user-safe error dialog. */
public final class GlobalErrorHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalErrorHandler.class);
    private static final AtomicBoolean dialogVisible = new AtomicBoolean();

    private GlobalErrorHandler() {
    }

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalErrorHandler());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        logger.error("Unexpected application failure on thread {}", thread.getName(), error);
        showFailureDialog();
    }

    private static void showFailureDialog() {
        Runnable show = () -> {
            if (!dialogVisible.compareAndSet(false, true)) {
                return;
            }
            try {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("TaskBoard");
                alert.setHeaderText("TaskBoard encountered an unexpected error.");
                alert.setContentText("Please try again. If the problem continues, review the application log.");
                alert.showAndWait();
            } finally {
                dialogVisible.set(false);
            }
        };
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }
}
