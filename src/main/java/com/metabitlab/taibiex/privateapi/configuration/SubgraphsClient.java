package com.metabitlab.taibiex.privateapi.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.netflix.graphql.dgs.client.GraphQLClient;
import com.netflix.graphql.dgs.client.RestClientGraphQLClient;

@Configuration
public class SubgraphsClient {

  @Autowired
  RestClient.Builder restClientBuilder;
  
  @Value("${app.subgraphs.endpoint}")
  private String subgraphsEndpoint;

  @Bean
  public GraphQLClient getRestClientGraphQlClient() {
    RestClient client = restClientBuilder.baseUrl(this.subgraphsEndpoint).build();
    return new RestClientGraphQLClient(client);
  }
}
