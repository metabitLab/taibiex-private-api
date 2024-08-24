package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PoolTransaction;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TransactionsSubgraphFetcher;
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

    private final Chain TABI = Chain.TABI;

    @DgsQuery
    public List<PoolTransaction> v3Transactions(
        @InputArgument Chain chain,
        @InputArgument Integer first,
        @InputArgument("timestampCursor") Integer cursor
    ) {
        // NOTE: 忽略了 chain 参数

        List<PoolTransaction> addList = transactionsSubgraphFetcher.mintsTransactions(0, first, cursor, TABI, null);
        List<PoolTransaction> removeList = transactionsSubgraphFetcher.burnsTransactions(0, first, cursor, TABI, null);
        List<PoolTransaction> swapsList = transactionsSubgraphFetcher.swapsTransactions(0, first, cursor, TABI, null);
        
        return Stream.of(addList, removeList, swapsList)
                .flatMap(List::stream)
                .toList();
    }
}
