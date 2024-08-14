package com.metabitlab.taibiex.privateapi.fetcher;

import org.springframework.beans.factory.annotation.Value;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsGraphQLQuery;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
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

  @Value("${app.subgraphs.endpoint}")
  private String subgraphsEndpoint;

  @DgsQuery
  public Token token(@InputArgument String chain, @InputArgument String address) {
    System.out.println("chain: " + chain);
    System.out.println("address: " + address);

    GraphQLQueryRequest request = new GraphQLQueryRequest(
      new PoolsGraphQLQuery.Builder()
        .skip(0)
        .first(100)
        .build()
    );

    String requestBody = request.serialize();
    System.out.println(subgraphsEndpoint);
    System.out.println(requestBody);

    return new Token() {
      {
        setName("ETH");
      }
    };
  }
}
