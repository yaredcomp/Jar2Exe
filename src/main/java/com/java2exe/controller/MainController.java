package com.java2exe.controller;

import com.java2exe.model.ProjectConfig;
import com.java2exe.service.PackagerService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for Java2Exe main application.
 */
public class MainController {

  private static final String APP_LOGO_CLASSPATH = "/image/logo.png";
  private static final String DEFAULT_EULA_TEMPLATE =
      """
      END USER LICENSE AGREEMENT (EULA) for {APP_NAME} v{VERSION}
      Developed by: {VENDOR}

      Please read this End User License Agreement carefully before using the software.

      1. License Grant
      The author ({VENDOR}) grants you a revocable, non-exclusive, non-transferable, limited \
      license to use the application strictly in accordance with the terms of this Agreement.

      2. Restrictions
      You agree not to, and you will not permit others to:
      - License, sell, rent, lease, assign, distribute, host, or otherwise commercially exploit the application.
      - Modify, make derivative works of, disassemble, decrypt, reverse compile or reverse engineer any part of the application.

      3. Termination
      This Agreement shall remain in effect until terminated by you or {VENDOR}.

      4. Disclaimer of Warranty
      The application is provided to you "AS IS" and "AS AVAILABLE" and with all faults and defects \
      without warranty of any kind.

      5. Limitation of Liability
      In no event shall {VENDOR} be liable for any special, incidental, indirect, or consequential \
      damages whatsoever.

      By installing or using this application, you agree to be bound by the terms and conditions \
      of this Agreement.""";

  @FXML private Button themeToggle;
  @FXML private FontIcon themeIcon;
  @FXML private ImageView appLogoView;
  @FXML private Circle statusDot;
  @FXML private ProgressIndicator busyIndicator;
  @FXML private VBox section1Card;
  @FXML private Label flowStepSource;
  @FXML private Label flowStepIdentity;
  @FXML private Label flowStepOutput;
  @FXML private VBox section2Card;
  @FXML private VBox section2Content;
  @FXML private Button section2Toggle;
  @FXML private VBox section3Card;
  @FXML private VBox section3Content;
  @FXML private Button section3Toggle;

  @FXML private TextField projectPathField;
  @FXML private TextField mainClassField;
  @FXML private TextField appNameField;
  @FXML private TextField versionField;
  @FXML private TextField vendorField;
  @FXML private TextField iconPathField;
  @FXML private TextArea licenseFileField;
  @FXML private TextField jvmOptionsArea;
  @FXML private TextField outputDirField;
  @FXML private CheckBox includeJRECheck;

  @FXML private ToggleGroup packageTypeGroup;
  @FXML private Label detectedOsLabel;
  @FXML private TextField antBuildTargetField;
  @FXML private CheckBox desktopShortcutCheck;
  @FXML private CheckBox startMenuCheck;
  @FXML private CheckBox startupCheck;
  @FXML private CheckBox debugSymbolsCheck;

  @FXML private TextArea logArea;
  @FXML private ProgressBar progressBar;
  @FXML private Label statusLabel;
  @FXML private Label validationMessageLabel;

  @FXML private Button browseButton;
  @FXML private Button iconBrowseButton;
  @FXML private Button licenseBrowseButton;
  @FXML private Button outputBrowseButton;
  @FXML private Button validateButton;
  @FXML private Button buildButton;
  @FXML private Button cancelButton;
  @FXML private Button step1NextButton;
  @FXML private Button step2BackButton;
  @FXML private Button step2NextButton;
  @FXML private Button step3BackButton;

  private final PackagerService packagerService = new PackagerService();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final BooleanProperty taskRunning = new SimpleBooleanProperty(false);
  private volatile Future<?> currentTask;
  private ScaleTransition busyPulse;
  private BooleanBinding requiredInputsValid;
  private boolean isDarkTheme = false;
  private int activeStep = 1;

  @FXML
  public void initialize() {
    outputDirField.setText(System.getProperty("user.home") + File.separator + "Java2ExeOutput");
    packagerService.setLogCallback(this::logMessage);
    initHeaderLogo();
    configureAnimations();
    setupValidationBindings();
    setupLicenseBinding();
    showActiveStep();
  }

  private void initHeaderLogo() {
    if (appLogoView == null) return;
    try (InputStream is = getClass().getResourceAsStream(APP_LOGO_CLASSPATH)) {
      if (is != null) {
        appLogoView.setImage(new Image(is));
        double width = appLogoView.getFitWidth();
        double height = appLogoView.getFitHeight();
        double radius = Math.min(width, height) / 2;
        Circle clip = new Circle(width / 2, height / 2, radius);
        appLogoView.setClip(clip);
      }
    } catch (IOException ignored) {}
  }

  private void setupLicenseBinding() {
    StringBinding licenseBinding = Bindings.createStringBinding(() -> {
      String name = appNameField.getText().isBlank() ? "[App Name]" : appNameField.getText();
      String version = versionField.getText().isBlank() ? "1.0.0" : versionField.getText();
      String vendor = vendorField.getText().isBlank() ? "[Company Name]" : vendorField.getText();

      return DEFAULT_EULA_TEMPLATE
          .replace("{APP_NAME}", name)
          .replace("{VERSION}", version)
          .replace("{VENDOR}", vendor);
    }, appNameField.textProperty(), versionField.textProperty(), vendorField.textProperty());

    licenseFileField.textProperty().bind(licenseBinding);
  }

