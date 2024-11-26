package com.metabitlab.taibiex.privateapi.service;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.metabitlab.taibiex.privateapi.errors.UnSupportDurationException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.HistoryDuration;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TimestampedAmount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProject;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProjectMarket;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenPriceSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData;
import com.metabitlab.taibiex.privateapi.util.RedisService;

import io.vavr.Tuple2;

/**
 * This class represents the TokenProjectMarketService.
 * 
 * @author: nix
 */
@Service
public class TokenProjectMarketService {
    private static final Logger log = LoggerFactory.getLogger(TokenProjectMarketService.class);

    @Autowired
    TokenProjectService tokenProjectService;

    @Autowired
    TokenService tokenService;

    @Autowired
    BundleSubgraphFetcher bundleSubgraphFetcher;

    @Autowired
    TokenPriceSubgraphFetcher tokenPriceSubgraphFetcher;

    @Autowired
    RedisService redisService;
    
    public TokenProjectMarket getMarketFromToken(Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }

        String cacheKey = "tokenProjectMarket:" +  token.getChain() + "_" + token.getSymbol() + "_" + token.getAddress();

        try {
            Object tokenProjectMarket = redisService.get(cacheKey);
            if (null != tokenProjectMarket) {
                return JSONObject.parseObject(tokenProjectMarket.toString(), TokenProjectMarket.class);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        Tuple2<com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token> tuple = tokenService
                .getTokenFromSubgraphs(token.getChain(), token.getAddress());

        TokenProject project = tokenProjectService.findByAddress(token);

        Encoder encoder = Base64.getEncoder();

        Bundle bundle = bundleSubgraphFetcher.bundle();
        BigDecimal price = tuple._2.getDerivedETH().multiply(bundle.getEthPriceUSD());

        List<TokenDayData> originPriceHistory = tokenPriceSubgraphFetcher.yearPriceHistoryByTokenId(tuple._2.getId());
        List<TimestampedAmount> priceHistory = originPriceHistory.stream().map(item -> {
            double priceUSD = item.getPriceUSD().doubleValue();
            int timestamp = item.getDate();

            String amountId = encoder.encodeToString(
                    ("TimestampedAmount:" + timestamp + "_" + priceUSD + "_" + Currency.USD)
                            .getBytes());

            TimestampedAmount timestampedAmount = new TimestampedAmount() {
                {
                    setId(amountId);
                    setTimestamp(timestamp);
                    setValue(priceUSD);
                    setCurrency(Currency.USD);
                }
            };

            return timestampedAmount;
        }).toList();

        double high = originPriceHistory.stream().map(item -> item.getHigh().doubleValue()).max(Double::compare).get();
        double low = originPriceHistory.stream().map(item -> item.getLow().doubleValue()).min(Double::compare).get();

        String projectId = encoder.encodeToString(
                ("TokenProject:" + token.getChain() + "_" + token.getAddress() + "_" + token.getName()).getBytes());
        String projectMarketId = encoder.encodeToString(
                ("TokenProjectMarket:" + projectId + "_" + Currency.USD).getBytes());
        String priceId = encoder.encodeToString(
                ("Amount:" + price.doubleValue() + "_" + Currency.USD).getBytes());
        String highAmountId = encoder.encodeToString(
                ("Amount:" + high + "_" + Currency.USD).getBytes());
        String lowAmountId = encoder.encodeToString(
                ("Amount:" + low + "_" + Currency.USD).getBytes());

        TokenProjectMarket onlyOneMarket = new TokenProjectMarket() {
            {
                setId(projectMarketId);
                setTokenProject(project);
                setCurrency(Currency.USD);

                setPrice(new Amount() {
                    {
                        setId(priceId);
                        setCurrency(Currency.USD);
                        setValue(price.doubleValue());
                    }
                });
                // // TODO: 需要填值，参考文档
                // setPricePercentChange(null);
                // setPricePercentChange24h(null);
                // setPriceHighLow(null);
                setPriceHigh52w(new Amount() {
                    {
                        setId(highAmountId);
                        setCurrency(Currency.USD);
                        setValue(high);
                    }
                });
                setPriceLow52w(new Amount() {
                    {
                        setId(lowAmountId);
                        setCurrency(Currency.USD);
                        setValue(low);
                    }
                });
                setPriceHistory(priceHistory);
            }
        };

        try {
            String pvoStr = JSON.toJSONString(onlyOneMarket, SerializerFeature.WriteNullStringAsEmpty);
            redisService.set(cacheKey, pvoStr, 2, TimeUnit.MINUTES);
        } catch (Exception e) {
           e.fillInStackTrace();
        }

        return onlyOneMarket;
    }

    public Amount pricePercentChange(HistoryDuration duration, TokenProjectMarket market) {
        switch (duration) {
            case FIVE_MINUTE:
                return fiveMinutesPricePercentChange(market);
            case HOUR:
                return hourPricePercentChange(market);
            case DAY:
                return dayPricePercentChange(market);
            case WEEK:
                return weekPricePercentChange(market);
            case MONTH:
                return monthPricePercentChange(market);
            case YEAR:
                return yearPricePercentChange(market);
            default:
                throw new UnSupportDurationException("This duration is not supported", duration);
        }
    }

    private Amount fiveMinutesPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount hourPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount dayPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount weekPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount monthPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount yearPricePercentChange(TokenProjectMarket market) {
        return null;
    }
}
