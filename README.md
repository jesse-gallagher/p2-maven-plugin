# p2-maven-plugin

This project is a fork of `org.reficio:p2-maven-plugin`. See NOTICE and the [original repository](https://github.com/reficio/p2-maven-plugin) for the original project's details.

The original project readme is available as [README-orig.md](README-orig.md).

## Differences From Upstream

### Features

- Added resolution of features from available `p2`-layout repositories when the protocol is "file"
  - Note: this can cause trouble when the Maven-resolved feature contains different plugin versions than are available in the file p2 repositories, and so is primarily useful in edge cases when gathering features that are mavenized but without wanting to enumerate every plugin dependency
- Added ability to specify `p2features` in the configuration to bring in features found in available `p2`-layout repositories when the protocol is "file"
- Adjust plugin resolution to be a bit more correct (previously it would match `com.foo.bar` to a plugin named `com.foo.bar.baz`)
- Adjust plugin version resolution to allow for specific versions, including multiple versions with the same ID
- Added ability to specify p2 artifacts in `featureDefinition` blocks

### Minor Changes

- Bumped several plugin and dependency versions
- Disabled failing IT suite

#### 