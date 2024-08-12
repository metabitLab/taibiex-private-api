package com.privateapi.service;

import org.springframework.stereotype.Service;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

@Service
public class SomeQueryStrDataFetcher implements DataFetcher<String> {

  /*
   * curl -X POST http://localhost:8803/api/graphql/query \
   * -H "Content-Type: application/json" \
   * -d '{"query": "query Wanda($id: ID!) { someQueryStr(id: $id) }", "operationName": "Wanda", "variables": { "id": 3 }}'
   */

  @Override
  public String get(DataFetchingEnvironment environment) {
    System.out.println(environment.getArgument("id").toString());
    return "query string";
  }
}
