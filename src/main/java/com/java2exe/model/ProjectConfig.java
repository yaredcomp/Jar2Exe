package com.java2exe.model;

/**
 * Data model for the project configuration.
 */
public class ProjectConfig {

  private String projectPath;
  private String mainClass;
  private String appName;
  private String version;
  private String vendor;
  private String iconPath;
  private String licenseFile;
  private String jvmOptions;
  private String outputDir;
  private boolean includeJre;
  private String packageType;
  private boolean createDesktopShortcut;
  private boolean createStartMenuEntry;
  private boolean generateDebugSymbols;
  private String antBuildTarget;

  public String getAntBuildTarget() { return antBuildTarget; }
  public void setAntBuildTarget(String antBuildTarget) { this.antBuildTarget = antBuildTarget; }
  public String getProjectPath() { return projectPath; }
  public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
  public String getMainClass() { return mainClass; }
  public void setMainClass(String mainClass) { this.mainClass = mainClass; }
  public String getAppName() { return appName; }
  public void setAppName(String appName) { this.appName = appName; }
  public String getVersion() { return version; }
  public void setVersion(String version) { this.version = version; }
  public String getVendor() { return vendor; }
  public void setVendor(String vendor) { this.vendor = vendor; }
  public String getIconPath() { return iconPath; }
  public void setIconPath(String iconPath) { this.iconPath = iconPath; }
  public String getLicenseFile() { return licenseFile; }
  public void setLicenseFile(String licenseFile) { this.licenseFile = licenseFile; }
  public String getJvmOptions() { return jvmOptions; }
  public void setJvmOptions(String jvmOptions) { this.jvmOptions = jvmOptions; }
  public String getOutputDir() { return outputDir; }
  public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
  public boolean isIncludeJre() { return includeJre; }
  public void setIncludeJre(boolean includeJre) { this.includeJre = includeJre; }
  public String getPackageType() { return packageType; }
  public void setPackageType(String packageType) { this.packageType = packageType; }
  public boolean isCreateDesktopShortcut() { return createDesktopShortcut; }
  public void setCreateDesktopShortcut(boolean createDesktopShortcut) { this.createDesktopShortcut = createDesktopShortcut; }
  public boolean isCreateStartMenuEntry() { return createStartMenuEntry; }
  public void setCreateStartMenuEntry(boolean createStartMenuEntry) { this.createStartMenuEntry = createStartMenuEntry; }
  public boolean isGenerateDebugSymbols() { return generateDebugSymbols; }
  public void setGenerateDebugSymbols(boolean generateDebugSymbols) { this.generateDebugSymbols = generateDebugSymbols; }
}
