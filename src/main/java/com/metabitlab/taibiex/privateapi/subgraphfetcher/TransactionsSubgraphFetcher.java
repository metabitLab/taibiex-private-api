package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import java.math.BigInteger;
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
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TransactionProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TransactionsGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Burn_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Mint_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Swap_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Transaction;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Transaction_filter;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

@Service
public class TransactionsSubgraphFetcher {
    @Autowired
    SubgraphsClient subgraphsClient;

    @Autowired
    TokenService tokenService;

    public List<PoolTransaction> mintsTransactions(Integer skip, Integer first, Integer cursor, Chain chain) {
        TransactionsGraphQLQuery.Builder builder = TransactionsGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .queryName("TransactionsSubgraphFetcher_mintsTransactions");

        if (cursor != null) {
            builder.where(new Transaction_filter() {
                {
                    setTimestamp_gte(BigInteger.valueOf(cursor));
                }
            });
        }

        GraphQLQueryRequest request = new GraphQLQueryRequest(
                builder.build(),
                new TransactionProjectionRoot<>()
                        .id()
                        .timestamp()
                        .mints(0, 1000, Mint_orderBy.timestamp, OrderDirection.desc, null)
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
                                .parent()
                        .root());

        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        List<Transaction> list = response.extractValueAsObject("transactions", new TypeRef<List<Transaction>>() {});
        Encoder encoder = Base64.getEncoder();

        return list.stream().flatMap(item -> {
            return item.getMints().stream().map(mint -> {
                String transactionId = encoder.encodeToString(
                        ("PoolTransaction:" + chain + "_" + item.getId() + "_" + PoolTransactionType.ADD + "_"
                                + mint.getToken0().getId() + "_" + mint.getToken1().getId()).getBytes());

                double usdValue = mint.getAmountUSD().doubleValue();
                String usdValueId = encoder.encodeToString(
                        ("Amount:" + usdValue + "_" + Currency.USD).getBytes());

                // TODO: 直接查询数据库，恐怕会有问题
                Token token0 = tokenService.token(chain, mint.getToken0().getId());
                Token token1 = tokenService.token(chain, mint.getToken1().getId());

                PoolTransaction transaction = new PoolTransaction();
                transaction.setId(transactionId);
                transaction.setChain(chain);
                transaction.setProtocolVersion(ProtocolVersion.V3);
                transaction.setType(PoolTransactionType.ADD);
                transaction.setHash(mint.getOrigin());
                transaction.setTimestamp(item.getTimestamp().intValue());
                transaction.setUsdValue(new Amount() {
                    {
                        setId(usdValueId);
                        setValue(usdValue);
                        setCurrency(Currency.USD);
                    }
                });
                transaction.setAccount(mint.getOrigin());
                transaction.setToken0(new Token() {
                    {
                        setId(mint.getToken0().getId());
                        setChain(chain);
                        setAddress(mint.getToken0().getId());
                        setStandard(token0.getStandard());
                        setDecimals(mint.getToken0().getDecimals().intValue());
                        setName(mint.getToken0().getName());
                        setSymbol(mint.getToken0().getSymbol());
                    }
                });
                transaction.setToken0Quantity(mint.getAmount0().toString());
                transaction.setToken1(new Token() {
                    {
                        setId(mint.getToken1().getId());
                        setChain(chain);
                        setAddress(mint.getToken0().getId());
                        setStandard(token1.getStandard());
                        setDecimals(mint.getToken1().getDecimals().intValue());
                        setName(mint.getToken1().getName());
                        setSymbol(mint.getToken1().getSymbol());
                    }
                });
                transaction.setToken1Quantity(mint.getAmount1().toString());

                return transaction;
            });
        }).toList();
    }

