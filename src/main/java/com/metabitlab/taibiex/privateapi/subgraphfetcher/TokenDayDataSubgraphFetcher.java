package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenDayDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenDayDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenHourDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenHourDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenDayDataSubgraphFetcher {

    private final SubgraphsClient subgraphsClient;

    public TokenDayDataSubgraphFetcher(SubgraphsClient subgraphsClient) {
        this.subgraphsClient = subgraphsClient;
    }

    public List<TokenDayData> tokenDayDatas(Integer skip, Integer first,
                                            TokenDayData_orderBy orderBy,
                                            OrderDirection orderDirection,
                                            TokenDayData_filter where) {
        TokenDayDatasGraphQLQuery query = TokenDayDatasGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("tokenDayDatas")
                .build();

        TokenDayDatasProjectionRoot<?, ?> projection = new TokenDayDatasProjectionRoot<>()
                .id().close().totalValueLocked().totalValueLockedUSD().priceUSD().high().low().open().volume()
                .volumeUSD().feesUSD().untrackedVolumeUSD().date();

        GraphQLQueryRequest request = new GraphQLQueryRequest(query, projection);

        GraphQLResponse graphqlResponse = subgraphsClient.build().executeQuery(request.serialize());
        return graphqlResponse.extractValueAsObject("tokenDayDatas",  new TypeRef<List<TokenDayData>>() {});
    }

    public TokenDayData tokenDayData(String id) {
        TokenDayDatasGraphQLQuery query = TokenDayDatasGraphQLQuery.newRequest()
                .where(TokenDayData_filter.newBuilder().id(id).build())
                .queryName("tokenDayData")
                .build();

        TokenDayDatasProjectionRoot<?, ?> projection = new TokenDayDatasProjectionRoot<>()
                .id().close().totalValueLocked().totalValueLockedUSD().priceUSD().high().low().open().volume()
                .volumeUSD().feesUSD().untrackedVolumeUSD().date();

        GraphQLQueryRequest request = new GraphQLQueryRequest(query, projection);

        GraphQLResponse graphqlResponse = subgraphsClient.build().executeQuery(request.serialize());
        return graphqlResponse.extractValueAsObject("tokenDayData",  TokenDayData.class);
    }
}
