package com.privateapi.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.privateapi.entity.QueryRequest;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;

@RestController
public class ApiController {

  @Autowired
  private GraphQL graphQL;

  @PostMapping("/api/graphql/query")
  public ResponseEntity<Map<String, Object>> query(@RequestBody QueryRequest queryRequest) {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        .query(queryRequest.getQuery())
        .operationName(queryRequest.getOperationName())
        .variables(queryRequest.getVariables())
        .build();

    ExecutionResult result = graphQL.execute(executionInput);

    return ResponseEntity.ok(result.toSpecification());
  }
}
