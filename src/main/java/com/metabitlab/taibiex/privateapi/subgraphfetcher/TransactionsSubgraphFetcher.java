package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PoolTransaction;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PoolTransactionType;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.ProtocolVersion;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.BurnsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.BurnsProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.MintsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.MintsProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.SwapsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.SwapsProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Burn;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Burn_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Burn_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Mint;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Mint_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Mint_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Swap;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Swap_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Swap_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_filter;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

/**
 * This class fetches transactions from the subgraph.
 * 
 * @author: nix
 */
@Service
public class TransactionsSubgraphFetcher {
    @Autowired
    SubgraphsClient subgraphsClient;

    @Autowired
    TokenService tokenService;

    public List<PoolTransaction> mintsTransactions(Integer skip, Integer first, Integer cursor, Chain chain,
            String poolAddress) {
        Mint_filter filter = new Mint_filter();
        if (cursor != null) {
            filter.setTimestamp_gte(BigInteger.valueOf(cursor));
        }
        if (poolAddress != null) {
            filter.setPool(poolAddress);
        }

        MintsGraphQLQuery.Builder builder = MintsGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(Mint_orderBy.timestamp)
                .orderDirection(OrderDirection.desc)
                .where(filter)
                .queryName("TransactionsSubgraphFetcher_mintsTransactions");

        GraphQLQueryRequest request = new GraphQLQueryRequest(
                builder.build(),
                new MintsProjectionRoot<>()
                        .id()
                        .transaction()
                        .id()
                        .parent()
                        .amount0()
                        .amount1()
                        .amountUSD()
                        .origin()
                        .timestamp()
                        .token1()
                        .id()
                        .name()
                        .symbol()
                        .decimals()
                        .parent()
                        .token0()
                        .id()
                        .name()
                        .symbol()
                        .decimals()
                        .parent());

        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        List<Mint> list = response.extractValueAsObject("mints", new TypeRef<List<Mint>>() {
        });
        Encoder encoder = Base64.getEncoder();

        return list.stream().map(item -> {
            String transactionId = encoder.encodeToString(
                    ("PoolTransaction:" + chain + "_" + item.getId() + "_" + PoolTransactionType.ADD + "_"
                            + item.getToken0().getId() + "_" + item.getToken1().getId()).getBytes());
            double usdValue = item.getAmountUSD().doubleValue();
            String usdValueId = encoder.encodeToString(
                    ("Amount:" + usdValue + "_" + Currency.USD).getBytes());

            // NOTE: 直接查询数据库，恐怕会有问题
            Token token0 = tokenService.token(chain, item.getToken0().getId());
            Token token1 = tokenService.token(chain, item.getToken1().getId());

            PoolTransaction transaction = new PoolTransaction();
            transaction.setId(transactionId);
            transaction.setChain(chain);
            transaction.setProtocolVersion(ProtocolVersion.V3);
            transaction.setType(PoolTransactionType.ADD);
            transaction.setHash(item.getTransaction().getId());
            transaction.setTimestamp(item.getTimestamp().intValue());
            transaction.setUsdValue(new Amount() {
                {
                    setId(usdValueId);
                    setValue(usdValue);
                    setCurrency(Currency.USD);
                }
            });
            transaction.setAccount(item.getOrigin());
            transaction.setToken0(new Token() {
                {
                    setId(item.getToken0().getId());
                    setChain(chain);
                    setAddress(item.getToken0().getId());
                    setStandard(token0.getStandard());
                    setDecimals(item.getToken0().getDecimals().intValue());
                    setName(item.getToken0().getName());
                    setSymbol(item.getToken0().getSymbol());
                }
            });
            transaction.setToken0Quantity(item.getAmount0().toString());
            transaction.setToken1(new Token() {
                {
                    setId(item.getToken1().getId());
                    setChain(chain);
                    setAddress(item.getToken0().getId());
                    setStandard(token1.getStandard());
                    setDecimals(item.getToken1().getDecimals().intValue());
                    setName(item.getToken1().getName());
                    setSymbol(item.getToken1().getSymbol());
                }
            });
            transaction.setToken1Quantity(item.getAmount1().toString());
            return transaction;
        }).toList();
    }

