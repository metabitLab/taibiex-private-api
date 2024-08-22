package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolDayDataProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolDayDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.PoolDayData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.PoolDayData_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.PoolDayData_orderBy;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.springframework.stereotype.Service;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolDayDatasGraphQLQuery;

import java.util.List;

@Service
public class PoolDayDataSubgraphFetcher {

    private final SubgraphsClient subgraphsClient;

    public PoolDayDataSubgraphFetcher(SubgraphsClient subgraphsClient) {
        this.subgraphsClient = subgraphsClient;
    }

    public List<PoolDayData> poolDayDatas(Integer skip, Integer first,
                                          PoolDayData_orderBy orderBy,
                                          OrderDirection orderDirection,
                                          PoolDayData_filter where) {

        PoolDayDatasGraphQLQuery qlQuery = PoolDayDatasGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("poolDayDatas")
                .build();

        PoolDayDatasProjectionRoot<?, ?> projection = new PoolDayDatasProjectionRoot<>()
                .id()
                .date()
                .close()
                .high()
                .low()
                .open()
                .volumeToken0()
                .volumeToken1()
                .volumeUSD()
                .tvlUSD()
                .sqrtPrice()
                .tick()
                .token0Price()
                .token1Price()
                .feeGrowthGlobal0X128()
                .feeGrowthGlobal1X128()
               .feesUSD()
                .liquidity()
                .sqrtPrice();

        GraphQLQueryRequest request = new GraphQLQueryRequest(qlQuery, projection);

        GraphQLResponse graphqlResponse = subgraphsClient.build().executeQuery(request.serialize());

        return graphqlResponse.extractValueAsObject("poolDayDatas", new TypeRef<List<PoolDayData>>() {});

    }

}
