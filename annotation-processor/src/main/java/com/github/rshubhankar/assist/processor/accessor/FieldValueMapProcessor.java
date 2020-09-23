package com.github.rshubhankar.assist.processor.accessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import org.apache.commons.lang3.StringUtils;

public final class FieldValueMapProcessor {
  private FieldValueMapProcessor() {}

  public static void addGetterMapping(
      final AnnotationMirror fieldMappingMirror,
      final DeclaredType mapperClassType,
      final Element fieldMappingMethodElement,
      final FieldSpec getterValueMapField,
      final TypeSpec.Builder typeSpecBuilder,
      final MethodSpec.Builder methodBuilder) {

    final TypeName mapperClassName = ClassName.get(mapperClassType);
    final Element mapperClassElement = mapperClassType.asElement();
    final String mapperClassFieldName = getClassFieldName(mapperClassName);

    final String fieldName =
        fieldMappingMirror.getElementValues().entrySet().stream()
            .filter(e -> e.getKey().getSimpleName().toString().equals("fieldName"))
            .map(Map.Entry::getValue)
            .map(AnnotationValue::getValue)
            .findFirst()
            .map(Object::toString)
            .orElse(null);
    final String classFieldName = fieldMappingMirror.getElementValues().entrySet().stream()
        .filter(e -> e.getKey().getSimpleName().toString().equals("classFieldName"))
        .map(Map.Entry::getValue)
        .map(AnnotationValue::getValue)
        .findFirst()
        .map(Object::toString)
        .orElse(null);
    final String customGetterMethod = fieldMappingMirror.getElementValues().entrySet().stream()
        .filter(e -> e.getKey().getSimpleName().toString().equals("customGetterMethod"))
        .map(Map.Entry::getValue)
        .map(AnnotationValue::getValue)
        .findFirst()
        .map(Object::toString)
        .orElse(null);

    if (StringUtils.isNotBlank(customGetterMethod)) {
      addCustomGetterMethodMapping(
          fieldMappingMethodElement,
          getterValueMapField,
          methodBuilder,
          mapperClassFieldName,
          fieldName,
          customGetterMethod);
    } else if (StringUtils.isNotBlank(classFieldName)) {
      if (!StringUtils.contains(classFieldName, ".")) {
        addFieldGetterMethodMapping(
            mapperClassElement,
            getterValueMapField,
            methodBuilder,
            classFieldName,
            mapperClassFieldName,
            fieldName);
      } else {
        addNestedFieldGetterMethodMapping(
            fieldMappingMirror,
            mapperClassElement,
            getterValueMapField,
            typeSpecBuilder,
            methodBuilder,
            classFieldName,
            mapperClassFieldName,
            fieldName);
      }
    }
  }

  public static void addSetterMapping(
      final AnnotationMirror fieldMappingMirror,
      final DeclaredType mapperClassType,
      final Element fieldMappingMethodElement,
      final FieldSpec setterValueMapField,
      final TypeSpec.Builder typeSpecBuilder,
      final MethodSpec.Builder methodBuilder) {
    final String fieldName =
        fieldMappingMirror.getElementValues().entrySet().stream()
            .filter(e -> e.getKey().getSimpleName().toString().equals("fieldName"))
            .map(Map.Entry::getValue)
            .map(AnnotationValue::getValue)
            .findFirst()
            .map(Object::toString)
            .orElse(null);
    final String classFieldName = fieldMappingMirror.getElementValues().entrySet().stream()
        .filter(e -> e.getKey().getSimpleName().toString().equals("classFieldName"))
        .map(Map.Entry::getValue)
        .map(AnnotationValue::getValue)
        .findFirst()
        .map(Object::toString)
        .orElse(null);
    final String customSetterMethod = fieldMappingMirror.getElementValues().entrySet().stream()
        .filter(e -> e.getKey().getSimpleName().toString().equals("customSetterMethod"))
        .map(Map.Entry::getValue)
        .map(AnnotationValue::getValue)
        .findFirst()
        .map(Object::toString)
        .orElse(null);

    if (StringUtils.isNotBlank(customSetterMethod)) {
      addCustomSetterMethodMapping(
          fieldMappingMethodElement,
          setterValueMapField,
          methodBuilder,
          mapperClassType,
          fieldName,
          customSetterMethod);
    } else if (StringUtils.isNotBlank(classFieldName)) {
      if (!StringUtils.contains(classFieldName, ".")) {
        addFieldSetterMethodMapping(
            mapperClassType, setterValueMapField, methodBuilder, classFieldName, fieldName);
      } else {
        addNestedFieldSetterMethodMapping(
            fieldMappingMirror,
            mapperClassType,
            setterValueMapField,
            typeSpecBuilder,
            methodBuilder,
            classFieldName,
            fieldName);
      }
    }
  }

