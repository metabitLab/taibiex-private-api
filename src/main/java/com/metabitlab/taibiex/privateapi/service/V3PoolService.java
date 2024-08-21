package com.metabitlab.taibiex.privateapi.service;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolsSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Pool;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class V3PoolService {

    private final TokenService tokenService;

    private final PoolsSubgraphFetcher poolsSubgraphFetcher;

    public V3PoolService(TokenService tokenService, PoolsSubgraphFetcher poolsSubgraphFetcher) {
        this.tokenService = tokenService;
        this.poolsSubgraphFetcher = poolsSubgraphFetcher;
    }

    public com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.V3Pool pool(Chain chain, String address) {
        Pool pool = poolsSubgraphFetcher.pool(address);
        if (pool == null){
            return null;
        }
        V3Pool v3Pool = new V3Pool();
        v3Pool.setAddress(pool.getId());
        v3Pool.setChain(Chain.TABI);
        v3Pool.setCreatedAtTimestamp(pool.getCreatedAtTimestamp().intValue());
        v3Pool.setProtocolVersion(ProtocolVersion.V3);
        v3Pool.setFeeTier(pool.getFeeTier().doubleValue());
        v3Pool.setTotalLiquidity(
                new Amount() {{
                    setId(getAmountIdEncoded(pool.getTotalValueLockedUSD().doubleValue(), "USD"));
                    setCurrency(Currency.USD);
                    setValue(pool.getLiquidity().doubleValue());
                }}
        );
        v3Pool.setToken0Supply(pool.getTotalValueLockedToken0().doubleValue());
        v3Pool.setToken1Supply(pool.getTotalValueLockedToken1().doubleValue());
        v3Pool.setTxCount(pool.getTxCount().intValue());
        v3Pool.setFeeTier(pool.getFeeTier().doubleValue());
        v3Pool.setToken0(
                tokenService.token(Chain.TABI, pool.getToken0().getId())
        );
        v3Pool.setToken1(
                tokenService.token(Chain.TABI, pool.getToken1().getId())
        );
        v3Pool.setId(pool.getId());

        return v3Pool;
    }

    private String getAmountIdEncoded(double value, String currency) {
        return Base64.getEncoder().encodeToString(("AMOUNT:" + value + "_" + currency).getBytes());
    }
}
