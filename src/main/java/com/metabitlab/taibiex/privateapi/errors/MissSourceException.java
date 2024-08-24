package com.metabitlab.taibiex.privateapi.errors;

/**
 * This exception is thrown when there is a missing source context.
 * 
 * @author nix
 */
public class MissSourceException extends RuntimeException {
    private String sourceDescription;

    /**
     * @return the sourceDescription
     */
    public String getSourceDescription() {
        return sourceDescription;
    }

    public MissSourceException(String message, String sourceDescription) {
        super(message);
        this.sourceDescription = sourceDescription;
    }
}
