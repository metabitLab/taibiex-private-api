package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.*;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenMinuteDataSubgraphFetcher {

    private final SubgraphsClient subgraphsClient;

    public TokenMinuteDataSubgraphFetcher(SubgraphsClient subgraphsClient) {
        this.subgraphsClient = subgraphsClient;
    }

    public List<TokenMinuteData> tokenMinuteDatas(Integer skip, Integer first,
                                                  TokenMinuteData_orderBy orderBy,
                                                  OrderDirection orderDirection,
                                                  TokenMinuteData_filter where) {
        TokenMinuteDatasGraphQLQuery query = TokenMinuteDatasGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("tokenMinuteDatas")
                .build();

        TokenMinuteDatasProjectionRoot<?, ?> projection = new TokenMinuteDatasProjectionRoot<>()
                .id().close().totalValueLocked().totalValueLockedUSD().priceUSD().high().low().open().periodStartUnix().volume().volumeUSD().feesUSD().untrackedVolumeUSD();

        GraphQLQueryRequest request = new GraphQLQueryRequest(query, projection);

        GraphQLResponse graphqlResponse = subgraphsClient.build().executeQuery(request.serialize());
        return graphqlResponse.extractValueAsObject("tokenMinuteDatas",  new TypeRef<List<TokenMinuteData>>() {});
    }

    public TokenMinuteData tokenMinuteData(String id) {
        TokenMinuteDataGraphQLQuery query = TokenMinuteDataGraphQLQuery.newRequest()
                .id(id)
                .queryName("tokenMinuteData")
                .build();

        TokenMinuteDataProjectionRoot<?, ?> projection = new TokenMinuteDataProjectionRoot<>()
                .id().close().totalValueLocked().totalValueLockedUSD().priceUSD().high().low().open().periodStartUnix().volume().volumeUSD().feesUSD().untrackedVolumeUSD();

        GraphQLQueryRequest request = new GraphQLQueryRequest(query, projection);

        GraphQLResponse graphqlResponse = subgraphsClient.build().executeQuery(request.serialize());
        return graphqlResponse.extractValueAsObject("tokenMinuteData",  TokenMinuteData.class);
    }
}
