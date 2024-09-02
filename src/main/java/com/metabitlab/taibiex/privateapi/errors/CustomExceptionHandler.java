package com.metabitlab.taibiex.privateapi.errors;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler;
import com.netflix.graphql.types.errors.TypedGraphQLError;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;

@Component
public class CustomExceptionHandler implements DataFetcherExceptionHandler {

    private final DefaultDataFetcherExceptionHandler defaultHandler = new DefaultDataFetcherExceptionHandler();

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters handlerParameters) {

        Throwable exception = handlerParameters.getException();

        Map<String, Object> debugInfo = new HashMap<>(4);

        if (exception instanceof UnSupportCurrencyException) {
            List<Chain> chains = ((UnSupportChainException) exception).getChains();
            StringBuilder sb = chains.stream()
                .reduce(new StringBuilder(), (acc, chain) -> {
                    if (acc.length() > 0) {
                        acc.append(", ");
                    }
                    acc.append(chain);
                    return acc;
                }, (acc1, acc2) -> acc1);
            debugInfo.put("Currency", sb.toString());

            TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message("privateapi exception: " + exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath()).build();

            DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build();

            return CompletableFuture.completedFuture(result);
        }

        if (exception instanceof UnSupportDurationException) {
            debugInfo.put("Duration", ((UnSupportDurationException) exception).getDuration());

            TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message("privateapi exception: " + exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath()).build();

            DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build();

            return CompletableFuture.completedFuture(result);
        }

        if (exception instanceof MissVariableException) {
            debugInfo.put("Variable", ((MissVariableException) exception).getVariable());

            TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message("privateapi exception: " + exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath()).build();

            DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build();

            return CompletableFuture.completedFuture(result);
        }

        if (exception instanceof UnKnownTokenException) {
            debugInfo.put("Token", ((UnKnownTokenException) exception).getToken());

            TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message("privateapi exception: " + exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath()).build();

            DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build();

            return CompletableFuture.completedFuture(result);
        }

        if (exception instanceof MissLocalContextException) {
            debugInfo.put("Local Context", ((MissLocalContextException) exception).getContextDescription());

            TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message("privateapi exception: " + exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath()).build();

            DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build();

            return CompletableFuture.completedFuture(result);
        }

        if (exception instanceof MissSourceException) {
            debugInfo.put("Source", ((MissSourceException) exception).getSourceDescription());

            TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message("privateapi exception: " + exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath()).build();

            DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build();

            return CompletableFuture.completedFuture(result);
        }

        if (exception instanceof UnSupportChainException) {
            List<Chain> chains = ((UnSupportChainException) exception).getChains();
            StringBuilder sb = chains.stream()
                .reduce(new StringBuilder(), (acc, chain) -> {
                    if (acc.length() > 0) {
                        acc.append(", ");
                    }
                    acc.append(chain);
                    return acc;
                }, (acc1, acc2) -> acc1);

            debugInfo.put("Chains", sb.toString());

            TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message("privateapi exception: " + exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath()).build();

            DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build();

            return CompletableFuture.completedFuture(result);
        }

        if (exception instanceof ParseCacheException) {
            debugInfo.put("Cache Key", ((ParseCacheException) exception).getCacheKey());

            TypedGraphQLError graphqlError = TypedGraphQLError.newInternalErrorBuilder()
                    .message("privateapi exception: " + exception.getMessage())
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
