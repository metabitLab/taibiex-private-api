package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.service.TokenMarketService;
import com.metabitlab.taibiex.privateapi.service.TokenProjectService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Base64;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenSortableField;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenStandard;
import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenMarketSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenHourData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenMinuteData;
import com.metabitlab.taibiex.privateapi.errors.UnSupportCurrencyException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportDurationException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.FeeData;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.HistoryDuration;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PriceSource;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TimestampedAmount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TimestampedOhlc;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenMarket;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProject;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProjectMarket;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import graphql.execution.DataFetcherResult;

@DgsComponent
public class TokenDataFetcher {

    private final TokenService tokenService;

    private final TokenProjectService tokenProjectService;


    private final TokenMarketService tokenMarketService;

    public TokenDataFetcher(TokenService tokenService,
                            TokenProjectService tokenProjectService,
                            TokenMarketService tokenMarketService) {
        this.tokenService = tokenService;
        this.tokenProjectService = tokenProjectService;
        this.tokenMarketService = tokenMarketService;
    }

    @Autowired
    BundleSubgraphFetcher bundleSubgraphFetcher;

    @Autowired
    TokenSubgraphFetcher tokenSubgraphFetcher;

    @Autowired
    TokenMarketSubgraphFetcher tokenMarketSubgraphFetcher;

    @DgsQuery
    public DataFetcherResult<Token> token(@InputArgument Chain chain,
                                          @InputArgument String address) {

        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token token
                = tokenSubgraphFetcher.token(address);

        Token t = new Token() {
            {
                setId(token.getId());
                setChain(chain);
                setAddress(address);
                setStandard(TokenStandard.ERC20);
                setName(token.getName());
                setSymbol(token.getSymbol());
                setFeeData(new FeeData() {
                    {
                        // TODO: 字段填值
                    }
                });
            }
        };

        return DataFetcherResult.<Token>newResult()
                .data(t)
                .localContext(token)
                .build();
    }

    @DgsData(parentType = DgsConstants.TOKEN.TYPE_NAME, field = DgsConstants.TOKEN.Market)
    public TokenMarket market(@InputArgument Currency currency, DgsDataFetchingEnvironment env) {
        if (currency != Currency.USD) {
            throw new UnSupportCurrencyException("This currency is not supported", currency);
        }

        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token t
                = env.getSource();

        return tokenMarketService.tokenMarket(t);
    }

    @DgsData(parentType = "TokenMarket", field = "pricePercentChange")
    public Amount pricePercentChange(@InputArgument HistoryDuration duration, DgsDataFetchingEnvironment env) {
        TokenMarket tokenMarket = env.getSource();

        return tokenMarketService.getPricePercentChange(tokenMarket, duration);
    }

    @DgsData(parentType = "TokenMarket", field = "volume")
    public Amount volume(@InputArgument HistoryDuration duration, DgsDataFetchingEnvironment env) {
        TokenMarket tokenMarket = env.getSource();

        return tokenMarketService.getVolume(tokenMarket, duration);
    }

    @DgsData(parentType = DgsConstants.TOKEN.TYPE_NAME, field = DgsConstants.TOKEN.Project)
    public TokenProject project(DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token t
                = env.getSource();

        return tokenProjectService.findByAddress(t);
    }

    @DgsData(parentType = DgsConstants.TOKENPROJECT.TYPE_NAME, field = DgsConstants.TOKENPROJECT.Markets)
    public List<TokenProjectMarket> markets(@InputArgument List<Currency> currencies,
                                            DgsDataFetchingEnvironment env) {
        System.out.println(currencies);

        // TODO: 未获取真实数据
        List<TokenProjectMarket> markets = Arrays.asList(
                new TokenProjectMarket() {
                    {
                        setId("uuid 1");
                        setCurrency(Currency.AUD);
                    }
                },
                new TokenProjectMarket() {
                    {
                        setId("uuid 2");
                        setCurrency(Currency.USD);
                    }
                }
        );

        return markets.stream()
                .filter(market -> currencies.contains(market.getCurrency()))
                .toList();
    }

