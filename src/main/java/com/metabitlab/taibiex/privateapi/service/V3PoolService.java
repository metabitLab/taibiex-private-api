package com.metabitlab.taibiex.privateapi.service;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolDayDataSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolHourDataSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolMinuteDataSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolsSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.metabitlab.taibiex.privateapi.util.DateUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class V3PoolService {

    private final TokenService tokenService;

    private final PoolsSubgraphFetcher poolsSubgraphFetcher;

    private final PoolDayDataSubgraphFetcher poolDayDataSubgraphFetcher;

    private final PoolHourDataSubgraphFetcher poolHourDataSubgraphFetcher;

    private final PoolMinuteDataSubgraphFetcher poolMinuteDataSubgraphFetcher;

    public V3PoolService(TokenService tokenService,
                         PoolsSubgraphFetcher poolsSubgraphFetcher,
                         PoolDayDataSubgraphFetcher poolDayDataSubgraphFetcher,
                         PoolHourDataSubgraphFetcher poolHourDataSubgraphFetcher,
                         PoolMinuteDataSubgraphFetcher poolMinuteDataSubgraphFetcher) {
        this.tokenService = tokenService;
        this.poolsSubgraphFetcher = poolsSubgraphFetcher;
        this.poolDayDataSubgraphFetcher = poolDayDataSubgraphFetcher;
        this.poolHourDataSubgraphFetcher = poolHourDataSubgraphFetcher;
        this.poolMinuteDataSubgraphFetcher = poolMinuteDataSubgraphFetcher;
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

    public Amount getTotalLiquidityPercentChange24h(V3Pool pool) {
        List<PoolDayData> poolDayDataList = poolDayDataSubgraphFetcher.poolDayDatas(0, 2, PoolDayData_orderBy.date, OrderDirection.desc,
                new PoolDayData_filter() {{
                    setPool(pool.getId());
                }}
        );
        if (poolDayDataList.size() >= 2) {
            BigInteger divide = (poolDayDataList.get(1).getLiquidity().subtract(poolDayDataList.get(0).getLiquidity())).divide(poolDayDataList.get(0).getLiquidity());
            return new Amount() {{
                setId(getAmountIdEncoded(divide.doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(divide.doubleValue());
            }};
        } else {
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

    public List<Amount> getHistoricalVolume(V3Pool pool, HistoryDuration duration) {
        switch (duration){
            case DAY -> {
                return getDayHistoricalVolume(pool);
            }
            case WEEK -> {
                return getWeekHistoricalVolume(pool);
            }
            case HOUR -> {
                return getHourHistoricalVolume(pool);
            }
            case MONTH -> {
                return getMonthHistoricalVolume(pool);
            }
            case YEAR -> {
                return getYearHistoricalVolume(pool);
            }
            default -> {
                return null;
            }
        }
    }

    public List<Amount> getHourHistoricalVolume(V3Pool pool) {
        long[] fiveMinuteTimestamp = DateUtil.getFiveMinuteTimestamp(12);
        List<String> ids = new ArrayList<>();
        for (long timestamp : fiveMinuteTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/60);
        }
        List<PoolMinuteData> poolHourDataList = poolMinuteDataSubgraphFetcher.poolMinuteDatas(0, null, PoolMinuteData_orderBy.periodStartUnix, OrderDirection.desc,
                new PoolMinuteData_filter() {{
                    setId_in(ids);
                }});
        return poolHourDataList.stream().map(poolHourData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolHourData.getVolumeUSD().doubleValue());
            amount.setId(getAmountIdEncoded(poolHourData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());

    }

    public List<Amount> getDayHistoricalVolume(V3Pool pool) {
        long[] fiveMinuteTimestamp = DateUtil.get24HourTimestamp();
        List<String> ids = new ArrayList<>();
        for (long timestamp : fiveMinuteTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/60);
        }
        List<PoolMinuteData> poolMinuteDatas = poolMinuteDataSubgraphFetcher.poolMinuteDatas(0, null, PoolMinuteData_orderBy.periodStartUnix, OrderDirection.desc,
                new PoolMinuteData_filter() {{
                    setId_in(ids);
                }});
        return poolMinuteDatas.stream().map(poolMinuteData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolMinuteData.getVolumeUSD().doubleValue());
            amount.setId(getAmountIdEncoded(poolMinuteData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());
    }

    public List<Amount> getWeekHistoricalVolume(V3Pool pool) {
        long[] sixHourTimestamp = DateUtil.getSixHourTimestamp(28);
        List<String> ids = new ArrayList<>();
        for (long timestamp : sixHourTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/3600);
        }
        List<PoolHourData> poolHourDataList = poolHourDataSubgraphFetcher.poolHourDatas(0, null, PoolHourData_orderBy.periodStartUnix, OrderDirection.desc,
                new PoolHourData_filter() {{
                    setId_in(ids);
                }});
        return poolHourDataList.stream().map(poolHourData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolHourData.getVolumeUSD().doubleValue());
            amount.setId(getAmountIdEncoded(poolHourData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());
    }

    public List<Amount> getMonthHistoricalVolume(V3Pool pool) {
        long[] oneDayTimestamp = DateUtil.getOneDayTimestamp(30);
        List<String> ids = new ArrayList<>();
        for (long timestamp : oneDayTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/86400);
        }
        List<PoolDayData> poolDayDataList = poolDayDataSubgraphFetcher.poolDayDatas(0, null, PoolDayData_orderBy.date, OrderDirection.desc,
                new PoolDayData_filter() {{
                    setId_in(ids);
                }});
        return poolDayDataList.stream().map(poolDayData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolDayData.getVolumeUSD().doubleValue());
            amount.setId(getAmountIdEncoded(poolDayData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());
    }

    public List<Amount> getYearHistoricalVolume(V3Pool pool) {
        long[] oneDayTimestamp = DateUtil.getSevenDayTimestamp(52);
        List<String> ids = new ArrayList<>();
        for (long timestamp : oneDayTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/86400);
        }
        List<PoolDayData> poolDayDataList = poolDayDataSubgraphFetcher.poolDayDatas(0, null, PoolDayData_orderBy.date, OrderDirection.desc,
                new PoolDayData_filter() {{
                    setId_in(ids);
                }});
        return poolDayDataList.stream().map(poolDayData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolDayData.getVolumeUSD().doubleValue());
            amount.setId(getAmountIdEncoded(poolDayData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());
    }

    public List<Amount> getPriceHistory(V3Pool pool, HistoryDuration duration) {
        switch (duration) {
            case DAY -> {
                return getDayPriceHistory(pool);
            }

            case WEEK -> {
                return getWeekPriceHistory(pool);
            }
            case MONTH -> {
                return getMonthPriceHistory(pool);
            }
            case YEAR -> {
               return getYearPriceHistory(pool);
            }
            default -> {
                return null;
            }
        }
    }


    private List<Amount> getDayPriceHistory(V3Pool pool){
        long[] minuteTimestamp = DateUtil.get24HourTimestamp();
        List<String> ids = new ArrayList<>();
        for (long timestamp : minuteTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/60);
        }
        List<PoolMinuteData> poolMinuteDatas = poolMinuteDataSubgraphFetcher.poolMinuteDatas(0, null, PoolMinuteData_orderBy.periodStartUnix, OrderDirection.desc,
                new PoolMinuteData_filter() {{
                    setId_in(ids);
                }});
        return poolMinuteDatas.stream().map(poolMinuteData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolMinuteData.getToken0Price().doubleValue());
            amount.setId(getAmountIdEncoded(poolMinuteData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());
    }

    private List<Amount> getWeekPriceHistory(V3Pool pool){
        long[] minuteTimestamp = DateUtil.getLastHourTimestamp(168);
        List<String> ids = new ArrayList<>();
        for (long timestamp : minuteTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/60);
        }
        List<PoolMinuteData> poolMinuteDatas = poolMinuteDataSubgraphFetcher.poolMinuteDatas(0, null, PoolMinuteData_orderBy.periodStartUnix, OrderDirection.desc,
                new PoolMinuteData_filter() {{
                    setId_in(ids);
                }});
        return poolMinuteDatas.stream().map(poolMinuteData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolMinuteData.getToken0Price().doubleValue());
            amount.setId(getAmountIdEncoded(poolMinuteData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());
    }

    private List<Amount> getMonthPriceHistory(V3Pool pool){
        long[] hourTimestamp = DateUtil.get4HourTimestamp(180);
        List<String> ids = new ArrayList<>();
        for (long timestamp : hourTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/60);
        }
        List<PoolMinuteData> poolMinuteDatas = poolMinuteDataSubgraphFetcher.poolMinuteDatas(0, null, PoolMinuteData_orderBy.periodStartUnix, OrderDirection.desc,
                new PoolMinuteData_filter() {{
                    setId_in(ids);
                }});
        return poolMinuteDatas.stream().map(poolMinuteData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolMinuteData.getToken0Price().doubleValue());
            amount.setId(getAmountIdEncoded(poolMinuteData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());
    }

    private List<Amount> getYearPriceHistory(V3Pool pool){
        long[] dayTimestamp = DateUtil.getSevenDayTimestamp(52);
        List<String> ids = new ArrayList<>();
        for (long timestamp : dayTimestamp) {
            ids.add(pool.getAddress().toLowerCase() + "-" + timestamp/86400);
        }
        List<PoolDayData> poolDayDatas = poolDayDataSubgraphFetcher.poolDayDatas(0, null, PoolDayData_orderBy.date, OrderDirection.desc,
                new PoolDayData_filter() {{
                    setId_in(ids);
                }});
        return poolDayDatas.stream().map(poolDayData -> {
            Amount amount = new Amount();
            amount.setCurrency(Currency.USD);
            amount.setValue(poolDayData.getToken0Price().doubleValue());
            amount.setId(getAmountIdEncoded(poolDayData.getVolumeUSD().doubleValue(), "USD"));
            return amount;
        }).collect(Collectors.toList());
    }

    public List<V3Pool> topV3Pools(Chain chain, Integer first, Float tvlCursor, String tokenFilter) {
        List<Pool> pools = poolsSubgraphFetcher.pools(0, first, Pool_orderBy.totalValueLockedUSD, OrderDirection.desc, null);
        if (pools.isEmpty()){
            return null;
        }
        return pools.stream().map(pool -> {
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
        }).collect(Collectors.toList());
    }
}
