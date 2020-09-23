package com.github.rshubhankar.assist.processor.accessor;

import static com.google.common.truth.Truth8.assertThat;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.junit.jupiter.api.Assertions.*;

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

class ClassFieldMappingProcessorTest {
  private final ClassFieldMappingProcessor processor = new ClassFieldMappingProcessor();

  @Test
  void shouldCreateTradeValueAccessorUsingInterface() throws IOException {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "com.github.rshubhankar.assist.processor.TradeValueAccessor",
            ""
                + "package com.github.rshubhankar.assist.processor;\n"
                + "\n"
                + "import java.time.LocalDate;\n"
                + "import java.util.HashMap;\n"
                + "import java.util.Map;\n"
                + "import com.github.rshubhankar.assist.accessor.*;\n"
                + "import com.github.rshubhankar.assist.annotation.accessor.*;\n"
                + "import com.github.rshubhankar.assist.processor.accessor.*;\n"
                + "\n"
                + "@ClassMapping(mapperClass = Deal.class)"
                + "\n"
                + "interface TradeValueAccessor extends ModelValueAccessor<Deal> {"
                + "  @FieldMapping(fieldName = Constants.DEAL_ID, classFieldName = \"dealId\")\n"
                + "  @FieldMapping(fieldName = \"quantity\", classFieldName =\"quantity\")\n"
                + "  @FieldMapping(fieldName = \"counterpart\", classFieldName =\"counterpart\")\n"
                + "  @FieldMapping(fieldName = \"deliveryFromDate\",customGetterMethod = \"getDeliveryFromDate\", customSetterMethod =\"setDeliveryFromDate\")\n"
                + "  @ClassValueGetter\n"
                + "  Object getValue(Deal trade, String field);\n"
                + "\n"
                + "  @ClassValueSetter\n"
                + "  void setValue(Deal trade, String field, Object value);"
                + "\n"
                + "  default LocalDate getDeliveryFromDate(Deal trade) {\n"
                + "     return trade.getDelivery().getFromDate();\n"
                + "  }\n"
                + "\n"
                + "  default void setDeliveryFromDate(Deal trade, LocalDate date) {\n"
                + "     trade.getDelivery().setFromDate(date);\n"
                + "  }\n"
                + "}");

    final Compilation compilation = this.compilerWithGenerator().compile(javaFileObject);
    final Optional<JavaFileObject> generatedSourceFile =
        compilation.generatedFile(
            SOURCE_OUTPUT, "com/github/rshubhankar/assist/processor/TradeValueAccessorImpl.java");
//    this.printOutput(compilation, generatedSourceFile);
    assertEquals(Compilation.Status.SUCCESS, compilation.status());
    assertThat(generatedSourceFile).isPresent();
  }

  @Test
  void shouldCreateTradeValueAccessorUsingAbstractClass() throws IOException {
    final JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "com.github.rshubhankar.assist.processor.Trade",
            ""
                + "package com.github.rshubhankar.assist.processor;\n"
                + "\n"
                + "import java.time.LocalDate;\n"
                + "import java.util.HashMap;\n"
                + "import java.util.Map;\n"
                + "import com.github.rshubhankar.assist.accessor.*;\n"
                + "import com.github.rshubhankar.assist.annotation.accessor.*;\n"
                + "import com.github.rshubhankar.assist.processor.accessor.*;\n"
                + "\n"
                + "@ClassMapping(mapperClass = Deal.class)"
                + "\n"
                + "abstract class TradeValueAccessor implements ModelValueAccessor<Deal> {"
                + "  @FieldMapping(fieldName = Constants.DEAL_ID, classFieldName = \"dealId\")\n"
                + "  @FieldMapping(fieldName = \"quantity\", classFieldName =\"quantity\")\n"
                + "  @FieldMapping(fieldName = \"counterpart\", classFieldName =\"counterpart\")\n"
                + "  @FieldMapping(fieldName = \"deliveryFromDate\", customGetterMethod = \"getDeliveryFromDate\", customSetterMethod =\"setDeliveryFromDate\")\n"
                + "  @FieldMapping(fieldName = \"costId\", classFieldName =\"primarySettlement.costId\")\n"
                + "  @FieldMapping(fieldName = \"costType\", classFieldName =\"primarySettlement.classification.costType\")\n"
                + "  @FieldMapping(fieldName = \"fixedPrice\", classFieldName =\"primarySettlement.pricingInfo.price\", downcastFieldsAtIndex = {\n"
                + "      @DowncastField(index = 1, downcastClass = FixedPricing.class)\n"
                + "  })\n"
                + "  @FieldMapping(fieldName = \"index\", classFieldName =\"primarySettlement.pricingInfo.index\", downcastFieldsAtIndex = {\n"
                + "      @DowncastField(index = 1, downcastClass = IndexPricing.class)\n"
                + "  })\n"
                + "  @ClassValueGetter\n"
                + "  public abstract Object getValue(Deal trade, String field);\n"
                + "\n"
                + "  @ClassValueSetter\n"
                + "  public abstract void setValue(Deal trade, String field, Object value);"
                + "\n"
                + "  protected LocalDate getDeliveryFromDate(Deal trade) {\n"
                + "     return trade.getDelivery().getFromDate();\n"
                + "  }\n"
                + "\n"
                + "  protected void setDeliveryFromDate(Deal trade, LocalDate date) {\n"
                + "     trade.getDelivery().setFromDate(date);\n"
                + "  }\n"
                + "}");

    final Compilation compilation = this.compilerWithGenerator().compile(javaFileObject);
    final Optional<JavaFileObject> generatedSourceFile =
        compilation.generatedFile(
            SOURCE_OUTPUT, "com/github/rshubhankar/assist/processor/TradeValueAccessorImpl.java");
//    this.printOutput(compilation, generatedSourceFile);
    assertEquals(Compilation.Status.SUCCESS, compilation.status());
    assertThat(generatedSourceFile).isPresent();
  }

  private Compiler compilerWithGenerator() {
    return Compiler.javac().withProcessors(this.processor);
  }

  private void printOutput(
      final Compilation compilation, final Optional<JavaFileObject> generatedSourceFile)
      throws IOException {
    for (final Diagnostic<? extends JavaFileObject> diagnostic : compilation.errors()) {
      System.out.println(diagnostic.toString());
    }
    try (final InputStream inputStream = generatedSourceFile.get().openInputStream()) {
      //      System.out.println(
      //          GoogleFormatter.formatSource(
      //              new BufferedReader(new InputStreamReader(inputStream))
      //                  .lines()
      //                  .collect(Collectors.joining("\n"))));
      System.out.println(
          new BufferedReader(new InputStreamReader(inputStream))
              .lines()
              .collect(Collectors.joining("\n")));
    }
  }
}
