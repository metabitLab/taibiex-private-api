package com.metabitlab.taibiex.privateapi.fetcher;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsProjectionRoot;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

/**
 * This class is responsible for fetching token data.
 * 
 * curl -X POST http://localhost:8803/api/graphql/query \
 * -H "Content-Type: application/json" \
 * -d '{"query": "query Wanda($chain: Chain!, $address: String) { token(chain: $chain, address: $address) { name } }", "operationName": "Wanda", "variables": { "chain": "ETHEREUM", "address": "321" }}'
 * 
 * @author Nix
 */
@DgsComponent
public class TokenDataFetcher {

  @Autowired
  SubgraphsClient subgraphsClient;

  @DgsQuery
  public Token token(@InputArgument String chain, @InputArgument String address) {
    System.out.println("chain: " + chain);
    System.out.println("address: " + address);

    PoolsGraphQLQuery pools = PoolsGraphQLQuery.newRequest()
      .skip(0)
      .first(100)
      .queryName("TokenSpotPrice")
      .build();

    PoolsProjectionRoot<?, ?> poolsProjection = new PoolsProjectionRoot<>()
      .id();

    GraphQLQueryRequest request = new GraphQLQueryRequest(pools, poolsProjection);

    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    System.out.println(request.serialize());
    System.out.println(JSON.toJSONString(response.extractValue("pools")));

    return new Token() {
      {
        setAddress(address);
        setChain(Chain.valueOf(chain));
      }
    };
  }
}
