package com.github.rshubhankar.assist.attribute;

import java.util.function.Function;

public class SingularAttribute<P, T> {
  private final String field;
  private final Class<?> typeClass;
  private final Function<P, T> valueProvider;

  public SingularAttribute(
      final String field, final Class<?> typeClass, final Function<P, T> valueProvider) {
    this.field = field;
    this.typeClass = typeClass;
    this.valueProvider = valueProvider;
  }

  public String getField() {
    return this.field;
  }

  public Function<P, T> getValueProvider() {
    return this.valueProvider;
  }

  public Class<?> getTypeClass() {
    return this.typeClass;
  }
}
