package com.metabitlab.taibiex.privateapi.fetcher;

import com.metabitlab.taibiex.privateapi.errors.MissSourceException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.service.V3PoolService;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TransactionsSubgraphFetcher;
import com.netflix.graphql.dgs.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

@DgsComponent
public class V3PoolDataFetcher {

    private final V3PoolService v3PoolService;

    public V3PoolDataFetcher(V3PoolService v3PoolService) {
        this.v3PoolService = v3PoolService;
    }

    @Autowired
    TransactionsSubgraphFetcher transactionsSubgraphFetcher;

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
    public List<PoolTransaction> transactions(
        @InputArgument Integer first,
        @InputArgument("timestampCursor") Integer cursor,
        DgsDataFetchingEnvironment env
    ) {
        V3Pool pool = env.getSource();
        if (pool == null) {
            throw new MissSourceException("V3Pool is required", "V3Pool");
        }

        Chain chain = pool.getChain();
        String address = pool.getAddress();

        // NOTE: 将 V3Pool 的地址作为 Subgraphs 中 Pool 的 ID 使用
        List<PoolTransaction> addList = transactionsSubgraphFetcher.mintsTransactions(0, first, cursor, chain, address);
        List<PoolTransaction> removeList = transactionsSubgraphFetcher.burnsTransactions(0, first, cursor, chain, address);
        List<PoolTransaction> swapsList = transactionsSubgraphFetcher.swapsTransactions(0, first, cursor, chain, address);
        
        return Stream.of(addList, removeList, swapsList)
                .flatMap(List::stream)
                .sorted(new Comparator<PoolTransaction>() {
                    @Override
                    public int compare(PoolTransaction o1, PoolTransaction o2) {
                        return o2.getTimestamp() - o1.getTimestamp();
                    }
                })
                .limit(first)
                .toList();
    }

    @DgsData(parentType = DgsConstants.V3POOL.TYPE_NAME)
    public List<V3PoolTick> ticks(@InputArgument Integer first, DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool = env.getSource();
        return v3PoolService.getV3PoolTicks(pool.getAddress(), first);
    }
}
