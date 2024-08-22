package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolHourDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolHourDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PoolHourDataSubgraphFetcher {

    private final SubgraphsClient subgraphsClient;

    public PoolHourDataSubgraphFetcher(SubgraphsClient subgraphsClient) {
        this.subgraphsClient = subgraphsClient;
    }

    public List<PoolHourData> poolHourDatas(Integer skip, Integer first,
                                           PoolHourData_orderBy orderBy,
                                           OrderDirection orderDirection,
                                           PoolHourData_filter where) {

        PoolHourDatasGraphQLQuery qlQuery = PoolHourDatasGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("poolHourDatas")
                .build();

        PoolHourDatasProjectionRoot<?, ?> projection = new PoolHourDatasProjectionRoot<>()
                .id()
                .periodStartUnix()
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
                .feesUSD()
                .liquidity()
                .sqrtPrice();

        GraphQLQueryRequest request = new GraphQLQueryRequest(qlQuery, projection);

        GraphQLResponse graphqlResponse = subgraphsClient.build().executeQuery(request.serialize());

        return graphqlResponse.extractValueAsObject("poolHourDatas", new TypeRef<List<PoolHourData>>() {});

    }
}