    public List<PoolTransaction> burnsTransactions(Integer skip, Integer first, Integer cursor, Chain chain,
            String poolAddress) {
        Burn_filter filter = new Burn_filter();
        if (cursor != null) {
            filter.setTimestamp_gte(BigInteger.valueOf(cursor));
        }
        if (poolAddress != null) {
            filter.setPool(poolAddress);
        }

        BurnsGraphQLQuery.Builder builder = BurnsGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(Burn_orderBy.timestamp)
                .orderDirection(OrderDirection.desc)
                .where(filter)
                .queryName("TransactionsSubgraphFetcher_burnsTransactions");

        GraphQLQueryRequest request = new GraphQLQueryRequest(
                builder.build(),
                new BurnsProjectionRoot<>()
                        .id()
                        .transaction()
                        .id()
                        .parent()
                        .amount0()
                        .amount1()
                        .amountUSD()
                        .origin()
                        .timestamp()
                        .token1()
                        .id()
                        .name()
                        .symbol()
                        .decimals()
                        .parent()
                        .token0()
                        .id()
                        .name()
                        .symbol()
                        .decimals()
                        .parent());

        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        List<Burn> list = response.extractValueAsObject("burns", new TypeRef<List<Burn>>() {
        });
        Encoder encoder = Base64.getEncoder();

        return list.stream().map(item -> {
            String transactionId = encoder.encodeToString(
                    ("PoolTransaction:" + chain + "_" + item.getId() + "_" + PoolTransactionType.ADD + "_"
                            + item.getToken0().getId() + "_" + item.getToken1().getId()).getBytes());
            double usdValue = item.getAmountUSD().doubleValue();
            String usdValueId = encoder.encodeToString(
                    ("Amount:" + usdValue + "_" + Currency.USD).getBytes());

            // NOTE: 直接查询数据库，恐怕会有问题
            Token token0 = tokenService.token(chain, item.getToken0().getId());
            Token token1 = tokenService.token(chain, item.getToken1().getId());

            PoolTransaction transaction = new PoolTransaction();
            transaction.setId(transactionId);
            transaction.setChain(chain);
            transaction.setProtocolVersion(ProtocolVersion.V3);
            transaction.setType(PoolTransactionType.REMOVE);
            transaction.setHash(item.getTransaction().getId());
            transaction.setTimestamp(item.getTimestamp().intValue());
            transaction.setUsdValue(new Amount() {
                {
                    setId(usdValueId);
                    setValue(usdValue);
                    setCurrency(Currency.USD);
                }
            });
            transaction.setAccount(item.getOrigin());
            transaction.setToken0(new Token() {
                {
                    setId(item.getToken0().getId());
                    setChain(chain);
                    setAddress(item.getToken0().getId());
                    setStandard(token0.getStandard());
                    setDecimals(item.getToken0().getDecimals().intValue());
                    setName(item.getToken0().getName());
                    setSymbol(item.getToken0().getSymbol());
                }
            });
            transaction.setToken0Quantity(item.getAmount0().toString());
            transaction.setToken1(new Token() {
                {
                    setId(item.getToken1().getId());
                    setChain(chain);
                    setAddress(item.getToken0().getId());
                    setStandard(token1.getStandard());
                    setDecimals(item.getToken1().getDecimals().intValue());
                    setName(item.getToken1().getName());
                    setSymbol(item.getToken1().getSymbol());
                }
            });
            transaction.setToken1Quantity(item.getAmount1().toString());
            return transaction;
        }).toList();
    }

    public List<PoolTransaction> swapsTransactions(Integer skip, Integer first, Integer cursor, Chain chain,
            String poolAddress) {
        Swap_filter filter = new Swap_filter();
        if (cursor != null) {
            filter.setTimestamp_gte(BigInteger.valueOf(cursor));
        }
        if (poolAddress != null) {
            filter.setPool(poolAddress);
        }

        return executeSwapsTransactionQuery(filter, skip, first, chain);
    }

