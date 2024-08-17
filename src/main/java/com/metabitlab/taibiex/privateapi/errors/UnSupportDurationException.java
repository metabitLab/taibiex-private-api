package com.metabitlab.taibiex.privateapi.errors;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.HistoryDuration;

public class UnSupportDurationException extends RuntimeException {
  
  private HistoryDuration duration;

  /**
   * @return the duration
   */
  public HistoryDuration getDuration() {
    return duration;
  }

  public UnSupportDurationException(String message, HistoryDuration duration) {
    super(message);
    this.duration = duration;
  }
}
