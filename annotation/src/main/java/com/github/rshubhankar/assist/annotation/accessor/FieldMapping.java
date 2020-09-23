package com.github.rshubhankar.assist.annotation.accessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Repeatable(FieldMappings.class)
public @interface FieldMapping {
  /**
   * Field Identifier
   *
   * @return field name
   */
  String fieldName();

  /**
   * Name of the field present in the Class. This value needs to match with the attribute in the
   * Class. If not then getter and setter mapping will not be created for the field.
   *
   * @return field name present in class
   */
  String classFieldName() default "";

  /**
   * Map the value getter of the field with a Custom getter method defined in the Value Accessor
   * class. If method is not found then mapping will not be done. Also, this method should have only
   * one parameter and the type of the parameter should match with the type of class mentioned in
   * {@link ClassMapping}.
   *
   * <p>If both customGetterMethod and classFieldName is set, then priority is given to
   * customGetterMethod.
   *
   * @return custom getter method name
   */
  String customGetterMethod() default "";

  /**
   * Map the value setter of the field with a Custom setter method defined in the Value Accessor
   * class. If method is not found then mapping will not be done. Also, this method should have only
   * two parameters. The first parameter type should match with the type of class mentioned in
   * {@link ClassMapping}, and the second parameter should match with the type of value to be set.
   *
   * <p>If both customSetterMethod and classFieldName is set, then priority is given to
   * customSetterMethod.
   *
   * @return custom setter method name
   */
  String customSetterMethod() default "";

  DowncastField[] downcastFieldsAtIndex() default {};
}