    public List<PoolTransaction> swapsTransactions(Integer skip, Integer first, Integer cursor, Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Token is required");
        }

        Swap_filter filter = new Swap_filter();
        if (cursor != null) {
            filter.setTimestamp_gte(BigInteger.valueOf(cursor));
        }

        filter.setOr(Arrays.asList(
                new Swap_filter() {
                    {
                        setToken0_(new Token_filter() {
                            {
                                setId(token.getAddress());
                            }
                        });
                    }
                },
                new Swap_filter() {
                    {
                        setToken1_(new Token_filter() {
                            {
                                setId(token.getAddress());
                            }
                        });
                    }
                }));

        return executeSwapsTransactionQuery(filter, skip, first, token.getChain());
    }

    private List<PoolTransaction> executeSwapsTransactionQuery(Swap_filter filter, Integer skip, Integer first,
            Chain chain) {
        SwapsGraphQLQuery.Builder builder = SwapsGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .orderBy(Swap_orderBy.timestamp)
                .orderDirection(OrderDirection.desc)
                .where(filter)
                .queryName("TransactionsSubgraphFetcher_swapsTransactions");

        GraphQLQueryRequest request = new GraphQLQueryRequest(
                builder.build(),
                new SwapsProjectionRoot<>()
                        .id()
                        .transaction()
                        .id()
                        .parent()
                        .amount0()
                        .amount1()
                        .amountUSD()
                        .origin()
                        .timestamp()
                        .token1()
                        .id()
                        .name()
                        .symbol()
                        .decimals()
                        .parent()
                        .token0()
                        .id()
                        .name()
                        .symbol()
                        .decimals()
                        .parent());

        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        List<Swap> list = response.extractValueAsObject("swaps", new TypeRef<List<Swap>>() {
        });
        Encoder encoder = Base64.getEncoder();

        return list.stream().map(item -> {
            String transactionId = encoder.encodeToString(
                    ("PoolTransaction:" + chain + "_" + item.getId() + "_" + PoolTransactionType.ADD + "_"
                            + item.getToken0().getId() + "_" + item.getToken1().getId()).getBytes());
            double usdValue = item.getAmountUSD().doubleValue();
            String usdValueId = encoder.encodeToString(
                    ("Amount:" + usdValue + "_" + Currency.USD).getBytes());

            // NOTE: 直接查询数据库，恐怕会有问题
            Token token0 = tokenService.token(chain, item.getToken0().getId());
            Token token1 = tokenService.token(chain, item.getToken1().getId());

            PoolTransaction transaction = new PoolTransaction();
            transaction.setId(transactionId);
            transaction.setChain(chain);
            transaction.setProtocolVersion(ProtocolVersion.V3);
            transaction.setType(PoolTransactionType.SWAP);
            transaction.setHash(item.getTransaction().getId());
            transaction.setTimestamp(item.getTimestamp().intValue());
            transaction.setUsdValue(new Amount() {
                {
                    setId(usdValueId);
                    setValue(usdValue);
                    setCurrency(Currency.USD);
                }
            });
            transaction.setAccount(item.getOrigin());
            transaction.setToken0(new Token() {
                {
                    setId(item.getToken0().getId());
                    setChain(chain);
                    setAddress(item.getToken0().getId());
                    setStandard(token0.getStandard());
                    setDecimals(item.getToken0().getDecimals().intValue());
                    setName(item.getToken0().getName());
                    setSymbol(item.getToken0().getSymbol());
                }
            });
            transaction.setToken0Quantity(item.getAmount0().toString());
            transaction.setToken1(new Token() {
                {
                    setId(item.getToken1().getId());
                    setChain(chain);
                    setAddress(item.getToken0().getId());
                    setStandard(token1.getStandard());
                    setDecimals(item.getToken1().getDecimals().intValue());
                    setName(item.getToken1().getName());
                    setSymbol(item.getToken1().getSymbol());
                }
            });
            transaction.setToken1Quantity(item.getAmount1().toString());
            return transaction;
        }).toList();
    }
}
