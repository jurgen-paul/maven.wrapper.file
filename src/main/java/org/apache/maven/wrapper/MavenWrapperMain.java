/*
 * Copyright 2007-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.maven.wrapper;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.wrapper.cli.CommandLineParser;
import org.apache.maven.wrapper.cli.SystemPropertiesCommandLineConverter;

/**
 * @author Hans Dockter
 */
public class MavenWrapperMain {
  public static final String DEFAULT_MAVEN_USER_HOME = System.getProperty("user.home") + "/.m2";

  public static final String MAVEN_USER_HOME_PROPERTY_KEY = "maven.user.home";

  public static final String MAVEN_USER_HOME_ENV_KEY = "MAVEN_USER_HOME";

  public static final String MVNW_VERBOSE = "MVNW_VERBOSE";
  public static final String MVNW_USERNAME = "MVNW_USERNAME";
  public static final String MVNW_PASSWORD = "MVNW_PASSWORD";
  public static final String MVNW_REPOURL = "MVNW_REPOURL";

  public static final String MVN_VERSION = "3.6.3";
  public static final String MVN_PATH =
      "org/apache/maven/apache-maven/" + MVN_VERSION + "/apache-maven-"+ MVN_VERSION + "-bin.zip";

  public static void main(String[] args) throws Exception {
    File wrapperJar = wrapperJar();
    File propertiesFile = wrapperProperties(wrapperJar);

    String wrapperVersion = wrapperVersion();
    Logger.info("Takari Maven Wrapper " + wrapperVersion);

    Properties systemProperties = System.getProperties();
    systemProperties.putAll(parseSystemPropertiesFromArgs(args));

    WrapperExecutor wrapperExecutor = WrapperExecutor.forWrapperPropertiesFile(propertiesFile);
    System.out.println(new Installer(new DefaultDownloader("mvnw", wrapperVersion), new PathAssembler(mavenUserHome())).createDist(wrapperExecutor.getConfiguration()).getCanonicalPath());
  }

  private static Map<String, String> parseSystemPropertiesFromArgs(String[] args) {
    SystemPropertiesCommandLineConverter converter = new SystemPropertiesCommandLineConverter();
    CommandLineParser commandLineParser = new CommandLineParser();
    converter.configure(commandLineParser);
    commandLineParser.allowUnknownOptions();
    return converter.convert(commandLineParser.parse(args));
  }

  private static File wrapperProperties(File wrapperJar) {
    return new File(wrapperJar.getParent(), wrapperJar.getName().replaceFirst("\\.jar$", ".properties"));
  }

  private static File wrapperJar() {
    URI location;
    try {
      location = MavenWrapperMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    if (!location.getScheme().equals("file")) {
      throw new RuntimeException(String.format("Cannot determine classpath for wrapper Jar from codebase '%s'.", location));
    }
    return new File(location.getPath());
  }

  static String wrapperVersion() {
    try {
      InputStream resourceAsStream = MavenWrapperMain.class.getResourceAsStream("/META-INF/maven/io.takari/maven-wrapper/pom.properties");
      if (resourceAsStream == null) {
        throw new RuntimeException("No maven properties found.");
      }
      Properties mavenProperties = new Properties();
      try {
        mavenProperties.load(resourceAsStream);
        String version = mavenProperties.getProperty("version");
        if (version == null) {
          throw new RuntimeException("No version number specified in build receipt resource.");
        }
        return version;
      } finally {
        resourceAsStream.close();
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not determine wrapper version.", e);
    }
  }

  private static File mavenUserHome() {
    String mavenUserHome = System.getProperty(MAVEN_USER_HOME_PROPERTY_KEY);
    if (mavenUserHome != null) {
      return new File(mavenUserHome);
    } else if ((mavenUserHome = System.getenv(MAVEN_USER_HOME_ENV_KEY)) != null) {
      return new File(mavenUserHome);
    } else {
      return new File(DEFAULT_MAVEN_USER_HOME);
    }
  }
}
