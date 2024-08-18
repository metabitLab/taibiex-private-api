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
public class TokenHourDataSubgraphFetcher {

    private final SubgraphsClient subgraphsClient;

    public TokenHourDataSubgraphFetcher(SubgraphsClient subgraphsClient) {
        this.subgraphsClient = subgraphsClient;
    }

    public List<TokenHourData> tokenHourDatas(Integer skip, Integer first,
                                              TokenHourData_orderBy orderBy,
                                              OrderDirection orderDirection,
                                              TokenHourData_filter where) {
        TokenHourDatasGraphQLQuery query = TokenHourDatasGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("tokenHourDatas")
                .build();

        TokenHourDatasProjectionRoot<?, ?> projection = new TokenHourDatasProjectionRoot<>()
                .id().close().totalValueLocked().totalValueLockedUSD().priceUSD().high().low().open().periodStartUnix().volume().volumeUSD().feesUSD().untrackedVolumeUSD();

        GraphQLQueryRequest request = new GraphQLQueryRequest(query, projection);

        GraphQLResponse graphqlResponse = subgraphsClient.build().executeQuery(request.serialize());
        return graphqlResponse.extractValueAsObject("tokenHourDatas",  new TypeRef<List<TokenHourData>>() {});
    }

    public TokenHourData tokenHourData(String id) {
        TokenHourDataGraphQLQuery query = TokenHourDataGraphQLQuery.newRequest()
                .id(id)
                .queryName("tokenHourData")
                .build();

        TokenHourDataProjectionRoot<?, ?> projection = new TokenHourDataProjectionRoot<>()
                .id().close().totalValueLocked().totalValueLockedUSD().priceUSD().high().low().open().periodStartUnix().volume().volumeUSD().feesUSD().untrackedVolumeUSD();

        GraphQLQueryRequest request = new GraphQLQueryRequest(query, projection);

        GraphQLResponse graphqlResponse = subgraphsClient.build().executeQuery(request.serialize());
        return graphqlResponse.extractValueAsObject("tokenHourData",  TokenHourData.class);
    }
}
