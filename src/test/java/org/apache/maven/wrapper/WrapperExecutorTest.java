package org.apache.maven.wrapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class WrapperExecutorTest {
  private final Installer install;

  private File propertiesFile;

  private Properties properties = new Properties();

  private File testDir = new File("target/test-files/WrapperExecutorTest-" + System.currentTimeMillis());

  private File mockInstallDir = new File(testDir, "mock-dir");

  public WrapperExecutorTest() throws Exception {
    install = mock(Installer.class);
    when(install.createDist(Mockito.any(WrapperConfiguration.class))).thenReturn(mockInstallDir);

    testDir.mkdirs();
    propertiesFile = new File(testDir, "maven/wrapper/maven-wrapper.properties");

    properties.put("distributionUrl", "http://server/test/maven.zip");
    properties.put("distributionBase", "testDistBase");
    properties.put("distributionPath", "testDistPath");
    properties.put("zipStoreBase", "testZipBase");
    properties.put("zipStorePath", "testZipPath");

    writePropertiesFile(properties, propertiesFile, "header");

  }

  @Test
  public void loadWrapperMetadataFromFile() throws Exception {
    WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile(propertiesFile);

    Assert.assertEquals(new URI("http://server/test/maven.zip"), wrapper.getDistribution());
    Assert.assertEquals(new URI("http://server/test/maven.zip"), wrapper.getConfiguration().getDistribution());
    Assert.assertEquals("testDistBase", wrapper.getConfiguration().getDistributionBase());
    Assert.assertEquals("testDistPath", wrapper.getConfiguration().getDistributionPath());
    Assert.assertEquals("testZipBase", wrapper.getConfiguration().getZipBase());
    Assert.assertEquals("testZipPath", wrapper.getConfiguration().getZipPath());
  }

  @Test
  public void propertiesFileOnlyContainsDistURL() throws Exception {

    properties = new Properties();
    properties.put("distributionUrl", "http://server/test/maven.zip");
    writePropertiesFile(properties, propertiesFile, "header");

    WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile(propertiesFile);

    Assert.assertEquals(new URI("http://server/test/maven.zip"), wrapper.getDistribution());
    Assert.assertEquals(new URI("http://server/test/maven.zip"), wrapper.getConfiguration().getDistribution());
    Assert.assertEquals(PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getDistributionBase());
    Assert.assertEquals(Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getDistributionPath());
    Assert.assertEquals(PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getZipBase());
    Assert.assertEquals(Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getZipPath());
  }

  @Test()
  public void failWhenDistNotSetInProperties() throws Exception {
    properties = new Properties();
    writePropertiesFile(properties, propertiesFile, "header");

    try {
      WrapperExecutor.forWrapperPropertiesFile(propertiesFile);
      Assert.fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      Assert.assertEquals("Could not load wrapper properties from '" + propertiesFile + "'.", e.getMessage());
      Assert.assertEquals("No value with key 'distributionUrl' specified in wrapper properties file '" + propertiesFile + "'.", e.getCause().getMessage());
    }

  }

  @Test
  public void failWhenPropertiesFileDoesNotExist() {
    propertiesFile = new File(testDir, "unknown.properties");

    try {
      WrapperExecutor.forWrapperPropertiesFile(propertiesFile);
      Assert.fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      Assert.assertEquals("Wrapper properties file '" + propertiesFile + "' does not exist.", e.getMessage());
    }
  }

  @Test
  public void testRelativeDistUrl() throws Exception {

    properties = new Properties();
    properties.put("distributionUrl", "some/relative/url/to/bin.zip");
    writePropertiesFile(properties, propertiesFile, "header");

    WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile(propertiesFile);
    Assert.assertNotEquals("some/relative/url/to/bin.zip", wrapper.getDistribution().getSchemeSpecificPart());
    Assert.assertTrue(wrapper.getDistribution().getSchemeSpecificPart().endsWith("some/relative/url/to/bin.zip"));
  }

  private void writePropertiesFile(Properties properties, File propertiesFile, String message) throws Exception {

    propertiesFile.getParentFile().mkdirs();

    OutputStream outStream = null;
    try {
      outStream = new FileOutputStream(propertiesFile);
      properties.store(outStream, message);
    } finally {
      IOUtils.closeQuietly(outStream);
    }
  }
}
