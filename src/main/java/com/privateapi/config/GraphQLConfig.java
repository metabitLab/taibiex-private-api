package com.privateapi.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

@Configuration
public class GraphQLConfig {
  
  @Bean
  public GraphQL graphQL() throws IOException {
    ClassPathResource classPathResource = new ClassPathResource("graphql/schema.graphql");
    if (!classPathResource.exists()) {
      throw new IOException("schema.graphql file does not exist in the classpath");
    }

    InputStream inputStream = classPathResource.getInputStream();
    String schema = convertInputStreamToString(inputStream);
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeRegistry = schemaParser.parse(schema);
    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
        .type("Query", builder -> builder
          .dataFetcher("hello", new StaticDataFetcher("world")))
        .build();
    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    return GraphQL.newGraphQL(graphQLSchema).build();
  }

  private String convertInputStreamToString(InputStream inputStream) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
        return reader.lines().collect(Collectors.joining("\n"));
    }
  }
}
