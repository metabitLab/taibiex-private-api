package com.metabitlab.taibiex.privateapi.errors;

/**
 * This class represents an exception for an unknown token.
 * 
 * @author: nix
 */
public class UnKnownTokenException extends RuntimeException {
    private String token;
    
    /**
    * @return the token
    */
    public String getToken() {
        return token;
    }
    
    public UnKnownTokenException(String message, String token) {
        super(message);
        this.token = token;
    }
}
