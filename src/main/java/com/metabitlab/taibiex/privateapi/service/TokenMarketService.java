package com.metabitlab.taibiex.privateapi.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.*;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.metabitlab.taibiex.privateapi.util.DateUtil;
import com.metabitlab.taibiex.privateapi.util.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TokenMarketService {

    private static final Logger log = LoggerFactory.getLogger(TokenMarketService.class);
    private final TokenHourDataSubgraphFetcher tokenHourDataSubgraphFetcher;

    private final TokenDayDataSubgraphFetcher tokenDayDataSubgraphFetcher;

    private final TokenMinuteDataSubgraphFetcher tokenMinuteDataSubgraphFetcher;

    private final TokenSubgraphFetcher tokenSubgraphFetcher;

    private final BundleSubgraphFetcher bundleSubgraphFetcher;

    @Autowired
    private RedisService redisService;

    public TokenMarketService(TokenHourDataSubgraphFetcher tokenHourDataSubgraphFetcher,
                              TokenDayDataSubgraphFetcher tokenDayDataSubgraphFetcher,
                              TokenMinuteDataSubgraphFetcher tokenMinuteDataSubgraphFetcher,
                              TokenSubgraphFetcher tokenSubgraphFetcher,
                              BundleSubgraphFetcher bundleSubgraphFetcher) {
        this.tokenHourDataSubgraphFetcher = tokenHourDataSubgraphFetcher;
        this.tokenDayDataSubgraphFetcher = tokenDayDataSubgraphFetcher;
        this.tokenMinuteDataSubgraphFetcher = tokenMinuteDataSubgraphFetcher;
        this.tokenSubgraphFetcher = tokenSubgraphFetcher;
        this.bundleSubgraphFetcher = bundleSubgraphFetcher;
    }

    public TokenMarket tokenMarket(Token token, Currency currency) {

        String cacheKey = "topMarket:" +  token.getChain() + "_" + token.getSymbol() + "_" + token.getAddress();

        try {
            Object topTokens = redisService.get(cacheKey);
            if (null != topTokens) {
                return JSONObject.parseObject(topTokens.toString(), TokenMarket.class);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        TokenMarket tokenMarket = new TokenMarket();

        tokenMarket.setId(Base64.getEncoder().encodeToString(("TokenMarket:TABI_" + token.getAddress().toLowerCase() + "_" + token.getSymbol()).getBytes()));

        tokenMarket.setToken(token);

        tokenMarket.setPriceSource(PriceSource.SUBGRAPH_V3);

        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token subgraphToken
                = tokenSubgraphFetcher.token(token.getAddress());
        log.info("fetch token from Subgraph, token symbol:{}, token address:{} ", token.getSymbol(), token.getAddress());
        log.info("tokenSubgraphFetcher.token: {}", subgraphToken);
        // price
        Bundle bundle = bundleSubgraphFetcher.bundle();
        double price = bundle.getEthPriceUSD().multiply(subgraphToken.getDerivedETH()).doubleValue();
        tokenMarket.setPrice(new Amount() {
            {
                setId(getAmountIdEncoded(price, currency));
                setCurrency(Currency.USD);
                setValue(price);
            }
        });

        // totalValueLocked
        tokenMarket.setTotalValueLocked(new Amount() {
            {
                setId(getAmountIdEncoded(subgraphToken.getTotalValueLockedUSD().doubleValue(), currency));
                setCurrency(Currency.USD);
                setValue(subgraphToken.getTotalValueLockedUSD().doubleValue());
            }
        });

        try {
            String pvoStr = JSON.toJSONString(tokenMarket, SerializerFeature.WriteNullStringAsEmpty);
            redisService.set(cacheKey, pvoStr, 2, TimeUnit.MINUTES);
        } catch (Exception e) {
           e.fillInStackTrace();
        }

        return tokenMarket;
    }

    private String getAmountIdEncoded(Amount amount) {
        return Base64.getEncoder().encodeToString(("AMOUNT:" + amount.getValue() + "_" + amount.getCurrency()).getBytes());
    }

    private String getAmountIdEncoded(double value, Currency currency) {
        return Base64.getEncoder().encodeToString(("AMOUNT:" + value + "_" + currency).getBytes());
    }

    private String getAmountIdEncoded(double value, String currency) {
        return Base64.getEncoder().encodeToString(("AMOUNT:" + value + "_" + currency).getBytes());
    }

    public Amount getPricePercentChange(TokenMarket tokenMarket, HistoryDuration duration){
        Token token = tokenMarket.getToken();
        if (duration == HistoryDuration.HOUR) {
            return getHourPricePercentChange(tokenMarket, token.getAddress());
        } else if (duration == HistoryDuration.FIVE_MINUTE) {
            return getMinutePricePercentChange(tokenMarket, token.getAddress());
        } else if (duration == HistoryDuration.DAY) {
           return getDayPricePercentChange(tokenMarket, token.getAddress());
        } else if (duration == HistoryDuration.WEEK){
            return getWeekPricePercentChange(tokenMarket, token.getAddress());
        } else if (duration == HistoryDuration.MONTH) {
            return getMonthPricePercentChange(tokenMarket, token.getAddress());
        } else {
            return getYearPricePercentChange(tokenMarket, token.getAddress());
        }
    }

    public Amount getPriceHighLow(TokenMarket tokenMarket, HistoryDuration duration, HighLow highLow){
        Token token = tokenMarket.getToken();
        if (duration == HistoryDuration.HOUR) {
            return getHourPriceHighLow(tokenMarket, token.getAddress(), highLow);
        } else if (duration == HistoryDuration.FIVE_MINUTE) {
            return getFiveMinutePriceHighLow(tokenMarket, token.getAddress(), highLow);
        } else if (duration == HistoryDuration.DAY) {
            return getDayPriceHighLow(tokenMarket, token.getAddress(), highLow);
        } else if (duration == HistoryDuration.WEEK){
            return getWeekPriceHighLow(tokenMarket, token.getAddress(), highLow);
        } else if (duration == HistoryDuration.MONTH) {
            return getMonthPriceHighLow(tokenMarket, token.getAddress(), highLow);
        } else {
            return getYearPriceHighLow(tokenMarket, token.getAddress(), highLow);
        }
    }

    private Amount getFiveMinutePriceHighLow(TokenMarket tokenMarket, String tokenAddress, HighLow highLow) {
        List<TokenMinuteData> tokenMinuteData = tokenMinuteDataSubgraphFetcher.tokenMinuteDatas(0, 5,
                TokenMinuteData_orderBy.periodStartUnix,
                OrderDirection.desc,
                new TokenMinuteData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (tokenMinuteData.isEmpty()){
            return null;
        }
        Amount amount = new Amount() {
            {
                setId(getAmountIdEncoded(tokenMinuteData.get(0).getPriceUSD().doubleValue(), tokenMarket.getToken().getSymbol()));
                setCurrency(Currency.USD);
            }
        };
        if (highLow == HighLow.HIGH) {
            BigDecimal max = tokenMinuteData.stream().map(TokenMinuteData::getPriceUSD).max(BigDecimal::compareTo).get();
            amount.setValue(max.doubleValue());
        } else {
            BigDecimal min = tokenMinuteData.stream().map(TokenMinuteData::getPriceUSD).min(BigDecimal::compareTo).get();
            amount.setValue(min.doubleValue());
        }
        return amount;
    }

    private Amount getDayPriceHighLow(TokenMarket tokenMarket, String tokenAddress, HighLow highLow) {
        List<TokenHourData> tokenHourData = tokenHourDataSubgraphFetcher.tokenHourDatas(0, 24,
                TokenHourData_orderBy.periodStartUnix,
                OrderDirection.desc,
                new TokenHourData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (tokenHourData.isEmpty()){
            return null;
        }
        Amount amount = new Amount() {
            {
                setId(getAmountIdEncoded(tokenHourData.get(0).getPriceUSD().doubleValue(), tokenMarket.getToken().getSymbol()));
                setCurrency(Currency.USD);
            }
        };
        if (highLow == HighLow.HIGH) {
            BigDecimal max = tokenHourData.stream().map(TokenHourData::getPriceUSD).max(BigDecimal::compareTo).get();
            amount.setValue(max.doubleValue());
        } else {
            BigDecimal min = tokenHourData.stream().map(TokenHourData::getPriceUSD).min(BigDecimal::compareTo).get();
            amount.setValue(min.doubleValue());
        }
        return amount;
    }

    private Amount getWeekPriceHighLow(TokenMarket tokenMarket, String tokenAddress, HighLow highLow) {
        List<TokenHourData> tokenHourData = tokenHourDataSubgraphFetcher.tokenHourDatas(0, 24 * 7,
                TokenHourData_orderBy.periodStartUnix,
                OrderDirection.desc,
                new TokenHourData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (tokenHourData.isEmpty()){
            return null;
        }
        Amount amount = new Amount() {
            {
                setId(getAmountIdEncoded(tokenHourData.get(0).getPriceUSD().doubleValue(), tokenMarket.getToken().getSymbol()));
                setCurrency(Currency.USD);
            }
        };
        if (highLow == HighLow.HIGH) {
            BigDecimal max = tokenHourData.stream().map(TokenHourData::getPriceUSD).max(BigDecimal::compareTo).get();
            amount.setValue(max.doubleValue());
        } else {
            BigDecimal min = tokenHourData.stream().map(TokenHourData::getPriceUSD).min(BigDecimal::compareTo).get();
            amount.setValue(min.doubleValue());
        }
        return amount;
    }

    private Amount getMonthPriceHighLow(TokenMarket tokenMarket, String tokenAddress, HighLow highLow) {
        List<TokenDayData> tokenDayData = tokenDayDataSubgraphFetcher.tokenDayDatas(0, 30,
                TokenDayData_orderBy.date,
                OrderDirection.desc,
                new TokenDayData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (tokenDayData.isEmpty()){
            return null;
        }
        Amount amount = new Amount() {
            {
                setId(getAmountIdEncoded(tokenDayData.get(0).getPriceUSD().doubleValue(), tokenMarket.getToken().getSymbol()));
                setCurrency(Currency.USD);
            }
        };
        if (highLow == HighLow.HIGH) {
            BigDecimal max = tokenDayData.stream().map(TokenDayData::getPriceUSD).max(BigDecimal::compareTo).get();
            amount.setValue(max.doubleValue());
        } else {
            BigDecimal min = tokenDayData.stream().map(TokenDayData::getPriceUSD).min(BigDecimal::compareTo).get();
            amount.setValue(min.doubleValue());
        }
        return amount;
    }

    private Amount getYearPriceHighLow(TokenMarket tokenMarket, String tokenAddress, HighLow highLow) {
        List<TokenDayData> tokenDayData = tokenDayDataSubgraphFetcher.tokenDayDatas(0, 365,
                TokenDayData_orderBy.date,
                OrderDirection.desc,
                new TokenDayData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (tokenDayData.isEmpty()){
            return null;
        }
        Amount amount = new Amount() {
            {
                setId(getAmountIdEncoded(tokenDayData.get(0).getPriceUSD().doubleValue(), tokenMarket.getToken().getSymbol()));
                setCurrency(Currency.USD);
            }
        };
        if (highLow == HighLow.HIGH) {
            BigDecimal max = tokenDayData.stream().map(TokenDayData::getPriceUSD).max(BigDecimal::compareTo).get();
            amount.setValue(max.doubleValue());
        } else {
            BigDecimal min = tokenDayData.stream().map(TokenDayData::getPriceUSD).min(BigDecimal::compareTo).get();
            amount.setValue(min.doubleValue());
        }
        return amount;
    }

    private Amount getHourPriceHighLow(TokenMarket tokenMarket, String tokenAddress, HighLow highLow) {
        List<TokenMinuteData> tokenHourData = tokenMinuteDataSubgraphFetcher.tokenMinuteDatas(0, 60,
                TokenMinuteData_orderBy.periodStartUnix,
                OrderDirection.desc,
                new TokenMinuteData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (tokenHourData.isEmpty()){
            return null;
        }
        Amount amount = new Amount() {
            {
                setId(getAmountIdEncoded(tokenHourData.get(0).getPriceUSD().doubleValue(), tokenMarket.getToken().getSymbol()));
                setCurrency(Currency.USD);
            }
        };
        if (highLow == HighLow.HIGH) {
            BigDecimal max = tokenHourData.stream().map(TokenMinuteData::getPriceUSD).max(BigDecimal::compareTo).get();
            amount.setValue(max.doubleValue());
        } else {
            BigDecimal min = tokenHourData.stream().map(TokenMinuteData::getPriceUSD).min(BigDecimal::compareTo).get();
            amount.setValue(min.doubleValue());
        }
        return amount;
    }



    public Amount getVolume(TokenMarket tokenMarket, HistoryDuration duration){
        Token token = tokenMarket.getToken();
        if (duration == HistoryDuration.HOUR) {
            return getHourVolume(token.getAddress());
        } else if (duration == HistoryDuration.FIVE_MINUTE) {
            return getMinuteVolume(token.getAddress());
        } else if (duration == HistoryDuration.DAY) {
            return getDayVolume(token.getAddress());
        } else {
            return getVolumeData(token.getAddress(), duration);
        }
    }

    private Amount getHourVolume(String tokenAddress) {
        List<TokenHourData> tokenHourData = tokenHourDataSubgraphFetcher.tokenHourDatas(0, 1,
                TokenHourData_orderBy.periodStartUnix,
                OrderDirection.desc,
                new TokenHourData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (!tokenHourData.isEmpty()){
            return new Amount(){{
                setId(getAmountIdEncoded(tokenHourData.get(0).getVolumeUSD().doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(tokenHourData.get(0).getVolumeUSD().doubleValue());
            }};
        }
        return null;
    }

    private Amount getMinuteVolume(String tokenAddress) {
        List<TokenMinuteData> tokenMinuteData = tokenMinuteDataSubgraphFetcher.tokenMinuteDatas(0, 1,
                TokenMinuteData_orderBy.periodStartUnix,
                OrderDirection.desc,
                new TokenMinuteData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (!tokenMinuteData.isEmpty()){
            return new Amount(){{
                setId(getAmountIdEncoded(tokenMinuteData.get(0).getVolumeUSD().doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(tokenMinuteData.get(0).getVolumeUSD().doubleValue());
            }};
        }
        return null;
    }

    private Amount getDayVolume(String tokenAddress) {
        List<TokenDayData> tokenDayDatas = tokenDayDataSubgraphFetcher.tokenDayDatas(0, 1,
                TokenDayData_orderBy.date,
                OrderDirection.desc,
                new TokenDayData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (!tokenDayDatas.isEmpty()){
            return new Amount(){{
                setId(getAmountIdEncoded(tokenDayDatas.get(0).getVolumeUSD().doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(tokenDayDatas.get(0).getVolumeUSD().doubleValue());
            }};
        }
        return null;
    }

    private Amount getVolumeData(String tokenAddress, HistoryDuration duration) {
        long timestamp;
        if (duration == HistoryDuration.WEEK) {
            timestamp = DateUtil.getAneWeekAgoMidnightTimestamp();
        } else if (duration == HistoryDuration.MONTH) {
            timestamp = DateUtil.getAneMonthAgoMidnightTimestamp();
        } else if (duration == HistoryDuration.YEAR) {
            timestamp = DateUtil.getAneYearAgoMidnightTimestamp();
        } else {
            return null;
        }
        String dataId = tokenAddress.toLowerCase() + ":" + timestamp / 86400;
        TokenDayData tokenDayDataWeekAgo = tokenDayDataSubgraphFetcher.tokenDayData(dataId);
        if (!ObjectUtils.isEmpty(tokenDayDataWeekAgo)) {
            return new Amount() {{
                setId(getAmountIdEncoded(tokenDayDataWeekAgo.getVolumeUSD().doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(tokenDayDataWeekAgo.getVolumeUSD().doubleValue());
            }};
        } else {
            return null;
        }
    }

    private Amount getMinutePricePercentChange(TokenMarket tokenMarket, String tokenAddress) {
        String cacheKey = "minutePricePercentChange:" + tokenAddress;
        try {
            Object amount = redisService.get(cacheKey);
            if (null != amount) {
                return JSONObject.parseObject(amount.toString(), Amount.class);
            }
        } catch (Exception e) {
            log.error("getMinutePricePercentChange redis read error：{}", e.getMessage());
        }
        List<TokenMinuteData> tokenHourData = tokenMinuteDataSubgraphFetcher.tokenMinuteDatas(0, 6,
                TokenMinuteData_orderBy.periodStartUnix,
                OrderDirection.desc,
                new TokenMinuteData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (tokenHourData.size() < 6){
            return null;
        }

        BigDecimal change = new BigDecimal(String.valueOf(tokenHourData.get(0).getPriceUSD()))
                .subtract(new BigDecimal(String.valueOf(tokenHourData.get(5).getPriceUSD())));
        BigDecimal percentChange = change.divide(new BigDecimal(String.valueOf(tokenHourData.get(5).getPriceUSD())));
        Amount amount = new Amount() {
            {
                setId(getAmountIdEncoded(percentChange.doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(percentChange.doubleValue());
            }
        };
        try {
            String pvoStr = JSON.toJSONString(amount, SerializerFeature.WriteNullStringAsEmpty);
            redisService.set(cacheKey, pvoStr);
        } catch (Exception e) {
            log.error("get redis write error：{}", e.getMessage());
        }
        return amount;
    }


    private Amount getHourPricePercentChange(TokenMarket tokenMarket, String tokenAddress) {
        String cacheKey = "hourPricePercentChange"  + ":" + tokenAddress;
        try {
            Object amount = redisService.get(cacheKey);
            if (null != amount) {
                return JSONObject.parseObject(amount.toString(), Amount.class);
            }
        } catch (Exception e) {
            log.error("getHourPricePercentChange redis read error：{}", e.getMessage());
        }
        List<TokenHourData> tokenHourData = tokenHourDataSubgraphFetcher.tokenHourDatas(0, 2,
                TokenHourData_orderBy.periodStartUnix,
                OrderDirection.desc,
                new TokenHourData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });

        if (tokenHourData.size() < 2){
            return null;
        }

        BigDecimal priceCurrent = new BigDecimal(String.valueOf(tokenHourData.get(0).getPriceUSD()));
        BigDecimal pricePrevious = new BigDecimal(String.valueOf(tokenHourData.get(1).getPriceUSD()));

        // 检查除数是否为零
        if (pricePrevious.signum() != 0) {
            BigDecimal change = priceCurrent.subtract(pricePrevious);
            BigDecimal percentChange = change.divide(pricePrevious, 2, RoundingMode.HALF_UP);

            // 返回百分比变化
            Amount amount = new Amount() {
                {
                    setId(getAmountIdEncoded(percentChange.doubleValue(), "USD"));
                    setCurrency(Currency.USD);
                    setValue(percentChange.doubleValue());
                }
            };
            try {
                String pvoStr = JSON.toJSONString(amount, SerializerFeature.WriteNullStringAsEmpty);
                redisService.set(cacheKey, pvoStr);
            } catch (Exception e) {
                log.error("getHourPricePercentChange redis write error：{}", e.getMessage());
            }
            return amount;
        } else {
            // 如果价格为零，可以返回null或者处理这种情况
            return null;
        }
    }


    private Amount getDayPricePercentChange(TokenMarket tokenMarket, String tokenAddress) {

        String cacheKey = "dayPricePercentChange"  + ":" + tokenAddress;

        try {
            Object amount = redisService.get(cacheKey);
            if (null != amount) {
                return JSONObject.parseObject(amount.toString(), Amount.class);
            }
        } catch (Exception e) {
            log.error("getDayPricePercentChange redis read error：{}", e.getMessage());
        }
        List<TokenDayData> tokenDayDatas = tokenDayDataSubgraphFetcher.tokenDayDatas(0, 2,
                TokenDayData_orderBy.date,
                OrderDirection.desc,
                new TokenDayData_filter() {
                    {
                        setToken(tokenAddress.toLowerCase());
                    }
                });
        if (tokenDayDatas.size() < 2){
            return null;
        }
        BigDecimal priceCurrent = new BigDecimal(String.valueOf(tokenDayDatas.get(0).getPriceUSD()));
        BigDecimal pricePrevious = new BigDecimal(String.valueOf(tokenDayDatas.get(1).getPriceUSD()));

        // 检查除数是否为零
        if (pricePrevious.signum() != 0) {
            BigDecimal change = priceCurrent.subtract(pricePrevious);
            BigDecimal percentChange = change.divide(pricePrevious, 2, RoundingMode.HALF_UP);

            // 返回百分比变化
            Amount amount = new Amount() {
                {
                    setId(getAmountIdEncoded(percentChange.doubleValue(), "USD"));
                    setCurrency(Currency.USD);
                    setValue(percentChange.doubleValue());
                }
            };
            try {
                String pvoStr = JSON.toJSONString(amount, SerializerFeature.WriteNullStringAsEmpty);
                redisService.set(cacheKey, pvoStr);
            } catch (Exception e) {
                log.error("getDayPricePercentChange redis write error：{}", e.getMessage());
            }
            return amount;
        } else {
            // 如果价格为零，可以返回null或者处理这种情况
            return null;
        }
    }

    private Amount getWeekPricePercentChange(TokenMarket tokenMarket, String tokenAddress) {

        String cacheKey = "weekPricePercentChange"  + ":" + tokenAddress;
        try {
            Object amount = redisService.get(cacheKey);
            if (null != amount) {
                return JSONObject.parseObject(amount.toString(), Amount.class);
            }
        } catch (Exception e) {
            log.error("getWeekPricePercentChange redis read error：{}", e.getMessage());
        }

        String currentId = tokenAddress.toLowerCase() + ":" + DateUtil.getStartOfDayTimestamp() / 86400;
        String weekDataId = tokenAddress.toLowerCase() + ":" + DateUtil.getAneWeekAgoMidnightTimestamp() / 86400;

        TokenDayData tokenDayData = tokenDayDataSubgraphFetcher.tokenDayData(currentId);
        TokenDayData tokenDayDataWeekAgo = tokenDayDataSubgraphFetcher.tokenDayData(weekDataId);
        if (tokenDayData != null && tokenDayDataWeekAgo != null) {
            tokenMarket.setVolume(new Amount() {{
                setId(getAmountIdEncoded(tokenDayData.getVolumeUSD().doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(tokenDayData.getVolumeUSD().doubleValue());
            }});

            BigDecimal change = new BigDecimal(String.valueOf(tokenDayData.getPriceUSD()))
                    .subtract(new BigDecimal(String.valueOf(tokenDayDataWeekAgo.getPriceUSD())));
            BigDecimal percentChange = change.divide(new BigDecimal(String.valueOf(tokenDayData.getPriceUSD())));
            Amount amount = new Amount() {
                {
                    setId(getAmountIdEncoded(percentChange.doubleValue(), "USD"));
                    setCurrency(Currency.USD);
                    setValue(percentChange.doubleValue());
                }
            };
            try {
                String pvoStr = JSON.toJSONString(amount, SerializerFeature.WriteNullStringAsEmpty);
                redisService.set(cacheKey, pvoStr);
            } catch (Exception e) {
                log.error("getWeekPricePercentChange redis write error：{}", e.getMessage());
            }
            return amount;
        }
        return null;
    }

    private Amount getMonthPricePercentChange(TokenMarket tokenMarket, String tokenAddress) {
        String currentId = tokenAddress.toLowerCase() + ":" + DateUtil.getStartOfDayTimestamp() / 86400;
        String monthDataId = tokenAddress.toLowerCase() + ":" + DateUtil.getAneMonthAgoMidnightTimestamp() / 86400;
        TokenDayData tokenDayData = tokenDayDataSubgraphFetcher.tokenDayData(currentId);
        TokenDayData tokenDayDataMonthAgo = tokenDayDataSubgraphFetcher.tokenDayData(monthDataId);
        if (tokenDayData != null && tokenDayDataMonthAgo != null) {
            tokenMarket.setVolume(new Amount() {{
                setId(getAmountIdEncoded(tokenDayData.getVolumeUSD().doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(tokenDayData.getVolumeUSD().doubleValue());
            }});

            BigDecimal change = new BigDecimal(String.valueOf(tokenDayData.getPriceUSD()))
                    .subtract(new BigDecimal(String.valueOf(tokenDayDataMonthAgo.getPriceUSD())));
            BigDecimal percentChange = change.divide(new BigDecimal(String.valueOf(tokenDayData.getPriceUSD())));
            return new Amount() {
                {
                    setId(getAmountIdEncoded(percentChange.doubleValue(), "USD"));
                    setCurrency(Currency.USD);
                    setValue(percentChange.doubleValue());
                }
            };
        }
        return null;
    }

    private Amount getYearPricePercentChange(TokenMarket tokenMarket, String tokenAddress) {

        String currentId = tokenAddress.toLowerCase() + ":" + DateUtil.getStartOfDayTimestamp() / 86400;
        String yearDataId = tokenAddress.toLowerCase() + ":" + DateUtil.getAneYearAgoMidnightTimestamp() / 86400;
        TokenDayData tokenDayData = tokenDayDataSubgraphFetcher.tokenDayData(currentId);
        TokenDayData tokenDayDataYearAgo = tokenDayDataSubgraphFetcher.tokenDayData(yearDataId);
        if (tokenDayData != null && tokenDayDataYearAgo != null) {
            tokenMarket.setVolume(new Amount() {{
                setId(getAmountIdEncoded(tokenDayData.getVolumeUSD().doubleValue(), "USD"));
                setCurrency(Currency.USD);
                setValue(tokenDayData.getVolumeUSD().doubleValue());
            }});

            BigDecimal change = new BigDecimal(String.valueOf(tokenDayData.getPriceUSD()))
                    .subtract(new BigDecimal(String.valueOf(tokenDayDataYearAgo.getPriceUSD())));
            BigDecimal percentChange = change.divide(new BigDecimal(String.valueOf(tokenDayData.getPriceUSD())));
            return new Amount() {
                {
                    setId(getAmountIdEncoded(percentChange.doubleValue(), "USD"));
                    setCurrency(Currency.USD);
                    setValue(percentChange.doubleValue());
                }
            };
        }
        return null;
    }

}
