package com.privateapi.entity;

import java.util.Map;

public class QueryRequest {
  private String query;
  private String operationName;
  private Map<String, Object> variables;

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }
  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }
  /**
   * @return the operationName
   */
  public String getOperationName() {
    return operationName;
  }
  /**
   * @param operationName the operationName to set
   */
  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }
  /**
   * @return the variables
   */
  public Map<String, Object> getVariables() {
    return variables;
  }
  /**
   * @param variables the variables to set
   */
  public void setVariables(Map<String, Object> variables) {
    this.variables = variables;
  }
}
