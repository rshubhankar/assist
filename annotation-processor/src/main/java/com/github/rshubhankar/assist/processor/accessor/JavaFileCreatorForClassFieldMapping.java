package com.github.rshubhankar.assist.processor.accessor;

import com.github.rshubhankar.assist.processor.builder.ParameterElementInfo;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import com.github.rshubhankar.assist.accessor.ModelValueAccessor;
import com.github.rshubhankar.assist.annotation.accessor.FieldMapping;
import com.github.rshubhankar.assist.annotation.accessor.FieldMappings;

public class JavaFileCreatorForClassFieldMapping {

  private final Messager messager;
  private final ProcessingEnvironment processingEnv;

  public JavaFileCreatorForClassFieldMapping(
      final Messager messager, final ProcessingEnvironment processingEnv) {
    this.messager = messager;
    this.processingEnv = processingEnv;
  }

  public void createJavaFile(
      final Element annotatedElement, final List<Element> getters, final List<Element> setters)
      throws IOException {
    final ClassName className = ClassName.get((TypeElement) annotatedElement);
    final DeclaredType mapperClassType =
        ((DeclaredType)
            annotatedElement
                .getAnnotationMirrors()
                .get(0)
                .getElementValues()
                .values()
                .iterator()
                .next()
                .getValue());
    String packageName = "";
    final String canonicalName = className.canonicalName();
    final int lastDot = canonicalName.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = canonicalName.substring(0, lastDot);
    }

    final String implClassName = canonicalName + "Impl";
    final String implSimpleClassName = implClassName.substring(lastDot + 1);

