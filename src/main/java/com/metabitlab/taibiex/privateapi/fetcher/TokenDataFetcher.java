package com.metabitlab.taibiex.privateapi.fetcher;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class TokenDataFetcher {
  /**
   * This class is responsible for fetching token data.
   * 
   * curl -X POST http://localhost:8803/api/graphql/query \
   * -H "Content-Type: application/json" \
   * -d '{"query": "query Wanda($chain: Chain!, $address: String) { token(chain: $chain, address: $address) { name } }", "operationName": "Wanda", "variables": { "chain": "ETHEREUM", "address": "321" }}'
   * 
   * @author nix
   */
  @DgsQuery
  public Token token(@InputArgument String chain, @InputArgument String address) {
    System.out.println("chain: " + chain);
    System.out.println("address: " + address);

    return new Token() {
      {
        setName("ETH");
      }
    };
  }
}
