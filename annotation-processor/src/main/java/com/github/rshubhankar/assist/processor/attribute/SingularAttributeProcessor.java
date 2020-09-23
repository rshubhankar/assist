package com.github.rshubhankar.assist.processor.attribute;

import com.github.rshubhankar.assist.processor.accessor.GoogleFormatter;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.beans.Transient;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.StringUtils;
import com.github.rshubhankar.assist.annotation.attribute.ClassAttribute;
import com.github.rshubhankar.assist.attribute.SingularAttribute;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class SingularAttributeProcessor extends AbstractProcessor {

  private Messager messager;

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.messager = processingEnv.getMessager();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(ClassAttribute.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return super.getSupportedSourceVersion();
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final Set<? extends Element> annotatedElements =
        roundEnv.getElementsAnnotatedWith(ClassAttribute.class);
    for (final Element annotatedElement : annotatedElements) {
      final ClassName className = ClassName.get((TypeElement) annotatedElement);
      String packageName = "";
      final String canonicalName = className.canonicalName();
      final int lastDot = canonicalName.lastIndexOf('.');
      if (lastDot > 0) {
        packageName = canonicalName.substring(0, lastDot);
      }

      final String classCannonicalName = canonicalName + "SingularAttributes";
      final String simpleClassname = classCannonicalName.substring(lastDot + 1);

      final Map<String, ExecutableElement> properties =
          this.getProperties((TypeElement) annotatedElement);
      final TypeSpec.Builder typeBuilder =
          TypeSpec.classBuilder(simpleClassname)
              .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

      this.createConstantFields(annotatedElement, Collections.emptyList(), properties, typeBuilder);

      DeclaredType parentClassType =
          (DeclaredType) ((TypeElement) annotatedElement).getSuperclass();
      List<? extends TypeMirror> typeArguments = Collections.emptyList();
      if (parentClassType != null) {
        typeArguments = parentClassType.getTypeArguments();
      }
      while (parentClassType != null
          && !ClassName.get(parentClassType).equals(ClassName.get(Object.class))) {
        final Map<String, ExecutableElement> parentClassProperties =
            this.getProperties((TypeElement) parentClassType.asElement());
        parentClassProperties.keySet().removeIf(properties::containsKey);
        this.createConstantFields(
            parentClassType.asElement(), typeArguments, parentClassProperties, typeBuilder);

        properties.putAll(parentClassProperties);

        parentClassType =
            (DeclaredType) ((TypeElement) parentClassType.asElement()).getSuperclass();
      }

      final TypeSpec typeSpec = typeBuilder.build();

      try {
        final JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
        // javaFile.writeTo(this.processingEnv.getFiler());
        StringBuilder stringBuilder = new StringBuilder();
        javaFile.writeTo(stringBuilder);
        Filer filer = this.processingEnv.getFiler();
        JavaFileObject sourceFile = filer.createSourceFile(packageName + '.' + simpleClassname);
        try (Writer writer = sourceFile.openWriter()) {
          writer.write(GoogleFormatter.formatSource(stringBuilder.toString()));
        }
      } catch (final Exception e) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, "Unable create Class " + className);
      }
    }
    return false;
  }

  private void createConstantFields(
      final Element annotatedElement,
      final List<? extends TypeMirror> typeArguments,
      final Map<String, ? extends Element> properties,
      final TypeSpec.Builder typeBuilder) {
    final TypeName[] typeArgumentNames =
        typeArguments.stream().map(ClassName::get).toArray(TypeName[]::new);

    for (final Map.Entry<String, ? extends Element> entry : properties.entrySet()) {
      final String fieldName = entry.getKey();
      final ExecutableElement element = (ExecutableElement) entry.getValue();
      final TypeName returnTypeName;
      if (element.getReturnType().getKind() == TypeKind.TYPEVAR && !typeArguments.isEmpty()) {
        returnTypeName = typeArgumentNames[0];
      } else {
        returnTypeName = ClassName.get(element.getReturnType());
      }
      final TypeName singularAttribute;
      final TypeName actualClassName;
      if (typeArguments.isEmpty()) {
        actualClassName = ClassName.get(annotatedElement.asType());
        singularAttribute =
            ParameterizedTypeName.get(
                ClassName.get(SingularAttribute.class), actualClassName, returnTypeName);
      } else {
        actualClassName =
            ParameterizedTypeName.get(
                ClassName.get((TypeElement) annotatedElement), typeArgumentNames);
        singularAttribute =
            ParameterizedTypeName.get(
                ClassName.get(SingularAttribute.class), actualClassName, returnTypeName);
      }
      final FieldSpec fieldSpec =
          FieldSpec.builder(singularAttribute, this.getConstantFieldName(fieldName))
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer(
                  "new $T<>($S, $T.class, $T::$L)",
                  SingularAttribute.class,
                  fieldName,
                  returnTypeName,
                  actualClassName,
                  element.getSimpleName().toString())
              .build();
      typeBuilder.addField(fieldSpec);
    }
  }

  private Map<String, ExecutableElement> getProperties(final TypeElement element) {
    final List<? extends Element> enclosedElements = element.getEnclosedElements();

    return enclosedElements.stream()
        .filter(e -> e.getSimpleName().toString().startsWith("get"))
        .filter(e -> e.getKind() == ElementKind.METHOD)
        .filter(e -> Objects.isNull(e.getAnnotation(Transient.class)))
        .filter(e -> ((ExecutableType) e.asType()).getParameterTypes().isEmpty())
        .collect(
            Collectors.toMap(
                e -> this.getFieldName(e.getSimpleName().toString().substring(3)),
                e -> (ExecutableElement) e));
  }

  private String getFieldName(final String name) {
    return name.substring(0, 1).toLowerCase() + name.substring(1);
  }

  private String getConstantFieldName(final String name) {
    return Stream.of(StringUtils.splitByCharacterTypeCamelCase(name))
        .map(StringUtils::upperCase)
        .collect(Collectors.joining("_"));
  }
}
