package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Pool;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

@Service
public class PoolsSubgraphFetcher {

    @Autowired
    SubgraphsClient subgraphsClient;
  
    public List<Pool> pools(int skip, int first) {
      PoolsGraphQLQuery poolsQuery = PoolsGraphQLQuery.newRequest()
        .skip(skip)
        .first(first)
        .queryName("PoolsSubgraphFetcher_pools")
        .build();

      PoolsProjectionRoot<?, ?> poolsProjection = new PoolsProjectionRoot<>()
        .token0Price()
        .id();

      GraphQLQueryRequest request = new GraphQLQueryRequest(poolsQuery, poolsProjection);
      GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

      List<Pool> pools = response.extractValueAsObject("pools", new TypeRef<List<Pool>>() {});

      return pools;
    }
}
