package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

import com.metabitlab.taibiex.privateapi.errors.UnSupportChainException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import static com.metabitlab.taibiex.privateapi.util.Constants.TABI;

/**
 * This is the IsV3SubgraphStaleDataFetcher class.
 * 
 * @author: nix
 */
@DgsComponent
public class IsV3SubgraphStaleDataFetcher {
    @Autowired
    BundleSubgraphFetcher bundleSubgraphFetcher;

    @DgsQuery
    public Boolean isV3SubgraphStale(
        @InputArgument Chain chain
    ) {
        if (chain == null) {
            throw new IllegalArgumentException("Chain is required");
        }
        if (chain != TABI) {
            throw new UnSupportChainException("Chain is not supported", Arrays.asList(chain));
        }

        // NOTE: [已确认] 参数 chain 未使用, 仅支持 TABI 

        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle bundle = bundleSubgraphFetcher.bundle();

        // NOTE: [已确认] 如果 bundle 不为空，则为 true
        return bundle != null;
    }
}
