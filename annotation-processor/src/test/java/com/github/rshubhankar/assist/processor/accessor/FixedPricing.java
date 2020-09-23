package com.github.rshubhankar.assist.processor.accessor;

public class FixedPricing extends Pricing {
  private Double price;
  private Data currency;
  private Data uom;

  public FixedPricing() {
    super("Fixed");
  }

  public Double getPrice() {
    return this.price;
  }

  public void setPrice(final Double price) {
    this.price = price;
  }

  public Data getCurrency() {
    return this.currency;
  }

  public void setCurrency(final Data currency) {
    this.currency = currency;
  }

  public Data getUom() {
    return this.uom;
  }

  public void setUom(final Data uom) {
    this.uom = uom;
  }
}
