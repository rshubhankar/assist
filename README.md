Java Field Assist
=====

## Summary
Java Field Assist is a small library, to reduce writing of boilerplate code.

## Requirements
* Java 8 or higher.
* Maven 3.6.x

## Build Status
[![Build Status](https://travis-ci.com/rshubhankar/assist.svg?branch=master)](https://travis-ci.com/github/rshubhankar/assist/builds)

## Dependecies
* Add dependency to your project:
```xml
<dependency>
  <groupId>com.github.rshubhankar.assist</groupId>
  <artifactId>annotation</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

* Add annotation processor in maven compiler plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
      <annotationProcessorPaths>
        <path>
          <groupId>com.github.rshubhankar.assist</groupId>
          <artifactId>annotation-processor</artifactId>
          <version>1.0.0-SNAPSHOT</version>
        </path>
      </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Sample Code
#### @ClassBuilder
Use ClassBuilder annotation to generate Builder classes.

```java
@ClassBuilder
public class Employee {
  private int empId;
  private String name;

  public int getEmpId() {
    return empId;
  }

  public void setEmpId(int empId) {
    this.empId = empId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
```
The annotation processor will generate a EmployeeBuilder class in the same package.

#### @ClassMapping, @ClassValueGetter, @ClassValueSetter and @FieldMapping

Use ClassMapping annotation on interface or abstract classes, use ClassValueGetter and ClassValueSetter
on getter and setter methods, and add FieldMapping over the same getter method. This will generate code
to access value of Domain/POJO by using a unique field name.

```java
@ClassMapping(mapperClass = Employee.class)
public interface EmployeeValueAccessor {
  @FieldMapping(fieldName = "EmployeeId", classFieldName ="empId")
  @FieldMapping(fieldName = "EmployeeName", classFieldName ="name")
  @ClassValueGetter
  Object getValue(Employee employee, String field);

  @ClassValueSetter
  void setValue(Employee employee, String field, Object value);
}
```

The above annotations will generate an implementation with getValue(), to get value of field from the domain 
and setValue() to set value of field in domain.

#### @ClassAttribute
Use ClassAttribute annotation to generate a class which contains singular attributes of Domain.

```java
@ClassAttribute
public class Employee {
  private int empId;
  private String name;

  public int getEmpId() {
    return empId;
  }

  public void setEmpId(int empId) {
    this.empId = empId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
```
The annotation will generate a class EmployeeSingularAttributes with Constants. Each constant 
represents a field of Domain class, in this case, Employee class.
