package com.github.rshubhankar.assist.processor.accessor;

public class Data {
  private String id;
  private String name;

  public Data() {}

  public Data(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return this.id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }
}
