package com.java2exe;

import com.java2exe.controller.MainController;
import java.io.InputStream;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main entry point for the Java2Exe application.
 */
public class Main extends Application {

  private static final String[] MAIN_WINDOW_ICON_PATHS = {
    "/image/logo.png", "/resources/image/logo.png"
  };

  private static final int DEFAULT_WIDTH = 850;
  private static final int DEFAULT_HEIGHT = 650;

  @Override
  public void start(Stage primaryStage) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(
        (t, e) -> {
          System.err.printf("Uncaught exception in thread '%s': %s%n", t.getName(), e.getMessage());
          e.printStackTrace();
        });

    URL fxmlUrl = getClass().getResource("/view/main.fxml");
    if (fxmlUrl == null) {
      System.err.println("Critical Error: Cannot find FXML file at /view/main.fxml");
      return;
    }

    FXMLLoader loader = new FXMLLoader(fxmlUrl);
    loader.setClassLoader(Main.class.getClassLoader());

    Parent root = loader.load();
    MainController controller = loader.getController();
    Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

    URL cssUrl = getClass().getResource("/css/style.css");
    if (cssUrl != null) {
      scene.getStylesheets().add(cssUrl.toExternalForm());
    }

    primaryStage.setTitle("Java2Exe - Java Application Packager");
    loadApplicationIcons(primaryStage);

    primaryStage.setScene(scene);
    primaryStage.setMinWidth(DEFAULT_WIDTH);
    primaryStage.setMinHeight(DEFAULT_HEIGHT);

    primaryStage.setOnCloseRequest(e -> controller.shutdown());
    primaryStage.show();
  }

  private void loadApplicationIcons(Stage stage) {
    for (String iconPath : MAIN_WINDOW_ICON_PATHS) {
      try (InputStream iconStream = getClass().getResourceAsStream(iconPath)) {
        if (iconStream != null) {
          stage.getIcons().add(new Image(iconStream));
          return;
        }
      } catch (Exception e) {
        System.err.printf("Notice: Failed to load icon from %s: %s%n", iconPath, e.getMessage());
      }
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