  private void setupValidationBindings() {
    requiredInputsValid = Bindings.createBooleanBinding(
      () -> !isBlank(projectPathField) && !isBlank(mainClassField) && !isBlank(appNameField),
      projectPathField.textProperty(), mainClassField.textProperty(), appNameField.textProperty());
    validateButton.disableProperty().bind(requiredInputsValid.not().or(taskRunning));
    buildButton.disableProperty().bind(requiredInputsValid.not().or(taskRunning));
  }

  private boolean isBlank(TextField f) { return f.getText() == null || f.getText().isBlank(); }

  @FXML
  private void handleBrowse() {
    DirectoryChooser dc = new DirectoryChooser();
    File dir = dc.showDialog(projectPathField.getScene().getWindow());
    if (dir != null) {
      projectPathField.setText(dir.getAbsolutePath());
      triggerAutoDetect(dir.toPath());
    }
  }

  private void triggerAutoDetect(java.nio.file.Path path) {
    executor.submit(() -> {
      try {
        String main = packagerService.detectMainClass(path);
        Platform.runLater(() -> { if (main != null) mainClassField.setText(main); });
      } catch (IOException ignored) {}
    });
  }

  @FXML
  private void handleBuild() {
    ProjectConfig config = createConfigFromUI();
    taskRunning.set(true);
    startBusyMode("Building Package...");
    executor.submit(() -> {
      try {
        packagerService.buildPackage(config);
        Platform.runLater(() -> { stopBusyMode("Success", 1); showAlert("Build", "Complete!"); });
      } catch (Exception e) {
        Platform.runLater(() -> { stopBusyMode("Failed", 0); showAlert("Error", e.getMessage()); });
      } finally { Platform.runLater(() -> taskRunning.set(false)); }
    });
  }

  private ProjectConfig createConfigFromUI() {
    ProjectConfig c = new ProjectConfig();
    c.setProjectPath(projectPathField.getText());
    c.setMainClass(mainClassField.getText());
    c.setAppName(appNameField.getText());
    c.setVersion(versionField.getText());
    c.setVendor(vendorField.getText());
    c.setOutputDir(outputDirField.getText());
    return c;
  }

  private void showActiveStep() {
    section1Card.setVisible(activeStep == 1); section1Card.setManaged(activeStep == 1);
    section2Card.setVisible(activeStep == 2); section2Card.setManaged(activeStep == 2);
    section3Card.setVisible(activeStep == 3); section3Card.setManaged(activeStep == 3);
  }

  @FXML private void handleStep1Next() { activeStep = 2; showActiveStep(); }
  @FXML private void handleStep2Back() { activeStep = 1; showActiveStep(); }
  @FXML private void handleStep2Next() { activeStep = 3; showActiveStep(); }
  @FXML private void handleStep3Back() { activeStep = 2; showActiveStep(); }

  @FXML private void handleToggleSection2() { }
  @FXML private void handleToggleSection3() { }

  @FXML
  private void toggleTheme() {
    isDarkTheme = !isDarkTheme;
    Parent root = themeToggle.getScene().getRoot();
    if (isDarkTheme) {
      if (!root.getStyleClass().contains("dark")) {
        root.getStyleClass().add("dark");
      }
    } else {
      root.getStyleClass().remove("dark");
    }
    themeIcon.setIconLiteral(isDarkTheme ? "fas-sun" : "fas-moon");
  }

  @FXML 
  private void handleIconBrowse() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Select Application Icon");
    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ICO Files", "*.ico"));
    File file = fc.showOpenDialog(iconPathField.getScene().getWindow());
    if (file != null) {
      iconPathField.setText(file.getAbsolutePath());
    }
  }
  
  @FXML 
  private void handleLicenseBrowse() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Select License File");
    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
    File file = fc.showOpenDialog(licenseFileField.getScene().getWindow());
    if (file != null) {
      try {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        licenseFileField.textProperty().unbind();
        licenseFileField.setText(content);
      } catch (IOException e) {
        logMessage("Error reading license file: " + e.getMessage());
      }
    }
  }

  @FXML private void handleOutputBrowse() { }
  @FXML private void handleValidate() { }
  @FXML private void handleCancel() { }

  private void logMessage(String msg) { Platform.runLater(() -> logArea.appendText(msg + "\n")); }
  private void showAlert(String title, String content) {
    Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setContentText(content); a.show();
  }

  private void configureAnimations() {
    busyPulse = new ScaleTransition(Duration.millis(800), statusDot);
    busyPulse.setFromX(1.0); busyPulse.setToX(1.3); busyPulse.setCycleCount(Animation.INDEFINITE); busyPulse.setAutoReverse(true);
  }

  private void startBusyMode(String msg) { statusLabel.setText(msg); busyPulse.play(); }
  private void stopBusyMode(String msg, double p) { statusLabel.setText(msg); busyPulse.stop(); progressBar.setProgress(p); }

  public void shutdown() { executor.shutdownNow(); }
}
