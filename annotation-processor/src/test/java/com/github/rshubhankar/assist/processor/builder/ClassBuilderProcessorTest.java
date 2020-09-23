package com.github.rshubhankar.assist.processor.builder;

import static com.google.common.truth.Truth8.assertThat;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

class ClassBuilderProcessorTest {
  private final ClassBuilderProcessor processor = new ClassBuilderProcessor();

  @Test
  void shouldCreateTradeBuilder() throws IOException, URISyntaxException {
    final String fileContent =
        String.join(
            "\n",
            Files.readAllLines(
                Paths.get(
                    Objects.requireNonNull(
                            this.getClass().getClassLoader().getResource("Trade.java"))
                        .toURI())));
    final JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines("org.roy.filed.assist.processor.Trade", fileContent);
    final Compilation compilation = this.compilerWithGenerator().compile(javaFileObject);
    final Optional<JavaFileObject> generatedSourceFile =
        compilation.generatedFile(
            SOURCE_OUTPUT, "com/github/rshubhankar/assist/processor/TradeBuilder.java");

    //    this.printOutput(compilation, generatedSourceFile.get());
    assertEquals(Compilation.Status.SUCCESS, compilation.status());
    assertThat(generatedSourceFile).isPresent();
  }

  private Compiler compilerWithGenerator() {
    return Compiler.javac().withProcessors(this.processor);
  }

  private void printOutput(final Compilation compilation, final JavaFileObject generatedSourceFile)
      throws IOException {
    for (final Diagnostic<? extends JavaFileObject> diagnostic : compilation.errors()) {
      System.out.println(diagnostic.toString());
    }
    try (final InputStream inputStream = generatedSourceFile.openInputStream()) {
      System.out.println(
          new BufferedReader(new InputStreamReader(inputStream))
              .lines()
              .collect(Collectors.joining("\n")));
    }
  }
}
