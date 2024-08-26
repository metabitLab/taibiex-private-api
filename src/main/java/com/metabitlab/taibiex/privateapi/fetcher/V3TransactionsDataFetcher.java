package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.Comparator;
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

    private final Chain TABI = Chain.ETHEREUM;

    @DgsQuery
    public List<PoolTransaction> v3Transactions(
        @InputArgument Chain chain,
        @InputArgument Integer first,
        @InputArgument("timestampCursor") Integer cursor
    ) {
        // NOTE: [已确认] 参数 chain 未使用, 仅支持 TABI 

        List<PoolTransaction> addList = transactionsSubgraphFetcher.mintsTransactions(0, first, cursor, TABI, null);
        List<PoolTransaction> removeList = transactionsSubgraphFetcher.burnsTransactions(0, first, cursor, TABI, null);
        List<PoolTransaction> swapsList = transactionsSubgraphFetcher.swapsTransactions(0, first, cursor, TABI, null);
        
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
}
