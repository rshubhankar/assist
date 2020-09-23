package com.github.rshubhankar.assist.processor.accessor;

import java.time.LocalDate;

public class Delivery {
  private LocalDate fromDate;
  private LocalDate toDate;

  public LocalDate getFromDate() {
    return this.fromDate;
  }

  public void setFromDate(final LocalDate fromDate) {
    this.fromDate = fromDate;
  }

  public LocalDate getToDate() {
    return this.toDate;
  }

  public void setToDate(final LocalDate toDate) {
    this.toDate = toDate;
  }
}
