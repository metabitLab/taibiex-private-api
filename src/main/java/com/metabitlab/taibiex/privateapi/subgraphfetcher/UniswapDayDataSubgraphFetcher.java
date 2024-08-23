package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.UniswapDayDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.UniswapDayDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData_orderBy;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

@Service
public class UniswapDayDataSubgraphFetcher {
    @Autowired
    SubgraphsClient subgraphsClient;

    public List<UniswapDayData> dayDataList(
            Integer skip,
            Integer first,
            UniswapDayData_orderBy orderBy,
            OrderDirection orderDirection,
            UniswapDayData_filter where) {

        UniswapDayDatasGraphQLQuery query = UniswapDayDatasGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("UniswapDayDataSubgraphFetcher_uniswapDayDatas")
                .build();

        UniswapDayDatasProjectionRoot projection = new UniswapDayDatasProjectionRoot()
                .id()
                .date()
                .volumeETH()
                .volumeUSD()
                .volumeUSDUntracked()
                .feesUSD()
                .txCount()
                .tvlUSD();

        GraphQLQueryRequest request = new GraphQLQueryRequest(query, projection);
        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        return response.extractValueAsObject("uniswapDayDatas", new TypeRef<List<UniswapDayData>>() {
        });
    }
}
