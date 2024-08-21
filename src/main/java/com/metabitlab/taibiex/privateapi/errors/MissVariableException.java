package com.metabitlab.taibiex.privateapi.errors;

/**
 * This class represents an exception for missing variables.
 * 
 * @author Nix
 */
public class MissVariableException extends RuntimeException {
  
  private String variableName;

  /**
   * @return the duration
   */
  public String getVariable() {
    return variableName;
  }

  public MissVariableException(String message, String variableName) {
    super(message);
    this.variableName = variableName;
  }
}
