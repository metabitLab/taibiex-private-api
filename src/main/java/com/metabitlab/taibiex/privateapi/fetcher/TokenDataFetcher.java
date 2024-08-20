package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;
import java.util.function.Function;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.service.TokenMarketService;
import com.metabitlab.taibiex.privateapi.service.TokenProjectService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;

import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenMarketSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle;
import com.metabitlab.taibiex.privateapi.errors.MissVariableException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportCurrencyException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportDurationException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
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

        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token token = tokenSubgraphFetcher
                .token(address);

        Encoder encoder = Base64.getEncoder();

        String tokenId = encoder.encodeToString(
                ("Token:" + chain + "_" + address).getBytes());

        Token t = new Token() {
            {
                setId(tokenId);
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

        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token t = env.getSource();

        return tokenMarketService.tokenMarket(t, currency);
    }

    @DgsData(parentType = "TokenMarket", field = "pricePercentChange")
    public Amount pricePercentChange(@InputArgument HistoryDuration duration, DgsDataFetchingEnvironment env) {
        TokenMarket tokenMarket = env.getSource();

        return tokenMarketService.getPricePercentChange(tokenMarket, duration);
    }

    @DgsData(parentType = "TokenMarket")
    public Amount priceHighLow(@InputArgument HistoryDuration duration,
                               @InputArgument HighLow highLow,
                               DgsDataFetchingEnvironment env) {
        TokenMarket tokenMarket = env.getSource();

        return tokenMarketService.getPriceHighLow(tokenMarket, duration, highLow);
    }

    @DgsData(parentType = "TokenMarket", field = "volume")
    public Amount volume(@InputArgument HistoryDuration duration, DgsDataFetchingEnvironment env) {
        TokenMarket tokenMarket = env.getSource();

        return tokenMarketService.getVolume(tokenMarket, duration);
    }

    @DgsData(parentType = DgsConstants.TOKEN.TYPE_NAME, field = DgsConstants.TOKEN.Project)
    public TokenProject project(DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token t = env.getSource();

        return tokenProjectService.findByAddress(t);
    }

    @DgsData(parentType = DgsConstants.TOKENPROJECT.TYPE_NAME, field = DgsConstants.TOKENPROJECT.Markets)
    public List<TokenProjectMarket> markets(@InputArgument List<Currency> currencies,
            DgsDataFetchingEnvironment env) {
        for (Currency currency : currencies) {
            if (currency != Currency.USD) {
                throw new UnSupportCurrencyException("This currency is not supported", currency);
            }
        }

        final String chainKey = "chain";
        if (!env.getVariables().containsKey(chainKey)) {
            throw new MissVariableException("chain is required", chainKey);
        }

        Chain chain  = Chain.valueOf((String)env.getVariables().get(chainKey));

        // 原生代币地址为null，所以address允许为null
        String address = env.getArgument("address");

        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token token  = env.getLocalContext();

        Encoder encoder = Base64.getEncoder();

        Bundle bundle = bundleSubgraphFetcher.bundle();
        String priceId = encoder.encodeToString(
                ("Amount:" + bundle.getEthPriceUSD() + "_" + Currency.USD).getBytes());
        String projectId = encoder.encodeToString(
                ("TokenProject:" + chain + "_" + address + "_" + token.getName()).getBytes());
        String projectMarketId = encoder.encodeToString(
                ("TokenProjectMarket:" + projectId + "_" + Currency.USD).getBytes());

        TokenProjectMarket onlyMarket = new TokenProjectMarket() {
            {
                setId(projectMarketId);
                setCurrency(Currency.USD);
                setPrice(new Amount() {
                    {
                        setId(priceId);
                        setCurrency(Currency.USD);
                        setValue(bundle.getEthPriceUSD().doubleValue());
                    }
                });
                // TODO: 以下字段需要填值
                setPricePercentChange24h(null);
                setPriceHigh52w(null);
                setPriceLow52w(null);
                setPriceHistory(null);
                setPricePercentChange(null);
                setPriceHighLow(null);
            }
        };

        return Arrays.asList(onlyMarket);
    }

    @DgsData(parentType = DgsConstants.TOKENPROJECTMARKET.TYPE_NAME, field = DgsConstants.TOKENPROJECTMARKET.TokenProject)
    public TokenProject tokenProject(DgsDataFetchingEnvironment env) {
        Token token = env.getSource();

        return tokenProjectService.findByAddress(token);
    }

    private <T> List<TimestampedOhlc> fetchOhlc(HistoryDuration duration,
            String tokenId,
            Function<String, List<T>> fetcher,
            Function<T, TimestampedOhlc> mapper) {
        List<T> history = fetcher.apply(tokenId);
        if (history == null) {
            return null;
        }

        return history.stream()
                .map(mapper)
                .toList();
    }

    public Amount marketCap() {
        return null;
    }

    public Amount fullyDilutedValuation() {
        return null;
    }

    @DgsData(parentType = DgsConstants.TOKENMARKET.TYPE_NAME, field = DgsConstants.TOKENMARKET.Ohlc)
    public List<TimestampedOhlc> ohlc(@InputArgument HistoryDuration duration,
            DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t = env.getLocalContext();

        Encoder encoder = Base64.getEncoder();

        List<TimestampedOhlc> ohlcList = null;
        switch (duration) {
            case DAY:
                ohlcList = fetchOhlc(duration, t.getId(), tokenMarketSubgraphFetcher::dayOhlcByTokenId, item -> {
                    String ohlcId = encoder.encodeToString(
                            ("TimestampedOhlc:" + item.getPeriodStartUnix()
                                    + "_" + item.getOpen()
                                    + "_" + item.getHigh() + "_"
                                    + item.getLow()
                                    + "_" + item.getClose()).getBytes());
                    String openAmountId = encoder.encodeToString(
                            ("Amount:" + item.getOpen() + "_" + Currency.USD).getBytes());
                    String highAmountId = encoder.encodeToString(
                            ("Amount:" + item.getHigh() + "_" + Currency.USD).getBytes());
                    String lowAmountId = encoder.encodeToString(
                            ("Amount:" + item.getLow() + "_" + Currency.USD).getBytes());
                    String closeAmountId = encoder.encodeToString(
                            ("Amount:" + item.getClose() + "_" + Currency.USD).getBytes());

                    return new TimestampedOhlc() {
                        {
                            setId(ohlcId);
                            setTimestamp(item.getPeriodStartUnix());
                            setOpen(new Amount() {
                                {
                                    setId(openAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getOpen().doubleValue());
                                }
                            });
                            setHigh(new Amount() {
                                {
                                    setId(highAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getHigh().doubleValue());
                                }
                            });
                            setLow(new Amount() {
                                {
                                    setId(lowAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getLow().doubleValue());
                                }
                            });
                            setClose(new Amount() {
                                {
                                    setId(closeAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getClose().doubleValue());
                                }
                            });
                        };
                    };
                });

                break;
            case HOUR:
                ohlcList = fetchOhlc(duration, t.getId(), tokenMarketSubgraphFetcher::hourOhlcByTokenId, item -> {
                    String ohlcId = encoder.encodeToString(
                            ("TimestampedOhlc:" + item.getPeriodStartUnix()
                                    + "_" + item.getOpen()
                                    + "_" + item.getHigh() + "_"
                                    + item.getLow()
                                    + "_" + item.getClose()).getBytes());
                    String openAmountId = encoder.encodeToString(
                            ("Amount:" + item.getOpen() + "_" + Currency.USD).getBytes());
                    String highAmountId = encoder.encodeToString(
                            ("Amount:" + item.getHigh() + "_" + Currency.USD).getBytes());
                    String lowAmountId = encoder.encodeToString(
                            ("Amount:" + item.getLow() + "_" + Currency.USD).getBytes());
                    String closeAmountId = encoder.encodeToString(
                            ("Amount:" + item.getClose() + "_" + Currency.USD).getBytes());

                    return new TimestampedOhlc() {
                        {
                            setId(ohlcId);
                            setTimestamp(item.getPeriodStartUnix());
                            setOpen(new Amount() {
                                {
                                    setId(openAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getOpen().doubleValue());
                                }
                            });
                            setHigh(new Amount() {
                                {
                                    setId(highAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getHigh().doubleValue());
                                }
                            });
                            setLow(new Amount() {
                                {
                                    setId(lowAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getLow().doubleValue());
                                }
                            });
                            setClose(new Amount() {
                                {
                                    setId(closeAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getClose().doubleValue());
                                }
                            });
                        };
                    };
                });

                break;
            case WEEK:
                ohlcList = fetchOhlc(duration, t.getId(), tokenMarketSubgraphFetcher::weekOhlcByTokenId, item -> {
                    String ohlcId = encoder.encodeToString(
                            ("TimestampedOhlc:" + item.getPeriodStartUnix()
                                    + "_" + item.getOpen()
                                    + "_" + item.getHigh() + "_"
                                    + item.getLow()
                                    + "_" + item.getClose()).getBytes());
                    String openAmountId = encoder.encodeToString(
                            ("Amount:" + item.getOpen() + "_" + Currency.USD).getBytes());
                    String highAmountId = encoder.encodeToString(
                            ("Amount:" + item.getHigh() + "_" + Currency.USD).getBytes());
                    String lowAmountId = encoder.encodeToString(
                            ("Amount:" + item.getLow() + "_" + Currency.USD).getBytes());
                    String closeAmountId = encoder.encodeToString(
                            ("Amount:" + item.getClose() + "_" + Currency.USD).getBytes());

                    return new TimestampedOhlc() {
                        {
                            setId(ohlcId);
                            setTimestamp(item.getPeriodStartUnix());
                            setOpen(new Amount() {
                                {
                                    setId(openAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getOpen().doubleValue());
                                }
                            });
                            setHigh(new Amount() {
                                {
                                    setId(highAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getHigh().doubleValue());
                                }
                            });
                            setLow(new Amount() {
                                {
                                    setId(lowAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getLow().doubleValue());
                                }
                            });
                            setClose(new Amount() {
                                {
                                    setId(closeAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getClose().doubleValue());
                                }
                            });
                        };
                    };
                });

                break;
            case MONTH:
                ohlcList = fetchOhlc(duration, t.getId(), tokenMarketSubgraphFetcher::monthOhlcByTokenId, item -> {
                    String ohlcId = encoder.encodeToString(
                            ("TimestampedOhlc:" + item.getPeriodStartUnix()
                                    + "_" + item.getOpen()
                                    + "_" + item.getHigh() + "_"
                                    + item.getLow()
                                    + "_" + item.getClose()).getBytes());
                    String openAmountId = encoder.encodeToString(
                            ("Amount:" + item.getOpen() + "_" + Currency.USD).getBytes());
                    String highAmountId = encoder.encodeToString(
                            ("Amount:" + item.getHigh() + "_" + Currency.USD).getBytes());
                    String lowAmountId = encoder.encodeToString(
                            ("Amount:" + item.getLow() + "_" + Currency.USD).getBytes());
                    String closeAmountId = encoder.encodeToString(
                            ("Amount:" + item.getClose() + "_" + Currency.USD).getBytes());

                    return new TimestampedOhlc() {
                        {
                            setId(ohlcId);
                            setTimestamp(item.getPeriodStartUnix());
                            setOpen(new Amount() {
                                {
                                    setId(openAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getOpen().doubleValue());
                                }
                            });
                            setHigh(new Amount() {
                                {
                                    setId(highAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getHigh().doubleValue());
                                }
                            });
                            setLow(new Amount() {
                                {
                                    setId(lowAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getLow().doubleValue());
                                }
                            });
                            setClose(new Amount() {
                                {
                                    setId(closeAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getClose().doubleValue());
                                }
                            });
                        };
                    };
                });

                break;
            case YEAR:
                ohlcList = fetchOhlc(duration, t.getId(), tokenMarketSubgraphFetcher::yearOhlcByTokenId, item -> {
                    String ohlcId = encoder.encodeToString(
                            ("TimestampedOhlc:" + item.getDate()
                                    + "_" + item.getOpen()
                                    + "_" + item.getHigh() + "_"
                                    + item.getLow()
                                    + "_" + item.getClose()).getBytes());
                    String openAmountId = encoder.encodeToString(
                            ("Amount:" + item.getOpen() + "_" + Currency.USD).getBytes());
                    String highAmountId = encoder.encodeToString(
                            ("Amount:" + item.getHigh() + "_" + Currency.USD).getBytes());
                    String lowAmountId = encoder.encodeToString(
                            ("Amount:" + item.getLow() + "_" + Currency.USD).getBytes());
                    String closeAmountId = encoder.encodeToString(
                            ("Amount:" + item.getClose() + "_" + Currency.USD).getBytes());

                    return new TimestampedOhlc() {
                        {
                            setId(ohlcId);
                            setTimestamp(item.getDate());
                            setOpen(new Amount() {
                                {
                                    setId(openAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getOpen().doubleValue());
                                }
                            });
                            setHigh(new Amount() {
                                {
                                    setId(highAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getHigh().doubleValue());
                                }
                            });
                            setLow(new Amount() {
                                {
                                    setId(lowAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getLow().doubleValue());
                                }
                            });
                            setClose(new Amount() {
                                {
                                    setId(closeAmountId);
                                    setCurrency(Currency.USD);
                                    setValue(item.getClose().doubleValue());
                                }
                            });
                        };
                    };
                });

                break;
            default:
                throw new UnSupportDurationException("This duration is not supported", duration);
        }

        return ohlcList;
    }

    private <T> List<TimestampedAmount> fetchHistory(HistoryDuration duration,
            String tokenId,
            Function<String, List<T>> fetcher,
            Function<T, TimestampedAmount> mapper) {
        List<T> history = fetcher.apply(tokenId);
        if (history == null) {
            return null;
        }

        return history.stream()
                .map(mapper)
                .toList();
    }

    @DgsData(parentType = DgsConstants.TOKENMARKET.TYPE_NAME, field = DgsConstants.TOKENMARKET.PriceHistory)
    public List<TimestampedAmount> priceHistory(@InputArgument HistoryDuration duration,
            DgsDataFetchingEnvironment env) {
        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t = env.getLocalContext();

        Encoder encoder = Base64.getEncoder();

        List<TimestampedAmount> history = null;
        String tokenId = t.getId();

        switch (duration) {
            case DAY:
                history = fetchHistory(duration, tokenId, tokenMarketSubgraphFetcher::dayPriceHistoryByTokenId,
                        item -> {
                            double priceUSD = item.getPriceUSD().doubleValue();
                            int timestamp = item.getPeriodStartUnix();

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
                        });
                break;
            case HOUR:
                history = fetchHistory(duration, tokenId, tokenMarketSubgraphFetcher::hourPriceHistoryByTokenId,
                        item -> {
                            double priceUSD = item.getPriceUSD().doubleValue();
                            int timestamp = item.getPeriodStartUnix();

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
                        });
                break;
            case WEEK:
                history = fetchHistory(duration, tokenId, tokenMarketSubgraphFetcher::weekPriceHistoryByTokenId,
                        item -> {
                            double priceUSD = item.getPriceUSD().doubleValue();
                            int timestamp = item.getPeriodStartUnix();

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
                        });
                break;
            case MONTH:
                history = fetchHistory(duration, tokenId, tokenMarketSubgraphFetcher::monthPriceHistoryByTokenId,
                        item -> {
                            double priceUSD = item.getPriceUSD().doubleValue();
                            int timestamp = item.getPeriodStartUnix();

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
                        });
                break;
            case YEAR:
                history = fetchHistory(duration, tokenId, tokenMarketSubgraphFetcher::yearPriceHistoryByTokenId,
                        item -> {
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
                        });
                break;
            default:
                throw new UnSupportDurationException("This duration is not supported", duration);
        }
        ;

        return history;
    }

    @DgsData(parentType = "Query", field = "topTokens")
    public List<Token> topTokens(@InputArgument Chain chain,
            @InputArgument Integer page,
            @InputArgument Integer pageSize,
            @InputArgument TokenSortableField orderBy) {
        return tokenService.topTokens(chain, page, pageSize, orderBy);
    }

}
