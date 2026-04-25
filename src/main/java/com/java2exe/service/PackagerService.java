package com.java2exe.service;

import com.java2exe.model.ProjectConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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

    Path targetDir = projectDir.resolve("target");
    if (Files.exists(targetDir)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, "*.jar")) {
        for (Path jarPath : stream) {
          try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
              String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
              if (mainClass != null && !mainClass.contains("org.springframework.boot.loader")) {
                  return mainClass;
              }
            }
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

  public void buildPackage(ProjectConfig config) throws Exception {
    log("Starting build process...");
    Path outputDir = Paths.get(config.getOutputDir());
    Files.createDirectories(outputDir);

    Path inputPath = Paths.get(config.getProjectPath());
    if (Files.isRegularFile(inputPath) && inputPath.toString().endsWith(".jar")) {
      runJPackage(config, inputPath.getParent(), inputPath.getFileName().toString(), outputDir);
    } else {
      buildFromProject(config, inputPath, outputDir);
    }
  }

  private void buildFromProject(ProjectConfig config, Path projectDir, Path outputDir) throws Exception {
    log("Building project with Maven...");
    String cmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
    executeProcess(new ProcessBuilder(cmd, "clean", "package", "-DskipTests").directory(projectDir.toFile()), "Maven");
    
    Path targetDir = projectDir.resolve("target");
    Path mainJar = null;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, "*.jar")) {
      for (Path jarPath : stream) {
        if (!jarPath.getFileName().toString().contains("sources") && !jarPath.getFileName().toString().contains("javadoc")) {
          mainJar = jarPath;
          break;
        }
      }
    }

    if (mainJar == null) {
      throw new RuntimeException("Could not find generated JAR file in target directory.");
    }

    log("Found JAR: " + mainJar.getFileName());
    runJPackage(config, targetDir, mainJar.getFileName().toString(), outputDir);
  }

  private void runJPackage(ProjectConfig config, Path inputDir, String mainJar, Path outputDir) throws Exception {
    log("Running jpackage...");
    List<String> args = new ArrayList<>();
    args.add("jpackage");
    args.add("--input"); args.add(inputDir.toString());
    args.add("--main-jar"); args.add(mainJar);
    
    if (config.getMainClass() != null && !config.getMainClass().isBlank()) {
      args.add("--main-class"); args.add(config.getMainClass());
    }
    
    args.add("--name"); args.add(config.getAppName());
    args.add("--app-version"); args.add(config.getVersion() != null && !config.getVersion().isBlank() ? config.getVersion() : "1.0.0");
    args.add("--vendor"); args.add(config.getVendor() != null && !config.getVendor().isBlank() ? config.getVendor() : "Unknown");
    args.add("--dest"); args.add(outputDir.toString());
    
    if (config.getJvmOptions() != null && !config.getJvmOptions().isBlank()) {
        for (String option : config.getJvmOptions().split("\\s+")) {
            if (!option.isBlank()) {
                args.add("--java-options");
                args.add(option);
            }
        }
    }

    // Handle License File for Installer
    if (config.getLicenseFile() != null && !config.getLicenseFile().isBlank()) {
        Path tempLicense = Files.createTempFile("license", ".txt");
        Files.writeString(tempLicense, config.getLicenseFile(), StandardCharsets.UTF_8);
        args.add("--license-file");
        args.add(tempLicense.toAbsolutePath().toString());
        log("License file prepared for installer.");
    }

    // On Windows, if we want an EXE installer
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      args.add("--type"); args.add("exe");
      if (config.isCreateDesktopShortcut()) args.add("--win-shortcut");
      if (config.isCreateStartMenuEntry()) args.add("--win-menu");
      if (config.getIconPath() != null && !config.getIconPath().isBlank()) {
          args.add("--icon"); args.add(config.getIconPath());
      }
    }

    executeProcess(new ProcessBuilder(args), "jpackage");
    log("Build complete! Output located at: " + outputDir);
  }

  private void executeProcess(ProcessBuilder pb, String name) throws Exception {
    pb.redirectErrorStream(true);
    Process process = pb.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) log("[" + name + "] " + line);
    }
    int exitCode = process.waitFor();
    if (exitCode != 0) {
        throw new RuntimeException(name + " failed with exit code " + exitCode);
    }
  }
}