    @DgsData(parentType = DgsConstants.TOKENMARKET.TYPE_NAME, field = DgsConstants.TOKENMARKET.Ohlc)
    public List<TimestampedOhlc> ohlc(@InputArgument HistoryDuration duration,
                                      DgsDataFetchingEnvironment env) {
        if (duration != HistoryDuration.DAY) {
            throw new UnSupportDurationException("This duration is not supported", duration);
        }

        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t
                = env.getLocalContext();

        List<TokenMinuteData> tokenMinuteDataList = tokenMarketSubgraphFetcher.ohlcByTokenId(t.getId());
        if (tokenMinuteDataList == null) {
            return null;
        }

        return tokenMinuteDataList.stream()
                .map(item -> {
                    TimestampedOhlc ohlc = new TimestampedOhlc();

                    ohlc.setId(item.getId());
                    ohlc.setTimestamp(item.getPeriodStartUnix());
                    ohlc.setClose(new Amount() {
                        {
                            setCurrency(Currency.USD);
                            setValue(item.getClose().doubleValue());
                            setId("uuid");
                        }
                    });
                    ohlc.setHigh(new Amount() {
                        {
                            setCurrency(Currency.USD);
                            setValue(item.getHigh().doubleValue());
                            setId("uuid");
                        }
                    });
                    ohlc.setLow(new Amount() {
                        {
                            setCurrency(Currency.USD);
                            setValue(item.getLow().doubleValue());
                            setId("uuid");
                        }
                    });
                    ohlc.setOpen(new Amount() {
                        {
                            setCurrency(Currency.USD);
                            setValue(item.getOpen().doubleValue());
                            setId("uuid");
                        }
                    });

                    return ohlc;
                })
                .toList();
    }

    @DgsData(parentType = DgsConstants.TOKENMARKET.TYPE_NAME, field = DgsConstants.TOKENMARKET.PriceHistory)
    public List<TimestampedAmount> priceHistory(@InputArgument HistoryDuration duration,
                                                DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t
                = env.getLocalContext();

        if (duration == HistoryDuration.DAY) {
            List<TokenMinuteData> history = tokenMarketSubgraphFetcher.dayPriceHistoryByTokenId(t.getId());
            if (history == null) {
                return null;
            }

            return history.stream()
                    .map(item -> {
                        TimestampedAmount timestampedAmount = new TimestampedAmount();

                        timestampedAmount.setId(item.getId());
                        timestampedAmount.setTimestamp(item.getPeriodStartUnix());
                        timestampedAmount.setValue(item.getPriceUSD().doubleValue());
                        timestampedAmount.setCurrency(Currency.USD);

                        return timestampedAmount;
                    })
                    .toList();
        }
        if (duration == HistoryDuration.HOUR) {
            List<TokenMinuteData> history = tokenMarketSubgraphFetcher.hourPriceHistoryByTokenId(t.getId());
            if (history == null) {
                return null;
            }

            return history.stream()
                    .map(item -> {
                        TimestampedAmount timestampedAmount = new TimestampedAmount();

                        timestampedAmount.setId(item.getId());
                        timestampedAmount.setTimestamp(item.getPeriodStartUnix());
                        timestampedAmount.setValue(item.getPriceUSD().doubleValue());
                        timestampedAmount.setCurrency(Currency.USD);

                        return timestampedAmount;
                    })
                    .toList();
        }
        if (duration == HistoryDuration.WEEK) {
            List<TokenHourData> history = tokenMarketSubgraphFetcher.weekPriceHistoryByTokenId(t.getId());
            if (history == null) {
                return null;
            }

            return history.stream()
                    .map(item -> {
                        TimestampedAmount timestampedAmount = new TimestampedAmount();

                        timestampedAmount.setId(item.getId());
                        timestampedAmount.setTimestamp(item.getPeriodStartUnix());
                        timestampedAmount.setValue(item.getPriceUSD().doubleValue());
                        timestampedAmount.setCurrency(Currency.USD);

                        return timestampedAmount;
                    })
                    .toList();
        }
        if (duration == HistoryDuration.MONTH) {
            List<TokenHourData> history = tokenMarketSubgraphFetcher.monthPriceHistoryByTokenId(t.getId());
            if (history == null) {
                return null;
            }

            return history.stream()
                    .map(item -> {
                        TimestampedAmount timestampedAmount = new TimestampedAmount();

                        timestampedAmount.setId(item.getId());
                        timestampedAmount.setTimestamp(item.getPeriodStartUnix());
                        timestampedAmount.setValue(item.getPriceUSD().doubleValue());
                        timestampedAmount.setCurrency(Currency.USD);

                        return timestampedAmount;
                    })
                    .toList();
        }
        if (duration == HistoryDuration.YEAR) {
            List<TokenDayData> history = tokenMarketSubgraphFetcher.yearPriceHistoryByTokenId(t.getId());
            if (history == null) {
                return null;
            }

            return history.stream()
                    .map(item -> {
                        TimestampedAmount timestampedAmount = new TimestampedAmount();

                        timestampedAmount.setId(item.getId());
                        timestampedAmount.setTimestamp(item.getDate());
                        timestampedAmount.setValue(item.getClose().doubleValue());
                        timestampedAmount.setCurrency(Currency.USD);

                        return timestampedAmount;
                    })
                    .toList();
        }

        throw new UnSupportDurationException("This duration is not supported", duration);
    }

    @DgsQuery
    public List<Token> topTokens(@InputArgument Chain chain,
                                 @InputArgument Integer page,
                                 @InputArgument Integer pageSize,
                                 @InputArgument TokenSortableField orderBy) {
        return tokenService.topTokens(chain, page, pageSize, orderBy);
    }

}
