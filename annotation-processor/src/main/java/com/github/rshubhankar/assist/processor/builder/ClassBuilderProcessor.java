package com.github.rshubhankar.assist.processor.builder;

import com.github.rshubhankar.assist.annotation.builder.ClassBuilder;
import com.github.rshubhankar.assist.processor.accessor.GoogleFormatter;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ClassBuilderProcessor extends AbstractProcessor {

  private Messager messager;

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.messager = processingEnv.getMessager();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(ClassBuilder.class.getName());
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final Set<? extends Element> annotatedElements =
        roundEnv.getElementsAnnotatedWith(ClassBuilder.class);

    for (final Element annotatedElement : annotatedElements) {
      final List<Element> setters =
          annotatedElement.getEnclosedElements().stream()
              .filter(element -> element.getKind() == ElementKind.METHOD)
              .filter(
                  element ->
                      ((ExecutableType) element.asType()).getParameterTypes().size() == 1
                          && element.getSimpleName().toString().startsWith("set"))
              .collect(Collectors.toList());

      if (setters.isEmpty()) {
        this.messager.printMessage(
            Diagnostic.Kind.WARNING,
            "No setter methods found in Class "
                + annotatedElement
                + " with @ClassBuilder annotation");
        return false;
      }

      final Map<String, MethodElementInfo> setterMap =
          setters.stream()
              .collect(
                  Collectors.toMap(
                      setter -> setter.getSimpleName().toString(),
                      setter -> {
                        final MethodElementInfo methodElementInfo = new MethodElementInfo();
                        methodElementInfo.name = setter.getSimpleName().toString();
                        final List<? extends TypeMirror> parameterTypes =
                            ((ExecutableType) setter.asType()).getParameterTypes();
                        final List<? extends VariableElement> variableNames =
                            ((ExecutableElement) setter).getParameters();
                        final List<ParameterElementInfo> parameterElementInfos = new ArrayList<>();
                        for (int i = 0; i < parameterTypes.size(); i++) {
                          parameterElementInfos.add(
                              new ParameterElementInfo(
                                  ClassName.get(parameterTypes.get(i)),
                                  variableNames.get(i).getSimpleName().toString()));
                        }
                        methodElementInfo.parameterTypeElementInfos = parameterElementInfos;
                        methodElementInfo.returnType = TypeName.get(annotatedElement.asType());
                        return methodElementInfo;
                      }));

      try {
        this.writeBuilderFile((TypeElement) annotatedElement, setterMap);
      } catch (final Exception e) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, "Unable to write the file");
        throw new RuntimeException(e);
      }
    }

    return false;
  }

  private void writeBuilderFile(
      final TypeElement classElement, final Map<String, MethodElementInfo> setterMap)
      throws Exception {
    final ClassName className = ClassName.get(classElement);
    String packageName = "";
    final String canonicalName = className.canonicalName();
    final int lastDot = canonicalName.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = canonicalName.substring(0, lastDot);
    }

    final String fieldName =
        canonicalName.substring(lastDot + 1, lastDot + 2).toLowerCase()
            + canonicalName.substring(lastDot + 2);
    final String fieldClassName = canonicalName.substring(lastDot + 1);
    final String builderClassName = canonicalName + "Builder";
    final String builderSimpleClassName = builderClassName.substring(lastDot + 1);

    final FieldSpec fieldSpec =
        FieldSpec.builder(TypeName.get(classElement.asType()), fieldName)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build();

    final MethodSpec constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addStatement("this.$L = new $L()", fieldName, fieldClassName)
            .build();

    final TypeSpec.Builder typeBuilder =
        TypeSpec.classBuilder(builderSimpleClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(fieldSpec)
            .addMethod(constructor);

    final TypeName methodBuilderReturnType = ClassName.get(packageName, builderSimpleClassName);

    setterMap.forEach(
        (name, methodElementInfo) -> {
          final ParameterElementInfo param = methodElementInfo.parameterTypeElementInfos.get(0);
          typeBuilder.addMethod(
              MethodSpec.methodBuilder(name)
                  .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                  .returns(methodBuilderReturnType)
                  .addParameter(ParameterSpec.builder(param.type, param.name).build())
                  .addStatement("this.$N.$N($N)", fieldSpec, name, param.name)
                  .addStatement("return this")
                  .build());
        });

    final TypeSpec typeSpec = typeBuilder.build();

    final JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
    // javaFile.writeTo(this.processingEnv.getFiler());
    final StringBuilder stringBuilder = new StringBuilder();
    javaFile.writeTo(stringBuilder);
    final Filer filer = this.processingEnv.getFiler();
    final JavaFileObject sourceFile =
        filer.createSourceFile(packageName + '.' + builderSimpleClassName);
    try (final Writer writer = sourceFile.openWriter()) {
      writer.write(GoogleFormatter.formatSource(stringBuilder.toString()));
    }
  }
}
