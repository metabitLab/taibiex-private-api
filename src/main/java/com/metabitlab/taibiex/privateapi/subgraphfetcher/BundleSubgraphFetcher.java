package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.BundleGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.BundleProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

@Service
public class BundleSubgraphFetcher {
  @Autowired
  SubgraphsClient subgraphsClient;
  
  public Bundle bundle() {
    // NOTE: 参数 id 写死为 "1"，目前仅支持查询 id 为 "1" 的 Bundle

    BundleGraphQLQuery bundleQuery = BundleGraphQLQuery.newRequest()
      .id("1")
      .queryName("BundleSubgraphFetcher_bundle")
      .build();

    BundleProjectionRoot<?, ?> projection = new BundleProjectionRoot<>()
      .id().ethPriceUSD();

    GraphQLQueryRequest request = new GraphQLQueryRequest(bundleQuery, projection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    return response.extractValueAsObject("bundle", new TypeRef<Bundle>() {});
  }
}
