package com.metabitlab.taibiex.privateapi.errors;

import java.util.List;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;

public class UnSupportCurrencyException extends RuntimeException {
  private List<Currency> currencies;

  /**
   * @return the currency
   */
  public List<Currency> getCurrencies() {
    return currencies;
  }

  public UnSupportCurrencyException(String message, List<Currency> currency) {
    super(message);
    this.currencies = currency;
  }
}
