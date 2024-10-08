# p2-maven-plugin

This project is a fork of `org.reficio:p2-maven-plugin`. See NOTICE and the [original repository](https://github.com/reficio/p2-maven-plugin) for the original project's details.

The original project readme is available as [README-orig.md](README-orig.md).

## Differences From Upstream

### Features

- Added resolution of features from available `p2`-layout repositories when the protocol is "file"
  - Note: this can cause trouble when the Maven-resolved feature contains different plugin versions than are available in the file p2 repositories, and so is primarily useful in edge cases when gathering features that are mavenized but without wanting to enumerate every plugin dependency
- Added ability to specify `p2features` in the configuration to bring in features found in available `p2`-layout repositories when the protocol is "file"
- Adjust plugin version resolution to allow for specific versions, including multiple versions with the same ID
- Added ability to specify p2 artifacts in `featureDefinition` blocks
- Added ability to generate an old-style "site.xml" file for the repository
- Added `archiveSite` configuration option to create a ZIP of the generated site
- Added ability to specify a `transform` option for an `artifact` definition to run the artifact (and its source, if bundled) through [Eclipse Transformer](https://github.com/eclipse/transformer). Currently only the value `jakarta` is supported

### Minor Changes

- Bumped several plugin and dependency versions
- Disabled failing IT suite
- Bubble feature-creation exceptions to the top
- Adjust plugin resolution to be a bit more correct (previously, it would match `com.foo.bar` to a plugin named `com.foo.bar.baz`)
- Adjust plugin version comparison to be a bit more correct (previously, "2.0.0" would outrank "15.0.0")

## Usage

The plugin is housed in OpenNTF's Maven repository, so you should add a `pluginRepository` to your project's pom:

```xml
<pluginRepositories>
  <pluginRepository>
    <id>artifactory.openntf.org</id>
    <name>artifactory.openntf.org</name>
    <url>https://artifactory.openntf.org/openntf</url>
  </pluginRepository>
</pluginRepositories>
```

Beyond that, basic usage is similar to the upstream project, except the Maven coordinates are changed:

```xml
<plugin>
  <groupId>org.openntf.maven</groupId>
  <artifactId>p2-maven-plugin</artifactId>
  <version>2.1.0</version>
  <executions>
    <execution>
      <id>generate-site</id>
      <goals>
        <goal>site</goal>
      </goals>
      <phase>prepare-package</phase>
      <configuration>
        <!-- config here -->
      </configuration>
    </execution>
  </executions>
</plugin>
```

