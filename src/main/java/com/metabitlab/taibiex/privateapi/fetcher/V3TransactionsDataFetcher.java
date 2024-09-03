package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson2.JSON;
import com.metabitlab.taibiex.privateapi.errors.ParseCacheException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportChainException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PoolTransaction;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TransactionsSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.util.RedisService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

/**
 * This class handles fetching V3 transactions data.
 * 
 * @author: nix
 */
@DgsComponent
public class V3TransactionsDataFetcher {
    @Autowired
    TransactionsSubgraphFetcher transactionsSubgraphFetcher;

    @Autowired
    RedisService redisService;

    private final Chain TABI = Chain.ETHEREUM;

    /**
     * The time-to-live for the V3 transactions cache in seconds.
     */
    private final int V3TRANSACTIONS_CACHE_TTL = 10;

    @DgsQuery
    public List<PoolTransaction> v3Transactions(
        @InputArgument Chain chain,
        @InputArgument Integer first,
        @InputArgument("timestampCursor") Integer cursor
    ) {
        // NOTE: [已确认] 参数 chain 未使用, 仅支持 TABI 
        if (chain != TABI) {
            throw new UnSupportChainException("Those chains are not supported", Arrays.asList(chain));
        }

        final String addCacheKey = "v3Transactions:" + chain + ":add";
        final String removeCacheKey = "v3Transactions:" + chain + ":remove";
        final String swapsCacheKey = "v3Transactions:" + chain + ":swaps";

        try {
            List<PoolTransaction> addList = null;
            Object wrappedAddList = redisService.get(addCacheKey);
            if (wrappedAddList != null) {
                addList = JSON.parseArray((String) wrappedAddList, PoolTransaction.class);
            } else {
                addList = transactionsSubgraphFetcher.mintsTransactions(0, first, cursor, chain, null);
                redisService.set(addCacheKey, JSON.toJSONString(addList), V3TRANSACTIONS_CACHE_TTL, TimeUnit.SECONDS);
            }

            List<PoolTransaction> removeList = null;
            Object wrappedRemoveList = redisService.get(removeCacheKey);
            if (wrappedRemoveList != null) {
                removeList = JSON.parseArray((String) wrappedRemoveList, PoolTransaction.class);
            } else {
                removeList = transactionsSubgraphFetcher.burnsTransactions(0, first, cursor, chain, null);
                redisService.set(removeCacheKey, JSON.toJSONString(removeList), V3TRANSACTIONS_CACHE_TTL, TimeUnit.SECONDS);
            }

            List<PoolTransaction> swapsList = null;
            Object wrappedSwapsList = redisService.get(swapsCacheKey);
            if (wrappedSwapsList != null) {
                swapsList = JSON.parseArray((String)wrappedSwapsList, PoolTransaction.class);
            } else {
                swapsList = transactionsSubgraphFetcher.swapsTransactions(0, first, cursor, chain, null);
                redisService.set(swapsCacheKey, JSON.toJSONString(swapsList), V3TRANSACTIONS_CACHE_TTL, TimeUnit.SECONDS);
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
            throw new ParseCacheException("Error parsing cache with chain", TABI.name());
        }
    }
}
