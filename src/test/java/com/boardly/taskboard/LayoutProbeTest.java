package com.boardly.taskboard;

import com.boardly.taskboard.config.AppConfig;
import com.boardly.taskboard.controller.CardDialogController;
import com.boardly.taskboard.model.CardStatus;
import com.boardly.taskboard.service.MarkdownService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class LayoutProbeTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void probeLayout() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            try {
                CardDialogController controller = new CardDialogController(
                        null, 0L, null, CardStatus.PLANNED, null, null, new MarkdownService(), null);
                FXMLLoader loader = new FXMLLoader(
                        CardDialogController.class.getResource(AppConfig.CARD_DIALOG_FXML));
                loader.setController(controller);
                Parent root = loader.load();

                Scene scene = new Scene(root, AppConfig.CARD_DIALOG_WIDTH, AppConfig.CARD_DIALOG_HEIGHT);
                scene.getStylesheets().add(CardDialogController.class.getResource(AppConfig.APP_CSS).toExternalForm());
                scene.getStylesheets().add(CardDialogController.class.getResource(AppConfig.BOARD_CSS).toExternalForm());
                scene.getStylesheets().add(CardDialogController.class.getResource(AppConfig.CARD_CSS).toExternalForm());

                Stage stage = new Stage();
                stage.setScene(scene);
                stage.show();

                StringBuilder sb = new StringBuilder();
                BorderPane bp = (BorderPane) root;
                Region top = (Region) bp.getTop();
                Region center = (Region) bp.getCenter();
                Region bottom = (Region) bp.getBottom();
                sb.append("scene=").append(scene.getWidth()).append('x').append(scene.getHeight()).append('\n');
                sb.append("root=").append(root.getLayoutBounds()).append('\n');
                sb.append("top=").append(top.getLayoutBounds()).append(" prefH=").append(top.prefHeight(-1)).append('\n');
                sb.append("center=").append(center.getLayoutBounds()).append(" prefH=").append(center.prefHeight(-1)).append('\n');
                sb.append("bottom=").append(bottom.getLayoutBounds()).append(" prefH=").append(bottom.prefHeight(-1)).append('\n');
                HBox actions = (HBox) bottom;
                for (Node c : actions.getChildren()) {
                    if (c instanceof Button b) {
                        sb.append("button '").append(b.getText()).append("' bounds=").append(c.getLayoutBounds())
                                .append(" visible=").append(b.isVisible()).append('\n');
                    }
                }
                Files.writeString(Path.of("target/layout-probe.txt"), sb.toString());
                stage.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await(30, TimeUnit.SECONDS);
    }
}
