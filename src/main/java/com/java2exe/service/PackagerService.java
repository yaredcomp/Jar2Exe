package com.java2exe.service;

import com.java2exe.model.ProjectConfig;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.imageio.ImageIO;

/**
 * Service class for packaging Java applications.
 */
public class PackagerService {

  private Consumer<String> logCallback;

  public void setLogCallback(Consumer<String> logCallback) {
    this.logCallback = logCallback;
  }

  private void log(String message) {
    if (logCallback != null) {
      logCallback.accept(message);
    }
  }

  public String detectMainClass(Path projectDir) throws IOException {
    log("Searching for main class in: " + projectDir);

    if (Files.isRegularFile(projectDir) && projectDir.toString().endsWith(".jar")) {
      try (JarFile jarFile = new JarFile(projectDir.toFile())) {
        Manifest manifest = jarFile.getManifest();
        if (manifest != null) {
          String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
          if (mainClass != null) return mainClass;
        }
      }
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(projectDir, "*.jar")) {
      for (Path jarPath : stream) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
          Manifest manifest = jarFile.getManifest();
          if (manifest != null) {
            String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (mainClass != null) return mainClass;
          }
        }
      }
    }

    try (var walk = Files.walk(projectDir)) {
      List<Path> javaFiles = walk.filter(p -> p.toString().endsWith(".java")).toList();
      for (Path javaFile : javaFiles) {
        String content = Files.readString(javaFile);
        if (content.contains("public static void main")) {
          String packageName = extractPackageName(content);
          String simpleName = javaFile.getFileName().toString().replace(".java", "");
          return (packageName != null && !packageName.isEmpty()) ? packageName + "." + simpleName : simpleName;
        }
      }
    }
    return null;
  }

  private String extractPackageName(String content) {
    for (String line : content.split("\n")) {
      line = line.trim();
      if (line.startsWith("package ")) return line.substring(8).replace(";", "").trim();
    }
    return null;
  }

  public boolean validateProject(ProjectConfig config) {
    try {
      Path projectPath = Paths.get(config.getProjectPath());
      if (!Files.exists(projectPath)) return false;
      log("Project validation completed.");
      return true;
    } catch (Exception e) {
      log("Validation error: " + e.getMessage());
      return false;
    }
  }

  public void buildPackage(ProjectConfig config) throws Exception {
    log("Starting build process...");
    Path outputDir = Paths.get(config.getOutputDir());
    Files.createDirectories(outputDir);

    Path inputPath = Paths.get(config.getProjectPath());
    if (inputPath.toString().endsWith(".jar")) {
      buildFromJar(config, inputPath, outputDir);
    } else {
      buildFromProject(config, inputPath, outputDir);
    }
  }

  private void buildFromJar(ProjectConfig config, Path jarPath, Path outputDir) throws Exception {
    List<String> args = new ArrayList<>();
    args.add("jpackage");
    args.add("--input"); args.add(jarPath.getParent().toString());
    args.add("--main-jar"); args.add(jarPath.getFileName().toString());
    if (config.getMainClass() != null && !config.getMainClass().isBlank()) {
      args.add("--main-class"); args.add(config.getMainClass());
    }
    args.add("--name"); args.add(config.getAppName());
    args.add("--dest"); args.add(outputDir.toString());

    executeProcess(new ProcessBuilder(args), "jpackage");
  }

  private void buildFromProject(ProjectConfig config, Path projectDir, Path outputDir) throws Exception {
    // Basic build system implementation
    String cmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
    executeProcess(new ProcessBuilder(cmd, "clean", "package", "-DskipTests").directory(projectDir.toFile()), "Maven");
    
    // Simplification for brevity
    log("Build complete.");
  }

  private void executeProcess(ProcessBuilder pb, String name) throws Exception {
    pb.redirectErrorStream(true);
    Process process = pb.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) log("[" + name + "] " + line);
    }
    if (process.waitFor() != 0) throw new RuntimeException(name + " failed.");
  }
}
