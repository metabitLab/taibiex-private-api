package com.metabitlab.taibiex.privateapi.fetcher;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.HistoryDuration;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool;
import com.metabitlab.taibiex.privateapi.service.V3PoolService;
import com.netflix.graphql.dgs.*;

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

    @DgsData(parentType = DgsConstants.V3POOL.TYPE_NAME, field = DgsConstants.V3POOL.CumulativeVolume)
    public Amount cumulativeVolume(@InputArgument HistoryDuration duration, DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool = env.getSource();
        return v3PoolService.cumulativeVolume(pool, duration);
    }
}
