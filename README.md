# Output Parser with LLMs

## Overview

This projects generates a binary. The binary is a native image that uses the
Ollama API to parse the output of a command. The output is then processed by a
language model to generate a more human-readable output, store the original
output in `/tmp/output.log` and the processed output in `/tmp/processed.json` so
it can be used by other applications (e.g. NeoVim).

![Demo Video](demo.mov)

## Requirements

- GraalVM JDK 21 or higher

## Setup

You need Ollama and the mistral model to run this project.

```sh
ollama pull mistral
ollama serve
```

## Compilation

To compile the project, navigate to the project directory and run the following
command:

```sh
./gradlew nativeCompile
```

## Compilation

Place the binary in your path:

```sh
mv outputParser/build/native/nativeCompile/outputParser ~/bin/o
```

After that, pipe the output of a command to the binary:

```sh
gradle test |& o
```

## TODO

- [ ] The code was generated by Copilot, needs to be cleaned up.
- [ ] Add more language models and proper configuration options.
- [ ] It would be nice to test JReleaser with this project.
