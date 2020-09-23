package com.github.rshubhankar.assist.processor.accessor;

public class IndexPricing extends Pricing {
  private Data index;

  public IndexPricing() {
    super("Index");
  }

  public Data getIndex() {
    return this.index;
  }

  public void setIndex(final Data index) {
    this.index = index;
  }
}
