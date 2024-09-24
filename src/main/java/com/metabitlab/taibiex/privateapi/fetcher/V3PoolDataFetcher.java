package com.metabitlab.taibiex.privateapi.fetcher;

import com.alibaba.fastjson2.JSON;
import com.metabitlab.taibiex.privateapi.errors.MissSourceException;
import com.metabitlab.taibiex.privateapi.errors.ParseCacheException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.service.V3PoolService;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TransactionsSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.util.RedisService;
import com.netflix.graphql.dgs.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    RedisService redisService;

    /**
     * The time-to-live for the V3 transactions cache in seconds.
     */
    private final int V3TRANSACTIONS_CACHE_TTL = 10;

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
    public List<TimestampedAmount> historicalVolume(DgsDataFetchingEnvironment env, @InputArgument HistoryDuration duration) {
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

    @DgsData(parentType = DgsConstants.QUERY.TYPE_NAME)
    public List<V3Pool> v3PoolsForTokenPair(@InputArgument Chain chain,
                                   @InputArgument String token0,
                                   @InputArgument String token1) {
        return v3PoolService.v3PoolsForTokenPair(chain, token0, token1);
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

        // 将 V3Pool 的地址作为 Subgraphs 中 Pool 的 ID 使用
        final String addCacheKey = "v3Transactions:" + chain + ":add:" + address;
        final String removeCacheKey = "v3Transactions:" + chain + ":remove:" + address;
        final String swapsCacheKey = "v3Transactions:" + chain + ":swaps:" + address;

        try {
            List<PoolTransaction> addList = null;
            Object wrappedAddList = redisService.get(addCacheKey);
            if (wrappedAddList != null) {
                addList = JSON.parseArray((String) wrappedAddList, PoolTransaction.class);
            } else {
                addList = Optional.ofNullable(transactionsSubgraphFetcher.mintsTransactions(0, first, cursor, chain, address)).orElse(List.of());
                redisService.set(addCacheKey, JSON.toJSONString(addList), V3TRANSACTIONS_CACHE_TTL, TimeUnit.SECONDS);
            }

            List<PoolTransaction> removeList = null;
            Object wrappedRemoveList = redisService.get(removeCacheKey);
            if (wrappedRemoveList != null) {
                removeList = JSON.parseArray((String) wrappedRemoveList, PoolTransaction.class);
            } else {
                removeList = Optional.ofNullable(transactionsSubgraphFetcher.burnsTransactions(0, first, cursor, chain, address)).orElse(List.of());
                redisService.set(removeCacheKey, JSON.toJSONString(removeList), V3TRANSACTIONS_CACHE_TTL,
                        TimeUnit.SECONDS);
            }

            List<PoolTransaction> swapsList = null;
            Object wrappedSwapsList = redisService.get(swapsCacheKey);
            if (wrappedSwapsList != null) {
                swapsList = JSON.parseArray((String) wrappedSwapsList, PoolTransaction.class);
            } else {
                swapsList = Optional.ofNullable(transactionsSubgraphFetcher.swapsTransactions(0, first, cursor, chain, address)).orElse(List.of());
                redisService.set(swapsCacheKey, JSON.toJSONString(swapsList), V3TRANSACTIONS_CACHE_TTL,
                        TimeUnit.SECONDS);
            }

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
        } catch (Exception e) {
            throw new ParseCacheException("Error occurs on transactions cache with chain", chain.name());
        }
    }

    @DgsData(parentType = DgsConstants.V3POOL.TYPE_NAME)
    public List<V3PoolTick> ticks(@InputArgument Integer first, @InputArgument Integer skip, DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool = env.getSource();
        return v3PoolService.getV3PoolTicks(pool.getAddress(), first, skip);
    }
}
