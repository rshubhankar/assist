package com.github.rshubhankar.assist.processor.builder;

import com.squareup.javapoet.TypeName;
import java.util.List;

public class MethodElementInfo {
  public String name;
  public List<ParameterElementInfo> parameterTypeElementInfos;
  public TypeName returnType;
}
