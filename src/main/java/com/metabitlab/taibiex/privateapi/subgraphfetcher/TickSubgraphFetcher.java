package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TicksGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TicksProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TickSubgraphFetcher {

    @Autowired
    SubgraphsClient subgraphsClient;

    public List<Tick> ticks(int skip, int first, Tick_orderBy orderBy, OrderDirection orderDirection, Tick_filter where) {
        TicksGraphQLQuery ticksGraphQLQuery = TicksGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("ticks")
                .build();

        TicksProjectionRoot<?, ?> ticksProjectionRoot = new TicksProjectionRoot<>()
                .id()
                .tickIdx()
                .liquidityGross()
                .liquidityNet()
                .price0()
                .price1()
                .poolAddress()
                .createdAtBlockNumber()
                .createdAtTimestamp();

        GraphQLQueryRequest request = new GraphQLQueryRequest(ticksGraphQLQuery, ticksProjectionRoot);
        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        return response.extractValueAsObject("ticks", new TypeRef<List<Tick>>() {});
    }
}
