package com.metabitlab.taibiex.privateapi.errors;

/**
 * This exception is thrown when there is a missing local context.
 * 
 * @author nix
 */
public class MissLocalContextException extends RuntimeException {
    private String contextDescription;

    /**
     * @return the contextDescription
     */
    public String getContextDescription() {
        return contextDescription;
    }

    public MissLocalContextException(String message, String contextDescription) {
        super(message);
        this.contextDescription = contextDescription;
    }
}
