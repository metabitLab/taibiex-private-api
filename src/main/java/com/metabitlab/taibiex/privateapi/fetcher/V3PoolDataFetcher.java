package com.metabitlab.taibiex.privateapi.fetcher;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.service.V3PoolService;
import com.netflix.graphql.dgs.*;

import java.util.List;

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

    @DgsData(parentType = DgsConstants.V3POOL.TYPE_NAME)
    public Amount cumulativeVolume(@InputArgument HistoryDuration duration, DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool = env.getSource();
        return v3PoolService.cumulativeVolume(pool, duration);
    }

    @DgsData(parentType = DgsConstants.V3POOL.TYPE_NAME)
    public List<Amount> historicalVolume(DgsDataFetchingEnvironment env, @InputArgument HistoryDuration duration) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool = env.getSource();
        return v3PoolService.getHistoricalVolume(pool, duration);
    }

    @DgsData(parentType = DgsConstants.V3POOL.TYPE_NAME)
    public List<Amount> priceHistory(DgsDataFetchingEnvironment env, @InputArgument HistoryDuration duration) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool = env.getSource();
        return v3PoolService.getPriceHistory(pool, duration);
    }

    @DgsData(parentType = DgsConstants.V3POOL.TYPE_NAME)
    public Amount totalLiquidityPercentChange24h(DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool = env.getSource();
        return v3PoolService.getTotalLiquidityPercentChange24h(pool);
    }

    @DgsData(parentType = DgsConstants.QUERY.TYPE_NAME)
    public List<V3Pool> topV3Pools(@InputArgument Chain chain,
                                 @InputArgument Integer first,
                                 @InputArgument Float tvlCursor,
                                 @InputArgument String tokenFilter) {
        return v3PoolService.topV3Pools(chain, first, tvlCursor, tokenFilter);
    }

    @DgsData(parentType = DgsConstants.V3POOL.TYPE_NAME)
    public List<V3PoolTick> ticks(@InputArgument Integer first, DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool = env.getSource();
        return v3PoolService.getV3PoolTicks(pool.getAddress(), first);
    }
}
