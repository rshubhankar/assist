package com.github.rshubhankar.assist.processor.accessor;

public class Cost {
  private String costId;
  private CostClassification classification;
  private Pricing pricingInfo;

  public String getCostId() {
    return this.costId;
  }

  public void setCostId(final String costId) {
    this.costId = costId;
  }

  public CostClassification getClassification() {
    return this.classification;
  }

  public void setClassification(final CostClassification classification) {
    this.classification = classification;
  }

  public Pricing getPricingInfo() {
    return this.pricingInfo;
  }

  public void setPricingInfo(final Pricing pricingInfo) {
    this.pricingInfo = pricingInfo;
  }
}
