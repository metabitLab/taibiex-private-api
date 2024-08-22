package com.metabitlab.taibiex.privateapi.service;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolDayDataSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolHourDataSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolsSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

@Service
public class V3PoolService {

    private final TokenService tokenService;

    private final PoolsSubgraphFetcher poolsSubgraphFetcher;

    private final PoolDayDataSubgraphFetcher poolDayDataSubgraphFetcher;

    private final PoolHourDataSubgraphFetcher poolHourDataSubgraphFetcher;

    public V3PoolService(TokenService tokenService,
                         PoolsSubgraphFetcher poolsSubgraphFetcher,
                         PoolDayDataSubgraphFetcher poolDayDataSubgraphFetcher,
                         PoolHourDataSubgraphFetcher poolHourDataSubgraphFetcher) {
        this.tokenService = tokenService;
        this.poolsSubgraphFetcher = poolsSubgraphFetcher;
        this.poolDayDataSubgraphFetcher = poolDayDataSubgraphFetcher;
        this.poolHourDataSubgraphFetcher = poolHourDataSubgraphFetcher;
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

    public Amount cumulativeVolume(V3Pool pool, HistoryDuration duration) {
        switch (duration){
            case DAY:
                return getDayCumulativeVolume(pool);
            case WEEK:
                return getWeekCumulativeVolume(pool);
            default:
                return null;
        }
    }

    private Amount getWeekCumulativeVolume(V3Pool pool) {
        List<PoolDayData> poolDayDataList = poolDayDataSubgraphFetcher.poolDayDatas(0, 7, PoolDayData_orderBy.date, OrderDirection.desc,
                new PoolDayData_filter() {{
                    setPool(pool.getAddress());
                }});
        if (poolDayDataList.isEmpty()){
            return null;
        }
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (PoolDayData poolDayData : poolDayDataList) {
            totalVolume = totalVolume.add(poolDayData.getVolumeUSD());
        }
        Amount amount = new Amount();
        amount.setCurrency(Currency.USD);
        amount.setValue(totalVolume.doubleValue());
        amount.setId(getAmountIdEncoded(totalVolume.doubleValue(), "USD"));
        return amount;
    }

    private Amount getDayCumulativeVolume(V3Pool pool) {
        List<PoolHourData> poolHourDataList = poolHourDataSubgraphFetcher.poolHourDatas(0, 24, PoolHourData_orderBy.periodStartUnix, OrderDirection.desc,
                new PoolHourData_filter() {{
                    setPool(pool.getAddress());
                }});
        if (poolHourDataList.isEmpty()){
            return null;
        }
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (PoolHourData poolHourData : poolHourDataList) {
            totalVolume = totalVolume.add(poolHourData.getVolumeUSD());
        }
        Amount amount = new Amount();
        amount.setCurrency(Currency.USD);
        amount.setValue(totalVolume.doubleValue());
        amount.setId(getAmountIdEncoded(totalVolume.doubleValue(), "USD"));
        return amount;
    }
}