    final TypeSpec.Builder typeSpecBuilder =
        TypeSpec.classBuilder(implSimpleClassName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    final FieldSpec getterValueMap = this.getGetterValueMapSpec(mapperClassType);
    final FieldSpec setterValueMap = this.getSetterValueMapSpec(mapperClassType);

    typeSpecBuilder.addField(getterValueMap).addField(setterValueMap);

    this.createInitMethodSpec(
        mapperClassType, getters.get(0), typeSpecBuilder, getterValueMap, setterValueMap);
    this.implementGetterValueMethodSpec(
        mapperClassType, getters.get(0), typeSpecBuilder, getterValueMap);
    this.implementSetterValueMethodSpec(
        mapperClassType, setters.get(0), typeSpecBuilder, setterValueMap);

    if (annotatedElement.getKind() == ElementKind.INTERFACE) {
      typeSpecBuilder.addSuperinterface(className);
    } else if (annotatedElement.getKind() == ElementKind.CLASS) {
      typeSpecBuilder.superclass(className);
    }

    this.implementModelValueAccessorMethods(
        (TypeElement) annotatedElement,
        mapperClassType,
        typeSpecBuilder,
        getterValueMap,
        setterValueMap);

    final JavaFile javaFile = JavaFile.builder(packageName, typeSpecBuilder.build()).build();
    //    javaFile.writeTo(this.processingEnv.getFiler());
    StringBuilder stringBuilder = new StringBuilder();
    javaFile.writeTo(stringBuilder);
    Filer filer = this.processingEnv.getFiler();
    JavaFileObject sourceFile = filer.createSourceFile(packageName + '.' + implSimpleClassName);
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(GoogleFormatter.formatSource(stringBuilder.toString()));
    }
  }

  private void implementModelValueAccessorMethods(
      final TypeElement annotatedElement,
      final DeclaredType mapperClassType,
      final TypeSpec.Builder typeSpecBuilder,
      final FieldSpec getterValueMap,
      final FieldSpec setterValueMap) {
    final Optional<DeclaredType> extInterfaceOptional =
        annotatedElement.getInterfaces().stream()
            .filter(
                type ->
                    ClassName.get(type)
                        .equals(
                            ParameterizedTypeName.get(
                                ClassName.get(ModelValueAccessor.class),
                                ClassName.get(mapperClassType))))
            .findFirst()
            .map(i -> (DeclaredType) i);
    if (extInterfaceOptional.isPresent()) {
      final DeclaredType extInterfaceType = extInterfaceOptional.get();
      final List<? extends Element> enclosedElements =
          extInterfaceType.asElement().getEnclosedElements();
      for (final Element enclosedElement : enclosedElements) {
        if (enclosedElement.getSimpleName().toString().equals("getGetterValueMap")) {
          typeSpecBuilder.addMethod(
              MethodSpec.methodBuilder(enclosedElement.getSimpleName().toString())
                  .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                  .addAnnotation(Override.class)
                  .returns(
                      ParameterizedTypeName.get(
                          ClassName.get(Map.class),
                          ClassName.get(String.class),
                          ParameterizedTypeName.get(
                              ClassName.get(Function.class),
                              ClassName.get(mapperClassType),
                              ClassName.get(Object.class))))
                  .addStatement("return $N", getterValueMap)
                  .build());
        }

        if (enclosedElement.getSimpleName().toString().equals("getSetterValueMap")) {
          typeSpecBuilder.addMethod(
              MethodSpec.methodBuilder(enclosedElement.getSimpleName().toString())
                  .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                  .addAnnotation(Override.class)
                  .returns(
                      ParameterizedTypeName.get(
                          ClassName.get(Map.class),
                          ClassName.get(String.class),
                          ParameterizedTypeName.get(
                              ClassName.get(BiConsumer.class),
                              ClassName.get(mapperClassType),
                              ClassName.get(Object.class))))
                  .addStatement("return $N", setterValueMap)
                  .build());
        }
      }
    }
  }

  private FieldSpec getGetterValueMapSpec(final ReferenceType referenceType) {
    return FieldSpec.builder(
            ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                    ClassName.get(Function.class),
                    ClassName.get(referenceType),
                    ClassName.get(Object.class))),
            "getterValueMap",
            Modifier.PRIVATE,
            Modifier.FINAL)
        .initializer("new $T<>()", LinkedHashMap.class)
        .build();
  }

  private FieldSpec getSetterValueMapSpec(final ReferenceType referenceType) {
    return FieldSpec.builder(
            ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                    ClassName.get(BiConsumer.class),
                    ClassName.get(referenceType),
                    ClassName.get(Object.class))),
            "setterValueMap",
            Modifier.PRIVATE,
            Modifier.FINAL)
        .initializer("new $T<>()", LinkedHashMap.class)
        .build();
  }

  private void createInitMethodSpec(
      final DeclaredType mapperClassType,
      final Element fieldMappingMethodElement,
      final TypeSpec.Builder typeSpecBuilder,
      final FieldSpec getterValueMap,
      final FieldSpec setterValueMap) {
    final MethodSpec.Builder methodBuilder =
        MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    final List<AnnotationMirror> fieldMappingsMirror;
    List<? extends AnnotationMirror> fieldMappingsAnnotationMirror =
        fieldMappingMethodElement.getAnnotationMirrors().stream()
            .filter(
                e ->
                    ClassName.get(e.getAnnotationType()).equals(ClassName.get(FieldMappings.class)))
            .collect(Collectors.toList());
    if (fieldMappingsAnnotationMirror.isEmpty()) {
      fieldMappingsAnnotationMirror =
          fieldMappingMethodElement.getAnnotationMirrors().stream()
              .filter(
                  e ->
                      ClassName.get(e.getAnnotationType())
                          .equals(ClassName.get(FieldMapping.class)))
              .collect(Collectors.toList());
      fieldMappingsMirror = (List<AnnotationMirror>) fieldMappingsAnnotationMirror;
    } else {
      fieldMappingsMirror =
          (List<AnnotationMirror>)
              fieldMappingsAnnotationMirror
                  .get(0)
                  .getElementValues()
                  .values()
                  .iterator()
                  .next()
                  .getValue();
    }
    for (int i = 0; i < fieldMappingsMirror.size(); i++) {
      final AnnotationMirror fieldMappingMirror = fieldMappingsMirror.get(i);
      FieldValueMapProcessor.addGetterMapping(
          fieldMappingMirror,
          mapperClassType,
          fieldMappingMethodElement,
          getterValueMap,
          typeSpecBuilder,
          methodBuilder);

      FieldValueMapProcessor.addSetterMapping(
          fieldMappingMirror,
          mapperClassType,
          fieldMappingMethodElement,
          setterValueMap,
          typeSpecBuilder,
          methodBuilder);
    }
    typeSpecBuilder.addMethod(methodBuilder.build());
  }

  private void implementGetterValueMethodSpec(
      final DeclaredType mapperClassType,
      final Element getterElement,
      final TypeSpec.Builder typeSpecBuilder,
      final FieldSpec getterValueMap) {
    final List<? extends TypeMirror> parameterTypes =
        ((ExecutableType) getterElement.asType()).getParameterTypes();
    final List<? extends VariableElement> variableNames =
        ((ExecutableElement) getterElement).getParameters();
    final List<ParameterElementInfo> parameterElementInfos = new ArrayList<>();
    for (int i = 0; i < parameterTypes.size(); i++) {
      parameterElementInfos.add(
          new ParameterElementInfo(
              ClassName.get(parameterTypes.get(i)),
              variableNames.get(i).getSimpleName().toString()));
    }

    if (parameterElementInfos.size() == 2) {
      final Optional<ParameterElementInfo> mapperClassParam =
          parameterElementInfos.stream()
              .filter(param -> param.type.equals(ClassName.get(mapperClassType)))
              .findFirst();
      final Optional<ParameterElementInfo> fieldNameParam =
          parameterElementInfos.stream()
              .filter(param -> param.type.equals(ClassName.get(String.class)))
              .findFirst();
      if (mapperClassParam.isPresent() && fieldNameParam.isPresent()) {
        typeSpecBuilder.addMethod(
            MethodSpec.methodBuilder(getterElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(Override.class)
                .addParameters(
                    parameterElementInfos.stream()
                        .map(
                            parameterElementInfo ->
                                ParameterSpec.builder(
                                        parameterElementInfo.type, parameterElementInfo.name)
                                    .build())
                        .collect(Collectors.toList()))
                .addStatement(
                    "return $T.ofNullable(this.$N.get($L)).map(func -> func.apply($L)).orElse(null)",
                    ClassName.get(Optional.class),
                    getterValueMap,
                    fieldNameParam.get().name,
                    mapperClassParam.get().name)
                .returns(ClassName.get(((ExecutableElement) getterElement).getReturnType()))
                .build());
      } else {
        this.messager.printMessage(
            Diagnostic.Kind.ERROR, "Getter method should have only 2 arguments.");
        throw new RuntimeException("Getter method should have only 2 arguments.");
      }
    } else {
      this.messager.printMessage(
          Diagnostic.Kind.ERROR,
          "Getter method should have only 2 arguments, 1 for the Mapping class and other for field's name.");
      throw new RuntimeException(
          "Getter method should have only 2 arguments, 1 for the Mapping class and other for field's name.");
    }
  }

  private void implementSetterValueMethodSpec(
      final DeclaredType mapperClassType,
      final Element setterElement,
      final TypeSpec.Builder typeSpecBuilder,
      final FieldSpec setterValueMap) {
    final List<? extends TypeMirror> parameterTypes =
        ((ExecutableType) setterElement.asType()).getParameterTypes();
    final List<? extends VariableElement> variableNames =
        ((ExecutableElement) setterElement).getParameters();
    final List<ParameterElementInfo> parameterElementInfos = new ArrayList<>();
    for (int i = 0; i < parameterTypes.size(); i++) {
      parameterElementInfos.add(
          new ParameterElementInfo(
              ClassName.get(parameterTypes.get(i)),
              variableNames.get(i).getSimpleName().toString()));
    }

    if (parameterElementInfos.size() == 3) {
      final Optional<ParameterElementInfo> mapperClassParam =
          parameterElementInfos.stream()
              .filter(param -> param.type.equals(ClassName.get(mapperClassType)))
              .findFirst();
      final Optional<ParameterElementInfo> valueParam =
          parameterElementInfos.stream()
              .filter(param -> param.type.equals(ClassName.get(Object.class)))
              .findFirst();
      final Optional<ParameterElementInfo> fieldNameParam =
          parameterElementInfos.stream()
              .filter(param -> param.type.equals(ClassName.get(String.class)))
              .findFirst();
      if (mapperClassParam.isPresent() && valueParam.isPresent() && fieldNameParam.isPresent()) {
        typeSpecBuilder.addMethod(
            MethodSpec.methodBuilder(setterElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(Override.class)
                .addParameters(
                    parameterElementInfos.stream()
                        .map(
                            parameterElementInfo ->
                                ParameterSpec.builder(
                                        parameterElementInfo.type, parameterElementInfo.name)
                                    .build())
                        .collect(Collectors.toList()))
                .addStatement(
                    "$T.ofNullable(this.$N.get($L)).ifPresent(consumer -> consumer.accept($L, $L))",
                    ClassName.get(Optional.class),
                    setterValueMap,
                    fieldNameParam.get().name,
                    mapperClassParam.get().name,
                    valueParam.get().name)
                .returns(void.class)
                .build());
      } else {
        this.messager.printMessage(
            Diagnostic.Kind.ERROR, "Getter method should have only 3 arguments.");
        throw new RuntimeException("Getter method should have only 3 arguments.");
      }
    } else {
      this.messager.printMessage(
          Diagnostic.Kind.ERROR,
          "Getter method should have only 3 arguments, 1 for the Mapping class, 1 for value and other for field's name.");
      throw new RuntimeException(
          "Getter method should have only 3 arguments, 1 for the Mapping class, 1 for value and other for field's name.");
    }
  }
}
