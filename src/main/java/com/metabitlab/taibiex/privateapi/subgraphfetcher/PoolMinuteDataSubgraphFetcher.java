package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolMinuteDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolMinuteDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PoolMinuteDataSubgraphFetcher {

    private final SubgraphsClient subgraphsClient;

    public PoolMinuteDataSubgraphFetcher(SubgraphsClient subgraphsClient) {
        this.subgraphsClient = subgraphsClient;
    }

    public List<PoolMinuteData> poolMinuteDatas(Integer skip, Integer first,
                                              PoolMinuteData_orderBy orderBy,
                                              OrderDirection orderDirection,
                                              PoolMinuteData_filter where) {

        PoolMinuteDatasGraphQLQuery qlQuery = PoolMinuteDatasGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("poolMinuteDatas")
                .build();

        PoolMinuteDatasProjectionRoot<?, ?> projection = new PoolMinuteDatasProjectionRoot<>()
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

        return graphqlResponse.extractValueAsObject("poolMinuteDatas", new TypeRef<List<PoolMinuteData>>() {});

    }
}
