package com.github.rshubhankar.assist.processor;

import com.github.rshubhankar.assist.annotation.builder.ClassBuilder;

@ClassBuilder
public class Trade {
  private String tradeId;
  private double quantity;

  public String getTradeId() {
    return tradeId;
  }

  public void setTradeId(String tradeId) {
    this.tradeId = tradeId;
  }

  public double getQuantity() {
    return quantity;
  }

  public void setQuantity(double quantity) {
    this.quantity = quantity;
  }
}
