package com.github.rshubhankar.assist.processor.accessor;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;

public final class GoogleFormatter {
  private static final Formatter JAVA_FORMATTER =
      new Formatter(JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.AOSP).build());

  private GoogleFormatter() {}

  public static String formatSource(final String source) {
    try {
      return JAVA_FORMATTER.formatSource(source);
    } catch (final FormatterException e) {
      throw new RuntimeException(e);
    }
  }
}
