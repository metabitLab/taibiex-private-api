package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenDayDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenDayDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_filter;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

@Service
public class TokenMarketSubgraphFetcher {
  @Autowired
  SubgraphsClient subgraphsClient;

  public List<TokenDayData> tokenOhlcById(String tokenId) {
    TokenDayDatasGraphQLQuery tokenDayDatasQuery = TokenDayDatasGraphQLQuery.newRequest()
      .first(1)
      .orderBy(TokenDayData_orderBy.date)
      .orderDirection(OrderDirection.desc)
      .where(new TokenDayData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_tokenDayDatas")
      .build();

    TokenDayDatasProjectionRoot<?, ?> tokenDayDatasProjection = new TokenDayDatasProjectionRoot<>()
      .id()
      .date()
      .high()
      .open()
      .close()
      .low();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenDayDatasQuery, tokenDayDatasProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenDayData> datas = response.extractValueAsObject("tokenDayDatas", new TypeRef<List<TokenDayData>>() {});

    return datas;
  }
}
