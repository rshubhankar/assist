package com.github.rshubhankar.assist.processor.accessor;

import com.google.auto.service.AutoService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import com.github.rshubhankar.assist.annotation.accessor.ClassMapping;
import com.github.rshubhankar.assist.annotation.accessor.ClassValueGetter;
import com.github.rshubhankar.assist.annotation.accessor.ClassValueSetter;
import com.github.rshubhankar.assist.annotation.accessor.FieldMapping;
import com.github.rshubhankar.assist.annotation.accessor.FieldMappings;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ClassFieldMappingProcessor extends AbstractProcessor {

  private Messager messager;
  private JavaFileCreatorForClassFieldMapping javaFileCreator;

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.messager = processingEnv.getMessager();
    this.javaFileCreator = new JavaFileCreatorForClassFieldMapping(this.messager, processingEnv);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    final Set<String> supportedAnnotationTypes = new HashSet<>();
    supportedAnnotationTypes.add(ClassMapping.class.getName());
    supportedAnnotationTypes.add(FieldMapping.class.getName());
    return supportedAnnotationTypes;
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final Set<? extends Element> annotatedElements =
        roundEnv.getElementsAnnotatedWith(ClassMapping.class);

    for (final Element annotatedElement : annotatedElements) {
      final List<? extends Element> enclosedElements = annotatedElement.getEnclosedElements();

      final List<Element> getters =
          enclosedElements.stream()
              .filter(
                  element ->
                      element.getAnnotation(ClassValueGetter.class) != null
                          && (element.getAnnotation(FieldMappings.class) != null
                              || element.getAnnotation(FieldMapping.class) != null))
              .collect(Collectors.toList());
      final List<Element> setters =
          enclosedElements.stream()
              .filter(element -> element.getAnnotation(ClassValueSetter.class) != null)
              .collect(Collectors.toList());

      if (getters.size() != 1 || setters.size() != 1) {
        this.messager.printMessage(
            Diagnostic.Kind.WARNING,
            "@ClassValueGetter and @ClassValueSetter should be annotated to one and only one method.");
        return false;
      }

      try {
        this.javaFileCreator.createJavaFile(annotatedElement, getters, setters);
      } catch (final Exception e) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, "Unable to write the file");
        throw new RuntimeException(e);
      }
    }

    return false;
  }
}
