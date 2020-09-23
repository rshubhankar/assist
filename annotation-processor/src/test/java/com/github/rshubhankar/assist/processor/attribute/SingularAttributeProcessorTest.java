package com.github.rshubhankar.assist.processor.attribute;

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
import java.util.Optional;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

class SingularAttributeProcessorTest {
  private final SingularAttributeProcessor processor = new SingularAttributeProcessor();

  @Test
  void shouldCreateTradeSingularAttribute() throws IOException {
    final JavaFileObject parentJavaFile =
        JavaFileObjects.forSourceString(
            "com.github.rshubhankar.assist.processor.AbstractTrade",
            "package com.github.rshubhankar.assist.processor;\n"
                + "\n"
                + "import java.time.LocalDate;\n"
                + "import java.util.HashMap;\n"
                + "import java.util.Map;\n"
                + "\n"
                + "public abstract class AbstractTrade<T> {"
                + "   private T id;\n"
                // + "\n @java.beans.Transient"
                + "   public T getId() {\n"
                + "    return this.id;\n"
                + "  }\n"
                + "\n"
                + "  public void setId(final T id) {\n"
                + "    this.id = id;\n"
                + "  }"
                + "\n"
                + "}");
    final JavaFileObject javaFileObject =
        JavaFileObjects.forSourceString(
            "com.github.rshubhankar.assist.processor.Trade",
            "package com.github.rshubhankar.assist.processor;\n"
                + "\n"
                + "import java.time.LocalDate;\n"
                + "import java.util.HashMap;\n"
                + "import java.util.Map;\n"
                + "import com.github.rshubhankar.assist.annotation.attribute.*;\n"
                + "\n"
                + "@ClassAttribute"
                + "\n"
                + "public class Trade extends AbstractTrade<String> {"
                + "   private String tradeId;\n"
                + "   private Double quantity;\n"
                + "   public String getId() {\n"
                + "    return null;\n"
                + "  }\n"
                + "   "
                + "   public final String getTradeId() {\n"
                + "    return this.tradeId;\n"
                + "  }\n"
                + "\n"
                + "  public final void setTradeId(final String tradeId) {\n"
                + "    this.tradeId = tradeId;\n"
                + "  }"
                + "   "
                + "   public final Double getQuantity() {\n"
                + "    return this.quantity;\n"
                + "  }\n"
                + "\n"
                + "   @java.beans.Transient"
                + "   public final Integer getTolerance() {\n"
                + "    return null;\n"
                + "  }\n"
                + "\n"
                + "  public final void setQuantity(final Double quantity) {\n"
                + "    this.quantity = quantity;\n"
                + "  }"
                + "}");
    final Compilation compilation =
        this.compilerWithGenerator().compile(javaFileObject, parentJavaFile);
    final Optional<JavaFileObject> generatedSourceFile =
        compilation.generatedFile(
            SOURCE_OUTPUT, "com/github/rshubhankar/assist/processor/TradeSingularAttributes.java");

    //    this.printOutput(compilation, generatedSourceFile.get());
    assertEquals(Compilation.Status.SUCCESS, compilation.status());
    assertThat(generatedSourceFile).isPresent();
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

  private Compiler compilerWithGenerator() {
    return Compiler.javac().withProcessors(this.processor);
  }
}
