package com.metabitlab.taibiex.privateapi.errors;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler;
import com.netflix.graphql.types.errors.TypedGraphQLError;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;

@Component
public class UnSupportCurrencyExceptionHandler implements DataFetcherExceptionHandler {

  private final DefaultDataFetcherExceptionHandler defaultHandler = new DefaultDataFetcherExceptionHandler();

  @Override
  public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
      DataFetcherExceptionHandlerParameters handlerParameters) {
    Throwable exception = handlerParameters.getException();
    if (exception instanceof UnSupportCurrencyException) {
      Map<String, Object> debugInfo = new HashMap<>(4);
      debugInfo.put("Currency", ((UnSupportCurrencyException)exception).getCurrency());

      TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
              .message(exception.getMessage())
              .debugInfo(debugInfo)
              .path(handlerParameters.getPath()).build();

      DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
              .error(graphqlError)
              .build();

      return CompletableFuture.completedFuture(result);
    }

    return defaultHandler.handleException(handlerParameters);
  }
}
