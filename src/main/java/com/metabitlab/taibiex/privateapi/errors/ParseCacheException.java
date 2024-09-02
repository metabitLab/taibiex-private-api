package com.metabitlab.taibiex.privateapi.errors;

/**
 * This exception is thrown when there is a problem with the cache.
 * 
 * @author nix
 */
public class ParseCacheException extends RuntimeException {
    private String cacheKey;

    /**
     * @return the cacheKey
     */
    public String getCacheKey() {
        return cacheKey;
    }

    public ParseCacheException(String message, String cacheKey) {
        super(message);
        this.cacheKey = cacheKey;
    }
}
