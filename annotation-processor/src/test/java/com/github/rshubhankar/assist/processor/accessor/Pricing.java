package com.github.rshubhankar.assist.processor.accessor;

public abstract class Pricing {
  private final String pricingType;

  public Pricing(final String pricingType) {
    this.pricingType = pricingType;
  }

  public String getPricingType() {
    return this.pricingType;
  }
}
