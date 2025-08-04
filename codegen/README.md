# `codegen/`

This module parses `.mvpe.yaml` DSL and generates:

- Java service interfaces and DTOs
- Dispatcher and client proxy stubs
- SBE XML schema for use with SBE compiler

It is intended to be used via the CLI or Gradle plugin.
