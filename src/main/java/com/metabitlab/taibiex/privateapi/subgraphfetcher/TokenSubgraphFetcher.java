package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import com.alibaba.fastjson.JSON;
import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokensGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokensProjectionRoot;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenSubgraphFetcher {

    private final SubgraphsClient subgraphsClient;

    public TokenSubgraphFetcher(SubgraphsClient subgraphsClient) {
        this.subgraphsClient = subgraphsClient;
    }

    public List<Token> tokens(Integer skip, Integer first, Token_orderBy orderBy, OrderDirection orderDirection, Token_filter where){
        TokensGraphQLQuery query = TokensGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("tokens")
                .build();

        TokensProjectionRoot projection = new TokensProjectionRoot()
                .id().decimals().name().symbol().totalSupply().derivedETH()
                .totalValueLocked().totalValueLockedUSD().totalValueLockedUSDUntracked()
                .txCount().untrackedVolumeUSD().volume().volumeUSD().poolCount();

        GraphQLQueryRequest request = new GraphQLQueryRequest(query, projection);

        GraphQLResponse graphQLResponse = subgraphsClient.build().executeQuery(request.serialize());
        String tokens = JSON.toJSONString(graphQLResponse.extractValue("tokens"));
        return graphQLResponse.extractValueAsObject("tokens",  new TypeRef<List<Token>>() {});
    }

    public List<Pool> pools(int skip, int first) {
      PoolsGraphQLQuery poolsQuery = PoolsGraphQLQuery.newRequest()
        .skip(skip)
        .first(first)
        .queryName("TokenDataFetcher")
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
