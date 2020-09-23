package com.github.rshubhankar.assist.processor.builder;

import com.squareup.javapoet.TypeName;

public class ParameterElementInfo {
  public TypeName type;
  public String name;

  public ParameterElementInfo(final TypeName type, final String name) {
    this.type = type;
    this.name = name;
  }
}
