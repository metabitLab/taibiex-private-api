package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProject;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProjectMarket;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.PoolsProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Pool;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

import graphql.com.google.common.base.Objects;

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
    PoolsGraphQLQuery poolsQuery = PoolsGraphQLQuery.newRequest()
      .skip(0)
      .first(100)
      .queryName("TokenDataFetcher")
      .build();

    PoolsProjectionRoot<?, ?> poolsProjection = new PoolsProjectionRoot<>()
      .token0Price()
      .id();

    GraphQLQueryRequest request = new GraphQLQueryRequest(poolsQuery, poolsProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<Pool> pools = response.extractValueAsObject("pools", new TypeRef<List<Pool>>() {});
    Optional<Pool> target = pools.stream()
      .filter(item -> Objects.equal(item.getId(), "0x6cec4df7ad64d5d06860a397c17edebc5f311ae3"))
      .findFirst();
    double price = target.orElse(null).getToken0Price().doubleValue();

    return new Token() {
      {
        setId("123");
        setChain(Chain.valueOf(chain));
        setAddress(address);
        setName("Ethereum");
        setSymbol("ETH");
        setProject(new TokenProject() {
          {
            setId("123");
            setMarkets(Arrays.asList(
              new TokenProjectMarket() {
                {
                  setId("123");
                  setPrice(new Amount() {
                    {
                      setId("123");
                      setCurrency(Currency.USD);
                      setValue(price);
                    }
                  });
                }
              }
            ));
          }
        });
      }
    };
  }
}
