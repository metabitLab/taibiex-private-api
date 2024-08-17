package com.metabitlab.taibiex.privateapi.errors;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;

public class UnSupportCurrencyException extends RuntimeException {
  private Currency currency;

  /**
   * @return the currency
   */
  public Currency getCurrency() {
    return currency;
  }

  public UnSupportCurrencyException(String message, Currency currency) {
    super(message);
    this.currency = currency;
  }
}
