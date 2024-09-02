package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;
import java.util.function.Function;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.service.TokenMarketService;
import com.metabitlab.taibiex.privateapi.service.TokenProjectMarketService;
import com.metabitlab.taibiex.privateapi.service.TokenProjectService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.concurrent.TimeUnit;

import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenPriceSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TransactionsSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle;
import com.metabitlab.taibiex.privateapi.util.RedisService;
import com.metabitlab.taibiex.privateapi.errors.MissSourceException;
import com.metabitlab.taibiex.privateapi.errors.MissVariableException;
import com.metabitlab.taibiex.privateapi.errors.ParseCacheException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportChainException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportCurrencyException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportDurationException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import io.vavr.Tuple2;

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

    private final Chain TABI = Chain.ETHEREUM;

    @Autowired
    TokenProjectMarketService tokenProjectMarketService;

    @Autowired
    BundleSubgraphFetcher bundleSubgraphFetcher;

    @Autowired
    TokenSubgraphFetcher tokenSubgraphFetcher;

    @Autowired
    TokenPriceSubgraphFetcher tokenMarketSubgraphFetcher;

    @Autowired
    TransactionsSubgraphFetcher transactionsSubgraphFetcher;

    @Autowired
    RedisService redisService;

    @DgsQuery
    public Token token(@InputArgument Chain chain,
            @InputArgument String address) {
        // NOTE: [已确认] 参数 chain 未使用, 仅支持 TABI 
        if (chain != TABI) {
            throw new UnSupportChainException("Those chains are not supported", Arrays.asList(chain));
        }

        Tuple2<
            com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, 
            com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token
        > tuple = tokenService
                .getTokenFromSubgraphs(TABI, address);

        return tuple._1;
    }

    @DgsData(parentType = DgsConstants.TOKEN.TYPE_NAME, field = DgsConstants.TOKEN.Market)
    public TokenMarket market(@InputArgument Currency currency, DgsDataFetchingEnvironment env) {
        if (currency != Currency.USD) {
            throw new UnSupportCurrencyException("This currency is not supported", Arrays.asList(currency));
        }

        Token t = env.getSource();

        return tokenMarketService.tokenMarket(t, currency);
    }

    @DgsData(parentType = DgsConstants.TOKEN.TYPE_NAME, field = DgsConstants.TOKEN.V3Transactions)
    public List<PoolTransaction> v3Transactions(@InputArgument Integer first,
            @InputArgument("timestampCursor") Integer cursor,
            DgsDataFetchingEnvironment env) {
        Token token = env.getSource();
        if (token == null) {
            throw new MissSourceException("Token is required", "token");
        }

        List<PoolTransaction> swapsList = transactionsSubgraphFetcher.swapsTransactions(0, first, cursor, token);
        
        return swapsList;
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
        try {
            Object wrappedTokenProject = redisService.get(t.getId());
            if (wrappedTokenProject != null) {
                return (TokenProject) wrappedTokenProject;
            }

            TokenProject tokenProject = tokenProjectService.findByAddress(t);
            redisService.set(t.getId(), tokenProject, 20, TimeUnit.MINUTES);

            return tokenProject;
        } catch (Exception e) {
            throw new ParseCacheException("Error parsing cache", t.getId());
        }
    }

    @DgsData(parentType = DgsConstants.TOKENPROJECT.TYPE_NAME, field = DgsConstants.TOKENPROJECT.Markets)
    public List<TokenProjectMarket> markets(@InputArgument List<Currency> currencies,
            DgsDataFetchingEnvironment env) {
        if (currencies == null) {
            throw new IllegalArgumentException("currencies is required");
        }
        if (currencies.size() > 1 || currencies.get(0) != Currency.USD) {
            throw new UnSupportCurrencyException("Only the currency USD is supported", currencies);
        }

        final String chainKey = "chain";
        if (!env.getVariables().containsKey(chainKey)) {
            throw new MissVariableException("chain is required", chainKey);
        }

        Chain chain = Chain.valueOf((String) env.getVariables().get(chainKey));

        // 原生代币地址为null，所以address允许为null
        String address = env.getArgument("address");

        Tuple2<
            com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, 
            com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token
        > tuple = tokenService
                .getTokenFromSubgraphs(chain, address);

        TokenProjectMarket onlyOneMarket = tokenProjectMarketService.getMarketFromToken(tuple._1);

        return Arrays.asList(onlyOneMarket);
    }

    @DgsData(parentType = DgsConstants.TOKENPROJECTMARKET.TYPE_NAME, field = DgsConstants.TOKENPROJECTMARKET.TokenProject)
    public TokenProject tokenProject(DgsDataFetchingEnvironment env) {
        TokenProjectMarket projectMarket = env.getSource();

        return projectMarket.getTokenProject();
    }

    @DgsData(parentType = DgsConstants.TOKENPROJECTMARKET.TYPE_NAME, field = DgsConstants.TOKENPROJECTMARKET.MarketCap)
    public Amount marketCap(DgsDataFetchingEnvironment env) {
        TokenProjectMarket tokenProjectMarket = env.getSource();
        if (tokenProjectMarket == null) {
            throw new MissSourceException("TokenProjectMarket is required", "tokenProjectMarket");
        }

        List<Token> tokens = tokenProjectMarket.getTokenProject().getTokens();
        if (tokens == null || tokens.size() == 0) {
            throw new MissSourceException("Tokens is required", "tokens");
        }

        Token token = tokenProjectMarket.getTokenProject().getTokens().get(0);
        if (token == null) {
            throw new MissSourceException("Token is required", "token");
        }

        Tuple2<
            com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token,
            com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token
        > tuple = tokenService
                .getTokenFromSubgraphs(token.getChain(), token.getAddress());

        Bundle bundle = bundleSubgraphFetcher.bundle();

        // NOTE: [已确认] 此处存疑，如果采用 totalSupply 作为流通量，那么市值计算方式与 fullyDilutedValuation 一致

        BigDecimal price = tuple._2.getDerivedETH().multiply(bundle.getEthPriceUSD());
        BigInteger totalSupply = tuple._2.getTotalSupply();

        double marketCap = totalSupply.intValue() * price.doubleValue();

        Encoder encoder = Base64.getEncoder();
        String amountId = encoder.encodeToString(
                ("Amount:" + marketCap + "_" + Currency.USD).getBytes());

        return new Amount() {
            {
                setId(amountId);
                setValue(marketCap);
                setCurrency(Currency.USD);
            }
        };
    }

    @DgsData(parentType = DgsConstants.TOKENPROJECTMARKET.TYPE_NAME, field = DgsConstants.TOKENPROJECTMARKET.FullyDilutedValuation)
    public Amount fullyDilutedValuation(DgsDataFetchingEnvironment env) {
        TokenProjectMarket tokenProjectMarket = env.getSource();
        if (tokenProjectMarket == null) {
            throw new MissSourceException("TokenProjectMarket is required", "tokenProjectMarket");
        }

        List<Token> tokens = tokenProjectMarket.getTokenProject().getTokens();
        if (tokens == null || tokens.size() == 0) {
            throw new MissSourceException("Tokens is required", "tokens");
        }

        Token token = tokenProjectMarket.getTokenProject().getTokens().get(0);
        if (token == null) {
            throw new MissSourceException("Tokens is required", "tokens");
        }

        Tuple2<
            com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, 
            com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token
        > tuple = tokenService
                .getTokenFromSubgraphs(token.getChain(), token.getAddress());

        Bundle bundle = bundleSubgraphFetcher.bundle();

        BigDecimal price = tuple._2.getDerivedETH().multiply(bundle.getEthPriceUSD());
        BigInteger totalSupply = tuple._2.getTotalSupply();

        double fullyDilutedValuation = totalSupply.intValue() * price.doubleValue();

        Encoder encoder = Base64.getEncoder();
        String amountId = encoder.encodeToString(
                ("Amount:" + fullyDilutedValuation + "_" + Currency.USD).getBytes());

        return new Amount() {
            {
                setId(amountId);
                setValue(fullyDilutedValuation);
                setCurrency(Currency.USD);
            }
        };
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

    @DgsData(parentType = DgsConstants.TOKENMARKET.TYPE_NAME, field = DgsConstants.TOKENMARKET.Ohlc)
    public List<TimestampedOhlc> ohlc(@InputArgument HistoryDuration duration,
            DgsDataFetchingEnvironment env) {
        TokenMarket tokenMarket = env.getSource();
        if (tokenMarket == null) {
            throw new MissSourceException("TokenMarket is required", "tokenMarket");
        }

        Token token = tokenMarket.getToken();
        if (token == null) {
            throw new MissSourceException("Token is required", "token");
        }

        Tuple2<
            com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, 
            com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token
        > tuple = tokenService
                .getTokenFromSubgraphs(token.getChain(), token.getAddress());


        Encoder encoder = Base64.getEncoder();

        List<TimestampedOhlc> ohlcList = null;
        switch (duration) {
            case DAY:
                ohlcList = fetchOhlc(duration, tuple._2.getId(), tokenMarketSubgraphFetcher::dayOhlcByTokenId, item -> {
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
                ohlcList = fetchOhlc(duration, tuple._2.getId(), tokenMarketSubgraphFetcher::hourOhlcByTokenId, item -> {
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
                ohlcList = fetchOhlc(duration, tuple._2.getId(), tokenMarketSubgraphFetcher::weekOhlcByTokenId, item -> {
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
                ohlcList = fetchOhlc(duration, tuple._2.getId(), tokenMarketSubgraphFetcher::monthOhlcByTokenId, item -> {
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
                ohlcList = fetchOhlc(duration, tuple._2.getId(), tokenMarketSubgraphFetcher::yearOhlcByTokenId, item -> {
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
        TokenMarket tokenMarket = env.getSource();
        if (tokenMarket == null) {
            throw new MissSourceException("TokenMarket is required", "tokenMarket");
        }

        Token token = tokenMarket.getToken();
        if (token == null) {
            throw new MissSourceException("Token is required", "token");
        }

        Tuple2<
            com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, 
            com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token
        > tuple = tokenService
            .getTokenFromSubgraphs(token.getChain(), token.getAddress());

        Encoder encoder = Base64.getEncoder();

        List<TimestampedAmount> history = null;
        String tokenId = tuple._2.getId();

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

    @DgsData(parentType = DgsConstants.QUERY.TYPE_NAME)
    public List<Token> tokens(@InputArgument(name = "contracts")  List<ContractInput> contractInput) {
        return tokenService.tokens(contractInput);
    }

}
