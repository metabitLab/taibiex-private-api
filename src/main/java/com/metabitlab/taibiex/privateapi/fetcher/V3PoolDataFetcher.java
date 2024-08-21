package com.metabitlab.taibiex.privateapi.fetcher;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool;
import com.metabitlab.taibiex.privateapi.service.V3PoolService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class V3PoolDataFetcher {

    private final V3PoolService v3PoolService;

    public V3PoolDataFetcher(V3PoolService v3PoolService) {
        this.v3PoolService = v3PoolService;
    }

    @DgsQuery(field = "v3Pool")
    public V3Pool getV3Pool(@InputArgument Chain chain, @InputArgument String address) {
        return v3PoolService.pool(chain, address);
    }
}
