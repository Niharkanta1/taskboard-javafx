package com.example.taskboard.controller;

import com.example.taskboard.config.AppConfig;
import com.example.taskboard.exception.AppException;
import com.example.taskboard.model.Workspace;
import com.example.taskboard.session.SessionManager;
import com.example.taskboard.service.NavigationService;
import com.example.taskboard.service.WorkspaceService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

/**
 * Dashboard: shows the signed-in user and manages workspaces.
 *
 * <p>Workspace business rules live in {@link WorkspaceService}; this
 * controller only handles the view and user interaction. Styling is
 * in CSS.</p>
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final SessionManager sessionManager;
    private final WorkspaceService workspaceService;
    private final NavigationService navigationService;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label emptyHint;

    @FXML
    private ListView<Workspace> workspaceList;

    @FXML
    private Button openButton;

    @FXML
    private Button createButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button logoutButton;

    public DashboardController(SessionManager sessionManager,
                              WorkspaceService workspaceService,
                              NavigationService navigationService) {
        this.sessionManager = sessionManager;
        this.workspaceService = workspaceService;
        this.navigationService = navigationService;
    }

    @FXML
    private void initialize() {
        sessionManager.getCurrentUser().ifPresent(user ->
                welcomeLabel.setText("Logged in as: " + user.getUsername()));
        workspaceList.setCellFactory(lv -> workspaceCellFactory());
        refreshWorkspaces();
    }

    @FXML
    private void onLogout() {
        sessionManager.logout();
        navigationService.showLogin();
    }

    @FXML
    private void onOpenWorkspace() {
        Workspace selected = workspaceList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            navigationService.showWorkspace(selected);
        }
    }

    @FXML
    private void onCreateWorkspace() {
        showWorkspaceDialog(null);
    }

    @FXML
    private void onEditWorkspace() {
        Workspace selected = workspaceList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showWorkspaceDialog(selected);
        }
    }

    @FXML
    private void onDeleteWorkspace() {
        Workspace selected = workspaceList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean confirmed = DeleteDialogs.confirmDelete(
                (Stage) workspaceList.getScene().getWindow(),
                "Delete Workspace",
                "Delete workspace \"" + selected.getName() + "\"?\n"
                        + "This will also delete all boards and cards in this workspace.");
        if (!confirmed) {
            return;
        }

        try {
            workspaceService.delete(selected.getId());
            refreshWorkspaces();
        } catch (AppException e) {
            logger.error("Failed to delete workspace", e);
            showFailure("Unable to delete the workspace. Please try again.");
        }
    }

    private void refreshWorkspaces() {
        List<Workspace> workspaces;
        try {
            workspaces = workspaceService.findAll();
        } catch (AppException e) {
            logger.error("Failed to load workspaces", e);
            workspaces = List.of();
        }
        workspaceList.getItems().setAll(workspaces);
        emptyHint.setVisible(workspaceList.getItems().isEmpty());
    }

    private void showWorkspaceDialog(Workspace existing) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.WORKSPACE_DIALOG_FXML));
            Stage dialogStage = new Stage();
            WorkspaceDialogController controller = new WorkspaceDialogController(
                    workspaceService, existing, this::refreshWorkspaces, dialogStage);
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root, AppConfig.WORKSPACE_DIALOG_WIDTH, AppConfig.WORKSPACE_DIALOG_HEIGHT);
            scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
            scene.getStylesheets().add(resolveResource(AppConfig.DASHBOARD_CSS).toExternalForm());
            dialogStage.setTitle(existing == null ? "Create Workspace" : "Edit Workspace");
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(navigationService.getStage());
            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open workspace dialog", e);
            showFailure("Unable to open the workspace dialog. Please try again.");
        }
    }

    private void showFailure(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("TaskBoard");
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    private ListCell<Workspace> workspaceCellFactory() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Workspace workspace, boolean empty) {
                super.updateItem(workspace, empty);
                if (empty || workspace == null) {
                    setGraphic(null);
                    setText("");
                } else {
                    Label nameLabel = new Label(workspace.getName());
                    nameLabel.getStyleClass().add("workspace-item-name");
                    VBox box = new VBox(2, nameLabel);
                    String description = workspace.getDescription();
                    if (description != null && !description.isBlank()) {
                        Label descriptionLabel = new Label(description);
                        descriptionLabel.getStyleClass().add("workspace-item-description");
                        descriptionLabel.setWrapText(true);
                        box.getChildren().add(descriptionLabel);
                    }
                    setGraphic(box);
                    setText("");
                    setOnMouseClicked(event -> {
                        if (!isEmpty() && workspace != null && event.getClickCount() == 2) {
                            navigationService.showWorkspace(workspace);
                        }
                    });
                }
            }
        };
    }

    private URL resolveResource(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Required resource not found on classpath: " + resourcePath);
        }
        return url;
    }
}
