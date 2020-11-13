# p2-maven-plugin

This project is a fork of `org.reficio:p2-maven-plugin`. See NOTICE and the [original repository](https://github.com/reficio/p2-maven-plugin) for the original project's details.

The original project readme is available as [README-orig.md](README-orig.md).

## Differences From Upstream

### Features

- Added resolution of features from available `p2`-layout repositories when the protocol is "file"
- Adjust plugin resolution to be a bit more correct (previously it would match `com.foo.bar` to a plugin named `com.foo.bar.baz`)
- Added ability to specify p2 artifacts in `featureDefinition` blocks

### Minor Changes

- Bumped several plugin and dependency versions
- Disabled failing IT suite

#### 