    public List<PoolTransaction> burnsTransactions(Integer skip, Integer first, Integer cursor, Chain chain) {
        TransactionsGraphQLQuery.Builder builder = TransactionsGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .queryName("TransactionsSubgraphFetcher_burnsTransactions");

        if (cursor != null) {
            builder.where(new Transaction_filter() {
                {
                    setTimestamp_gte(BigInteger.valueOf(cursor));
                }
            });
        }

        GraphQLQueryRequest request = new GraphQLQueryRequest(
                builder.build(),
                new TransactionProjectionRoot<>()
                        .id()
                        .timestamp()
                        .burns(0, 1000, Burn_orderBy.timestamp, OrderDirection.desc, null)
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
                                .parent()
                        .root());

        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        List<Transaction> list = response.extractValueAsObject("transactions", new TypeRef<List<Transaction>>() {});
        Encoder encoder = Base64.getEncoder();

        return list.stream().flatMap(item -> {
            return item.getBurns().stream().map(burn -> {
                String transactionId = encoder.encodeToString(
                        ("PoolTransaction:" + chain + "_" + item.getId() + "_" + PoolTransactionType.REMOVE + "_"
                                + burn.getToken0().getId() + "_" + burn.getToken1().getId()).getBytes());

                double usdValue = burn.getAmountUSD().doubleValue();
                String usdValueId = encoder.encodeToString(
                        ("Amount:" + usdValue + "_" + Currency.USD).getBytes());

                // TODO: 直接查询数据库，恐怕会有问题
                Token token0 = tokenService.token(chain, burn.getToken0().getId());
                Token token1 = tokenService.token(chain, burn.getToken1().getId());

                PoolTransaction transaction = new PoolTransaction();
                transaction.setId(transactionId);
                transaction.setChain(chain);
                transaction.setProtocolVersion(ProtocolVersion.V3);
                transaction.setType(PoolTransactionType.REMOVE);
                transaction.setHash(burn.getOrigin());
                transaction.setTimestamp(item.getTimestamp().intValue());
                transaction.setUsdValue(new Amount() {
                    {
                        setId(usdValueId);
                        setValue(usdValue);
                        setCurrency(Currency.USD);
                    }
                });
                transaction.setAccount(burn.getOrigin());
                transaction.setToken0(new Token() {
                    {
                        setId(burn.getToken0().getId());
                        setChain(chain);
                        setAddress(burn.getToken0().getId());
                        setStandard(token0.getStandard());
                        setDecimals(burn.getToken0().getDecimals().intValue());
                        setName(burn.getToken0().getName());
                        setSymbol(burn.getToken0().getSymbol());
                    }
                });
                transaction.setToken0Quantity(burn.getAmount0().toString());
                transaction.setToken1(new Token() {
                    {
                        setId(burn.getToken1().getId());
                        setChain(chain);
                        setAddress(burn.getToken0().getId());
                        setStandard(token1.getStandard());
                        setDecimals(burn.getToken1().getDecimals().intValue());
                        setName(burn.getToken1().getName());
                        setSymbol(burn.getToken1().getSymbol());
                    }
                });
                transaction.setToken1Quantity(burn.getAmount1().toString());

                return transaction;
            });
        }).toList();
    }

    public List<PoolTransaction> swapsTransactions(Integer skip, Integer first, Integer cursor, Chain chain) {
        TransactionsGraphQLQuery.Builder builder = TransactionsGraphQLQuery.newRequest()
                .skip(skip)
                .first(first)
                .queryName("TransactionsSubgraphFetcher_swapsTransactions");

        if (cursor != null) {
            builder.where(new Transaction_filter() {
                {
                    setTimestamp_gte(BigInteger.valueOf(cursor));
                }
            });
        }

        GraphQLQueryRequest request = new GraphQLQueryRequest(
                builder.build(),
                new TransactionProjectionRoot<>()
                        .id()
                        .timestamp()
                        .swaps(0, 1000, Swap_orderBy.timestamp, OrderDirection.desc, null)
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
                                .parent()
                        .root());

        GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

        List<Transaction> list = response.extractValueAsObject("transactions", new TypeRef<List<Transaction>>() {});
        Encoder encoder = Base64.getEncoder();

        return list.stream().flatMap(item -> {
            return item.getSwaps().stream().map(swap -> {
                String transactionId = encoder.encodeToString(
                        ("PoolTransaction:" + chain + "_" + item.getId() + "_" + PoolTransactionType.SWAP + "_"
                                + swap.getToken0().getId() + "_" + swap.getToken1().getId()).getBytes());

                double usdValue = swap.getAmountUSD().doubleValue();
                String usdValueId = encoder.encodeToString(
                        ("Amount:" + usdValue + "_" + Currency.USD).getBytes());

                // TODO: 直接查询数据库，恐怕会有问题
                Token token0 = tokenService.token(chain, swap.getToken0().getId());
                Token token1 = tokenService.token(chain, swap.getToken1().getId());

                PoolTransaction transaction = new PoolTransaction();
                transaction.setId(transactionId);
                transaction.setChain(chain);
                transaction.setProtocolVersion(ProtocolVersion.V3);
                transaction.setType(PoolTransactionType.SWAP);
                transaction.setHash(swap.getOrigin());
                transaction.setTimestamp(item.getTimestamp().intValue());
                transaction.setUsdValue(new Amount() {
                    {
                        setId(usdValueId);
                        setValue(usdValue);
                        setCurrency(Currency.USD);
                    }
                });
                transaction.setAccount(swap.getOrigin());
                transaction.setToken0(new Token() {
                    {
                        setId(swap.getToken0().getId());
                        setChain(chain);
                        setAddress(swap.getToken0().getId());
                        setStandard(token0.getStandard());
                        setDecimals(swap.getToken0().getDecimals().intValue());
                        setName(swap.getToken0().getName());
                        setSymbol(swap.getToken0().getSymbol());
                    }
                });
                transaction.setToken0Quantity(swap.getAmount0().toString());
                transaction.setToken1(new Token() {
                    {
                        setId(swap.getToken1().getId());
                        setChain(chain);
                        setAddress(swap.getToken0().getId());
                        setStandard(token1.getStandard());
                        setDecimals(swap.getToken1().getDecimals().intValue());
                        setName(swap.getToken1().getName());
                        setSymbol(swap.getToken1().getSymbol());
                    }
                });
                transaction.setToken1Quantity(swap.getAmount1().toString());
                
                return transaction;
            });
        }).toList();
    }
}
