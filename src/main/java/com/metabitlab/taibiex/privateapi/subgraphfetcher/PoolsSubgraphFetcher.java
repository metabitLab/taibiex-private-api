package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import java.util.List;

import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.*;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Pool_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Pool_orderBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Pool;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

@Service
public class PoolsSubgraphFetcher {

    @Autowired
    SubgraphsClient subgraphsClient;

    public List<Pool> pools(int skip, int first, Pool_orderBy orderBy, OrderDirection orderDirection, Pool_filter where) {
        PoolsGraphQLQuery poolsQuery = PoolsGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .where(where)
                .queryName("PoolsSubgraphFetcher_pools")
                .build();

        PoolProjectionRoot<?, ?> poolProjection = new PoolProjectionRoot<>()
                .id()
                .createdAtTimestamp()
                .createdAtBlockNumber()
                .token0Price()
                .token1Price()
                .liquidity()
                .sqrtPrice()
                .feeTier()
                .tick()
                .liquidityProviderCount()
                .collectedFeesToken0()
                .collectedFeesToken1()
                .collectedFeesUSD()
                .observationIndex()
                .volumeToken0()
                .volumeToken1()
                .volumeUSD()
                .untrackedVolumeUSD()
                .txCount()
                .totalValueLockedToken0()
                .totalValueLockedToken1()
                .totalValueLockedETH()
                .totalValueLockedUSD()
                .totalValueLockedUSDUntracked()
                .feesUSD();

        poolProjection.token0().id().decimals().name().symbol().totalSupply().derivedETH()
                .totalValueLocked().totalValueLockedUSD().totalValueLockedUSDUntracked()
                .txCount().untrackedVolumeUSD().volume().volumeUSD().poolCount();

        poolProjection.token1().id().decimals().name().symbol().totalSupply().derivedETH()
                .totalValueLocked().totalValueLockedUSD().totalValueLockedUSDUntracked()
                .txCount().untrackedVolumeUSD().volume().volumeUSD().poolCount();

        GraphQLQueryRequest request = new GraphQLQueryRequest(poolsQuery, poolProjection);
        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        List<Pool> pools = response.extractValueAsObject("pools", new TypeRef<List<Pool>>() {
        });

        return pools;
    }

    public Pool pool(String id) {
        PoolGraphQLQuery poolsQuery = PoolGraphQLQuery.newRequest()
                .id(id)
                .build();

        PoolProjectionRoot<?, ?> poolProjection = new PoolProjectionRoot<>()
                .id()
                .createdAtTimestamp()
                .createdAtBlockNumber()
                .token0Price()
                .token1Price()
                .liquidity()
                .sqrtPrice()
                .feeTier()
                .tick()
                .liquidityProviderCount()
                .collectedFeesToken0()
                .collectedFeesToken1()
                .collectedFeesUSD()
                .observationIndex()
                .volumeToken0()
                .volumeToken1()
                .volumeUSD()
                .untrackedVolumeUSD()
                .txCount()
                .totalValueLockedToken0()
                .totalValueLockedToken1()
                .totalValueLockedETH()
                .totalValueLockedUSD()
                .totalValueLockedUSDUntracked()
                .feesUSD();

        poolProjection.token0()
                .id()
                .symbol()
                .name()
                .decimals()
                .totalSupply()
                .untrackedVolumeUSD()
                .txCount()
                .totalValueLocked()
                .totalValueLockedUSD()
                .totalValueLockedUSDUntracked()
                .derivedETH()
                .poolCount()
                .volumeUSD()
                .totalValueLocked()
                .totalValueLockedUSD();

        poolProjection.token1()
                .id()
                .symbol()
                .name()
                .decimals()
                .totalSupply()
                .untrackedVolumeUSD()
                .txCount()
                .totalValueLocked()
                .totalValueLockedUSD()
                .totalValueLockedUSDUntracked()
                .derivedETH()
                .poolCount()
                .volumeUSD()
                .totalValueLocked()
               .totalValueLockedUSD();


        GraphQLQueryRequest request = new GraphQLQueryRequest(poolsQuery, poolProjection);
        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        return response.extractValueAsObject("pool", Pool.class);
    }
}
