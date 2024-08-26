package com.metabitlab.taibiex.privateapi.errors;

import java.util.List;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;

/**
 * This class represents an exception for unsupported chain.
 * 
 * @author nix
 */
public class UnSupportChainException extends RuntimeException {
        
    private List<Chain> chains;
    
    /**
    * @return the chain
    */
    public List<Chain> getChains() {
        return chains;
    }
    
    public UnSupportChainException(String message, List<Chain> chains) {
        super(message);
        this.chains = chains;
    }   
}
