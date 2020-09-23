package com.github.rshubhankar.assist.processor.accessor;

public class Deal {
  private String dealId;
  private String dealName;
  private Data counterpart;
  private Double quantity;
  private Data trader;
  private Delivery delivery;
  private Cost primarySettlement;

  public String getDealId() {
    return this.dealId;
  }

  public void setDealId(final String dealId) {
    this.dealId = dealId;
  }

  public String getDealName() {
    return this.dealName;
  }

  public void setDealName(final String dealName) {
    this.dealName = dealName;
  }

  public Data getCounterpart() {
    return this.counterpart;
  }

  public void setCounterpart(final Data counterpart) {
    this.counterpart = counterpart;
  }

  public Double getQuantity() {
    return this.quantity;
  }

  public void setQuantity(final Double quantity) {
    this.quantity = quantity;
  }

  public Data getTrader() {
    return this.trader;
  }

  public void setTrader(final Data trader) {
    this.trader = trader;
  }

  public Delivery getDelivery() {
    return this.delivery;
  }

  public void setDelivery(final Delivery delivery) {
    this.delivery = delivery;
  }

  public Cost getPrimarySettlement() {
    return this.primarySettlement;
  }

  public void setPrimarySettlement(final Cost primarySettlement) {
    this.primarySettlement = primarySettlement;
  }
}