  private static void addFieldGetterMethodMapping(
      final Element mapperClassElement,
      final FieldSpec getterValueMapField,
      final MethodSpec.Builder methodBuilder,
      final String classFieldName,
      final String mapperClassFieldName,
      final String fieldName) {
    final String getterMethodName =
        "get" + classFieldName.substring(0, 1).toUpperCase() + classFieldName.substring(1);
    final Optional<? extends Element> getterMethodElement =
        mapperClassElement.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.METHOD)
            .filter(element -> element.getSimpleName().toString().startsWith(getterMethodName))
            .findFirst();

    if (getterMethodElement.isPresent()) {
      methodBuilder.addCode("\n");
      methodBuilder.addComment("Getter for field $L", fieldName);
      methodBuilder.addStatement(
          "this.$N.put($S, $L -> { return ($T) $L.$L(); })",
          getterValueMapField,
          fieldName,
          mapperClassFieldName,
          ((ExecutableElement) getterMethodElement.get()).getReturnType(),
          mapperClassFieldName,
          getterMethodName);
    }
  }

  private static void addCustomGetterMethodMapping(
      final Element fieldMappingMethodElement,
      final FieldSpec getterValueMapField,
      final MethodSpec.Builder methodBuilder,
      final String mapperClassFieldName,
      final String fieldName,
      final String customGetterMethod) {
    final Optional<? extends Element> getterMethodElement =
        fieldMappingMethodElement.getEnclosingElement().getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.METHOD)
            .filter(element -> element.getSimpleName().toString().startsWith(customGetterMethod))
            .findFirst();

    if (getterMethodElement.isPresent()) {
      methodBuilder.addCode("\n");
      methodBuilder.addComment("Getter for field $L", fieldName);
      methodBuilder.addStatement(
          "this.$N.put($S, $L -> { return ($T) this.$L($L); })",
          getterValueMapField,
          fieldName,
          mapperClassFieldName,
          ((ExecutableElement) getterMethodElement.get()).getReturnType(),
          customGetterMethod,
          mapperClassFieldName);
    }
  }

  private static void addNestedFieldGetterMethodMapping(
      final AnnotationMirror fieldMappingMirror,
      final Element mapperClassElement,
      final FieldSpec getterValueMapField,
      final TypeSpec.Builder typeSpecBuilder,
      final MethodSpec.Builder methodBuilder,
      final String classFieldName,
      final String mapperClassFieldName,
      final String nestedFieldName) {
    final List<? extends AnnotationValue> downcastFieldsAttrMirror =
        fieldMappingMirror.getElementValues().entrySet().stream()
            .filter(e1 -> e1.getKey().getSimpleName().toString().equals("downcastFieldsAtIndex"))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    Map<Integer, DeclaredType> downcastFieldMap = Collections.emptyMap();
    if (!downcastFieldsAttrMirror.isEmpty()) {
      downcastFieldMap =
          ((List<AnnotationMirror>) downcastFieldsAttrMirror.get(0).getValue())
              .stream()
                  .collect(
                      Collectors.toMap(
                          d ->
                              d.getElementValues().entrySet().stream()
                                  .filter(
                                      e -> e.getKey().getSimpleName().toString().equals("index"))
                                  .findFirst()
                                  .map(a -> (Integer) a.getValue().getValue())
                                  .orElse(-1),
                          d ->
                              d.getElementValues().entrySet().stream()
                                  .filter(
                                      e ->
                                          e.getKey()
                                              .getSimpleName()
                                              .toString()
                                              .equals("downcastClass"))
                                  .findFirst()
                                  .map(a -> (DeclaredType) a.getValue().getValue())
                                  .orElse(null)));
    }
    final String[] nestedFields = classFieldName.split("\\.");
    final String customGetterMethodName =
        "get"
            + Stream.of(nestedFields)
                .map(field -> field.substring(0, 1).toUpperCase() + field.substring(1))
                .reduce(String::concat)
                .orElse("");

    final MethodSpec.Builder customGetterMethodBuilder =
        MethodSpec.methodBuilder(customGetterMethodName)
            .addModifiers(Modifier.PRIVATE)
            .addParameter(ClassName.get(mapperClassElement.asType()), mapperClassFieldName);

    Element classElement = mapperClassElement;
    String classElementFieldName = mapperClassFieldName;
    Optional<? extends Element> getterMethodElement = Optional.empty();
    for (int i = 0; i < nestedFields.length; i++) {
      final String nestedField = nestedFields[i];
      final String getterMethodName =
          "get" + nestedField.substring(0, 1).toUpperCase() + nestedField.substring(1);
      getterMethodElement =
          classElement.getEnclosedElements().stream()
              .filter(element -> element.getKind() == ElementKind.METHOD)
              .filter(element -> element.getSimpleName().toString().startsWith(getterMethodName))
              .findFirst();

      if (getterMethodElement.isPresent()) {
        final Element returnedClassElement =
            ((DeclaredType) ((ExecutableType) getterMethodElement.get().asType()).getReturnType())
                .asElement();
        final String returnedClassFieldName =
            getFieldNameFromMethod((ExecutableElement) getterMethodElement.get());

        customGetterMethodBuilder
            .beginControlFlow(
                "if(null == $L.$L)", classElementFieldName, getterMethodElement.get().toString())
            .addStatement("return null")
            .endControlFlow()
            .addCode("\n")
            .addStatement(
                "$T $L = $L.$L",
                ClassName.get(returnedClassElement.asType()),
                returnedClassFieldName,
                classElementFieldName,
                getterMethodElement.get().toString());

        if (downcastFieldMap.containsKey(i)) {
          customGetterMethodBuilder
              .beginControlFlow(
                  "if(!($L instanceof $T))", returnedClassFieldName, downcastFieldMap.get(i))
              .addStatement("return null")
              .endControlFlow()
              .addCode("\n");

          final TypeName downcastedClassName = ClassName.get(downcastFieldMap.get(i));
          final String downcastedClassFieldName = getClassFieldName(downcastedClassName);

          customGetterMethodBuilder.addStatement(
              "$T $L = ($T)$L",
              downcastedClassName,
              downcastedClassFieldName,
              downcastedClassName,
              returnedClassFieldName);

          classElement = downcastFieldMap.get(i).asElement();
          classElementFieldName = downcastedClassFieldName;
        } else {
          classElement = returnedClassElement;
          classElementFieldName = returnedClassFieldName;
        }
      } else {
        break;
      }
    }

    final TypeName finalReturnType;
    if (!getterMethodElement.isPresent()) {
      finalReturnType = ClassName.get(Object.class);
      customGetterMethodBuilder.addStatement("return null");
    } else {
      finalReturnType = ClassName.get(classElement.asType());
      customGetterMethodBuilder.addStatement("return $L", classElementFieldName);

      typeSpecBuilder.addMethod(customGetterMethodBuilder.returns(finalReturnType).build());

      methodBuilder.addCode("\n");
      methodBuilder.addComment("Getter for field $L", nestedFieldName);
      methodBuilder.addStatement(
          "this.$N.put($S, $L -> { return ($T) this.$L($L); })",
          getterValueMapField,
          nestedFieldName,
          mapperClassFieldName,
          finalReturnType,
          customGetterMethodName,
          mapperClassFieldName);
    }
  }

  private static void addFieldSetterMethodMapping(
      final DeclaredType mapperClassType,
      final FieldSpec setterValueMapField,
      final MethodSpec.Builder methodBuilder,
      final String classFieldName,
      final String fieldName) {
    final String mapperClassFieldName = getClassFieldName(ClassName.get(mapperClassType));
    final String setterMethodName =
        "set" + classFieldName.substring(0, 1).toUpperCase() + classFieldName.substring(1);
    final Optional<? extends Element> setterMethodElement =
        mapperClassType.asElement().getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.METHOD)
            .filter(element -> element.getSimpleName().toString().startsWith(setterMethodName))
            .findFirst();
    if (setterMethodElement.isPresent()) {
      methodBuilder.addCode("\n");
      methodBuilder.addComment("Setter for field $L", fieldName);
      methodBuilder.addStatement(
          "this.$N.put($S, ($L, value) -> { if(value != null) $L.$L(($T) value); })",
          setterValueMapField,
          fieldName,
          mapperClassFieldName,
          mapperClassFieldName,
          setterMethodName,
          ((ExecutableType) setterMethodElement.get().asType()).getParameterTypes().get(0));
    }
  }

  private static void addCustomSetterMethodMapping(
      final Element fieldMappingMethodElement,
      final FieldSpec setterValueMapField,
      final MethodSpec.Builder methodBuilder,
      final DeclaredType mapperClassType,
      final String fieldName,
      final String customSetterMethod) {
    final String mapperClassFieldName = getClassFieldName(ClassName.get(mapperClassType));
    final Optional<? extends Element> setterMethodElement =
        fieldMappingMethodElement.getEnclosingElement().getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.METHOD)
            .filter(element -> element.getSimpleName().toString().startsWith(customSetterMethod))
            .findFirst();
    if (setterMethodElement.isPresent()) {
      methodBuilder.addCode("\n");
      methodBuilder.addComment("Setter for field $L", fieldName);
      methodBuilder.addStatement(
          "this.$N.put($S, ($L, value) -> { if(value != null) this.$L($L, ($T) value); })",
          setterValueMapField,
          fieldName,
          mapperClassFieldName,
          customSetterMethod,
          mapperClassFieldName,
          ((ExecutableType) setterMethodElement.get().asType())
              .getParameterTypes().stream()
                  .filter(type -> !type.equals(mapperClassType))
                  .findFirst()
                  .orElse(null));
    }
  }

  private static void addNestedFieldSetterMethodMapping(
      final AnnotationMirror fieldMappingMirror,
      final DeclaredType mapperClassType,
      final FieldSpec setterValueMapField,
      final TypeSpec.Builder typeSpecBuilder,
      final MethodSpec.Builder methodBuilder,
      final String classFieldName,
      final String nestedFieldName) {
    final List<? extends AnnotationValue> downcastFieldsAttrMirror =
        fieldMappingMirror.getElementValues().entrySet().stream()
            .filter(e1 -> e1.getKey().getSimpleName().toString().equals("downcastFieldsAtIndex"))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    Map<Integer, DeclaredType> downcastFieldMap = Collections.emptyMap();
    if (!downcastFieldsAttrMirror.isEmpty()) {
      downcastFieldMap =
          ((List<AnnotationMirror>) downcastFieldsAttrMirror.get(0).getValue())
              .stream()
                  .collect(
                      Collectors.toMap(
                          d ->
                              d.getElementValues().entrySet().stream()
                                  .filter(
                                      e -> e.getKey().getSimpleName().toString().equals("index"))
                                  .findFirst()
                                  .map(a -> (Integer) a.getValue().getValue())
                                  .orElse(-1),
                          d ->
                              d.getElementValues().entrySet().stream()
                                  .filter(
                                      e ->
                                          e.getKey()
                                              .getSimpleName()
                                              .toString()
                                              .equals("downcastClass"))
                                  .findFirst()
                                  .map(a -> (DeclaredType) a.getValue().getValue())
                                  .orElse(null)));
    }
    final String[] nestedFields = classFieldName.split("\\.");
    final String customSetterMethodName =
        "set"
            + Stream.of(nestedFields)
                .map(field -> field.substring(0, 1).toUpperCase() + field.substring(1))
                .reduce(String::concat)
                .orElse("");

    final MethodSpec.Builder customSetterMethodBuilder =
        MethodSpec.methodBuilder(customSetterMethodName)
            .addModifiers(Modifier.PRIVATE)
            .returns(void.class);

    Element classElement = mapperClassType.asElement();
    String classElementFieldName = getClassFieldName(ClassName.get(classElement.asType()));

    for (int i = 0; i < nestedFields.length - 1; i++) {
      final String nestedField = nestedFields[i];
      final String setterMethodName =
          "set" + nestedField.substring(0, 1).toUpperCase() + nestedField.substring(1);
      final Optional<? extends Element> setterMethodElement =
          classElement.getEnclosedElements().stream()
              .filter(element -> element.getKind() == ElementKind.METHOD)
              .filter(element -> element.getSimpleName().toString().startsWith(setterMethodName))
              .findFirst();

      final String getterMethodName =
          "get" + nestedField.substring(0, 1).toUpperCase() + nestedField.substring(1);
      final Optional<? extends Element> getterMethodElement =
          classElement.getEnclosedElements().stream()
              .filter(element -> element.getKind() == ElementKind.METHOD)
              .filter(element -> element.getSimpleName().toString().startsWith(getterMethodName))
              .findFirst();

      if (setterMethodElement.isPresent() && getterMethodElement.isPresent()) {
        final Element returnedClassElement =
            ((DeclaredType) ((ExecutableType) getterMethodElement.get().asType()).getReturnType())
                .asElement();
        final String returnedClassFieldName =
            getFieldNameFromMethod((ExecutableElement) getterMethodElement.get());

        if (downcastFieldMap.containsKey(i)) {
          customSetterMethodBuilder
              .beginControlFlow(
                  "if(null == $L.$L)", classElementFieldName, getterMethodElement.get().toString())
              .addStatement("return")
              .endControlFlow()
              .addCode("\n")
              .addStatement(
                  "$T $L = $L.$L",
                  ClassName.get(returnedClassElement.asType()),
                  returnedClassFieldName,
                  classElementFieldName,
                  getterMethodElement.get().toString())
              .beginControlFlow(
                  "if(!($L instanceof $T))", returnedClassFieldName, downcastFieldMap.get(i))
              .addStatement("return")
              .endControlFlow()
              .addCode("\n");

          final TypeName downcastedClassName = ClassName.get(downcastFieldMap.get(i));
          final String downcastedClassFieldName = getClassFieldName(downcastedClassName);

          customSetterMethodBuilder.addStatement(
              "$T $L = ($T)$L",
              downcastedClassName,
              downcastedClassFieldName,
              downcastedClassName,
              returnedClassFieldName);

          classElement = downcastFieldMap.get(i).asElement();
          classElementFieldName = downcastedClassFieldName;
        } else {
          customSetterMethodBuilder
              .beginControlFlow(
                  "if(null == $L.$L)", classElementFieldName, getterMethodElement.get().toString())
              .addStatement(
                  "$L.$L(new $T())",
                  classElementFieldName,
                  setterMethodElement.get().getSimpleName().toString(),
                  ClassName.get(returnedClassElement.asType()))
              .endControlFlow()
              .addCode("\n")
              .addStatement(
                  "$T $L = $L.$L",
                  ClassName.get(returnedClassElement.asType()),
                  returnedClassFieldName,
                  classElementFieldName,
                  getterMethodElement.get().toString());

          classElement = returnedClassElement;
          classElementFieldName = returnedClassFieldName;
        }
      } else {
        // Getter and Setter for the element is not found.
        break;
      }
    }

    final String lastField = nestedFields[nestedFields.length - 1];
    final String setterMethodName =
        "set" + lastField.substring(0, 1).toUpperCase() + lastField.substring(1);
    final Optional<? extends Element> setterMethodElement =
        classElement.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.METHOD)
            .filter(element -> element.getSimpleName().toString().startsWith(setterMethodName))
            .findFirst();
    if (setterMethodElement.isPresent()) {
      final TypeName finalReturnType =
          ClassName.get(
              ((ExecutableType) setterMethodElement.get().asType()).getParameterTypes().get(0));
      customSetterMethodBuilder.addStatement(
          "$L.$L(($T) value)",
          classElementFieldName,
          setterMethodElement.get().getSimpleName().toString(),
          finalReturnType);

      final String mapperClassFieldName = getClassFieldName(ClassName.get(mapperClassType));
      typeSpecBuilder.addMethod(
          customSetterMethodBuilder
              .addParameters(
                  Arrays.asList(
                      ParameterSpec.builder(ClassName.get(mapperClassType), mapperClassFieldName)
                          .build(),
                      ParameterSpec.builder(finalReturnType, "value").build()))
              .build());

      methodBuilder.addCode("\n");
      methodBuilder.addComment("Setter for field $L", nestedFieldName);
      methodBuilder.addStatement(
          "this.$N.put($S, ($L, value) -> { if(value != null) this.$L($L, ($T) value); })",
          setterValueMapField,
          nestedFieldName,
          mapperClassFieldName,
          customSetterMethodName,
          mapperClassFieldName,
          finalReturnType);
    }
  }

  private static String getClassFieldName(final TypeName mapperClassName) {
    final String canonicalName = mapperClassName.toString();
    final int lastDot = canonicalName.lastIndexOf('.');
    return canonicalName.substring(lastDot + 1, lastDot + 2).toLowerCase()
        + canonicalName.substring(lastDot + 2);
  }

  private static String getFieldNameFromMethod(final ExecutableElement methodElement) {
    final String canonicalName = methodElement.getSimpleName().toString().substring(3);
    return canonicalName.substring(0, 1).toLowerCase() + canonicalName.substring(1);
  }
}
