package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.ContractInput;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Portfolio;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PortfolioValueModifier;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

/**
 * This class is responsible for fetching portfolios data.
 * 
 * @author: nix
 */
@DgsComponent
public class PortfoliosDataFetcher {
	@DgsQuery
    public List<Portfolio> portfolios(
        @InputArgument List<String> ownerAddresses,
        @InputArgument List<Chain> chains,
        @InputArgument List<ContractInput> lookupTokens,
        @InputArgument List<PortfolioValueModifier> valueModifiers
    ) {
        return null;
    }
}
