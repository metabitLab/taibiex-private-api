package com.metabitlab.taibiex.privateapi.subgraphfetcher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.TypeRef;
import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenDayDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenDayDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenHourDataGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenHourDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenHourDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenMinuteDataGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenMinuteDatasGraphQLQuery;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.client.TokenMinuteDatasProjectionRoot;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenHourData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenHourData_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenHourData_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenMinuteData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenMinuteData_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenMinuteData_orderBy;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_filter;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

@Service
public class TokenMarketSubgraphFetcher {
  @Autowired
  SubgraphsClient subgraphsClient;

  /**
   * Ohlc
   * Day: 每 1 分钟一个点，共 1440 个点
   * 
   * @param tokenId
   * @return
   */
  public List<TokenMinuteData> dayOhlcByTokenId(String tokenId) {
    TokenMinuteDatasGraphQLQuery tokenMinuteDatasQuery = TokenMinuteDatasGraphQLQuery.newRequest()
      // the subgraphs only allow client to query 1000 records at most
      // but actually we need 1440 records
      .first(1000)
      .orderBy(TokenMinuteData_orderBy.periodStartUnix)
      .orderDirection(OrderDirection.desc)
      .where(new TokenMinuteData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_ohlcByTokenId")
      .build();

    TokenMinuteDatasProjectionRoot<?, ?> tokenMinuteDatasProjection = new TokenMinuteDatasProjectionRoot<>()
      .id()
      .periodStartUnix()
      .high()
      .open()
      .close()
      .low();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenMinuteDatasQuery, tokenMinuteDatasProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenMinuteData> origin = response.extractValueAsObject("tokenMinuteDatas", new TypeRef<List<TokenMinuteData>>() {});

    return origin;
  }

  /**
   * Ohlc
   * Hour: 每 1 分钟一个点，共 60 个点
   * 
   * @param tokenId
   * @return
   */
  public List<TokenMinuteData> hourOhlcByTokenId(String tokenId) {
    TokenMinuteDatasGraphQLQuery tokenMinuteDatasQuery = TokenMinuteDatasGraphQLQuery.newRequest()
      .first(60)
      .orderBy(TokenMinuteData_orderBy.periodStartUnix)
      .orderDirection(OrderDirection.desc)
      .where(new TokenMinuteData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_hourOhlcByTokenId")
      .build();

    TokenMinuteDatasProjectionRoot<?, ?> tokenMinuteDatasProjection = new TokenMinuteDatasProjectionRoot<>()
      .id()
      .periodStartUnix()
      .high()
      .open()
      .close()
      .low();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenMinuteDatasQuery, tokenMinuteDatasProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenMinuteData> origin = response.extractValueAsObject("tokenMinuteDatas", new TypeRef<List<TokenMinuteData>>() {});

    return origin;
  }

  /**
   * Ohlc
   * Week: 每 1 小时一个点，共 168 个点
   * 
   * @param tokenId
   * @return
   */
  public List<TokenHourData> weekOhlcByTokenId(String tokenId) {
    TokenHourDatasGraphQLQuery tokenHourDataQuery = TokenHourDatasGraphQLQuery.newRequest()
      .first(168)
      .orderBy(TokenHourData_orderBy.periodStartUnix)
      .orderDirection(OrderDirection.desc)
      .where(new TokenHourData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_weekOhlcByTokenId")
      .build();

    TokenHourDatasProjectionRoot<?, ?> tokenHourDataProjection = new TokenHourDatasProjectionRoot<>()
      .id()
      .periodStartUnix()
      .high()
      .open()
      .close()
      .low();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenHourDataQuery, tokenHourDataProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenHourData> origin = response.extractValueAsObject("tokenHourDatas", new TypeRef<List<TokenHourData>>() {});

    return origin;
  }
  
  /**
   * Ohlc
   * Month: 每 4 小时一个点，共 180 个点
   * 
   * @param tokenId
   * @return
   */
  public List<TokenHourData> monthOhlcByTokenId(String tokenId) {
    TokenHourDatasGraphQLQuery tokenHourDataQuery = TokenHourDatasGraphQLQuery.newRequest()
      .first(180 * 4)
      .orderBy(TokenHourData_orderBy.periodStartUnix)
      .orderDirection(OrderDirection.desc)
      .where(new TokenHourData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_monthOhlcByTokenId")
      .build();

    TokenHourDatasProjectionRoot<?, ?> tokenHourDataProjection = new TokenHourDatasProjectionRoot<>()
      .id()
      .periodStartUnix()
      .high()
      .open()
      .close()
      .low();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenHourDataQuery, tokenHourDataProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenHourData> origin = response.extractValueAsObject("tokenHourDatas", new TypeRef<List<TokenHourData>>() {});

    return origin;
  }

  /**
   * Ohlc
   * Year: 每 1 天一个点，共 365 个点
   * 
   * @param tokenId
   * @return
   */
  public List<TokenDayData> yearOhlcByTokenId(String tokenId) {
    TokenDayDatasGraphQLQuery tokenDayDatasQuery = TokenDayDatasGraphQLQuery.newRequest()
      .first(365)
      .orderBy(TokenDayData_orderBy.date)
      .orderDirection(OrderDirection.desc)
      .where(new TokenDayData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_yearOhlcByTokenId")
      .build();

    TokenDayDatasProjectionRoot<?, ?> tokenDayDatasProjection = new TokenDayDatasProjectionRoot<>()
      .id()
      .date()
      .high()
      .open()
      .close()
      .low();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenDayDatasQuery, tokenDayDatasProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenDayData> origin = response.extractValueAsObject("tokenDayDatas", new TypeRef<List<TokenDayData>>() {});

    return origin;
  }

  /**
   * PriceHistory
   * Day: 每 10 分钟一个点，共 144 个点
   * 
   * @param tokenId
   * @return List<TokenMinuteData>
   */
  public List<TokenMinuteData> dayPriceHistoryByTokenId(String tokenId) {
    /*
     * 1. 查询 TokenMinuteDatas，取最近的 1440 条数据
     * 2. 每 10 条数据取一条
     * 3. 为了提高精度，也可以每 10 条数据汇总一次，取平均值 (暂未实现)
     */
    TokenMinuteDatasGraphQLQuery tokenMinuteDatasQuery = TokenMinuteDatasGraphQLQuery.newRequest()
      // the subgraphs only allow client to query 1000 records at most
      // but actually we need 1440 records
      .first(1000)
      .orderBy(TokenMinuteData_orderBy.periodStartUnix)
      .orderDirection(OrderDirection.desc)
      .where(new TokenMinuteData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_dayPriceHistoryByTokenId")
      .build();

    TokenMinuteDatasProjectionRoot<?, ?> tokenMinuteDatasProjection = new TokenMinuteDatasProjectionRoot<>()
      .id()
      .periodStartUnix()
      .priceUSD();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenMinuteDatasQuery, tokenMinuteDatasProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenMinuteData> origin = response.extractValueAsObject("tokenMinuteDatas", new TypeRef<List<TokenMinuteData>>() {});
    if (origin == null) {
      return null;
    }

    List<TokenMinuteData> dist = IntStream.range(0, origin.size())
             .filter(index -> index % 10 == 0)
             .mapToObj(index -> origin.get(index))
             .toList();

    return dist;
  }

  /**
   * PriceHistory
   * Hour: 每 5 分钟一个点，共 12 个点
   * 
   * @param tokenId
   * @return List<TokenMinuteData>
   */
  public List<TokenMinuteData> hourPriceHistoryByTokenId(String tokenId) {
    TokenMinuteDatasGraphQLQuery tokenMinuteDatasQuery = TokenMinuteDatasGraphQLQuery.newRequest()
      .first(12 * 5)
      .orderBy(TokenMinuteData_orderBy.periodStartUnix)
      .orderDirection(OrderDirection.desc)
      .where(new TokenMinuteData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_hourPriceHistoryByTokenId")
      .build();

    TokenMinuteDatasProjectionRoot<?, ?> tokenMinuteDatasProjection = new TokenMinuteDatasProjectionRoot<>()
      .id()
      .periodStartUnix()
      .priceUSD();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenMinuteDatasQuery, tokenMinuteDatasProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenMinuteData> origin = response.extractValueAsObject("tokenMinuteDatas", new TypeRef<List<TokenMinuteData>>() {});
    if (origin == null) {
      return null;
    }

    List<TokenMinuteData> dist = IntStream.range(0, origin.size())
             .filter(index -> index % 5 == 0)
             .mapToObj(index -> origin.get(index))
             .toList();

    return dist;
  }

  /**
   * PriceHistory
   * Week: 每 1 小时一个点，共 168 个点
   * 
   * @param tokenId
   * @return List<TokenHourData>
   */
  public List<TokenHourData> weekPriceHistoryByTokenId(String tokenId) {
    TokenHourDatasGraphQLQuery tokenHourDataQuery = TokenHourDatasGraphQLQuery.newRequest()
      .first(168)
      .orderBy(TokenHourData_orderBy.periodStartUnix)
      .orderDirection(OrderDirection.desc)
      .where(new TokenHourData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_weekPriceHistoryByTokenId")
      .build();

    TokenHourDatasProjectionRoot<?, ?> tokenHourDataProjection = new TokenHourDatasProjectionRoot<>()
      .id()
      .periodStartUnix()
      .priceUSD();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenHourDataQuery, tokenHourDataProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenHourData> origin = response.extractValueAsObject("tokenHourDatas", new TypeRef<List<TokenHourData>>() {});

    return origin;
  }

  /**
   * PriceHistory
   * Month: 每 4 小时一个点，共 180 个点
   * 
   * @param tokenId
   * @return List<TokenHourData>
   */
  public List<TokenHourData> monthPriceHistoryByTokenId(String tokenId) {
    TokenHourDatasGraphQLQuery tokenHourDataQuery = TokenHourDatasGraphQLQuery.newRequest()
      .first(180 * 4)
      .orderBy(TokenHourData_orderBy.periodStartUnix)
      .orderDirection(OrderDirection.desc)
      .where(new TokenHourData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_monthPriceHistoryByTokenId")
      .build();

    TokenHourDatasProjectionRoot<?, ?> tokenHourDataProjection = new TokenHourDatasProjectionRoot<>()
      .id()
      .periodStartUnix()
      .priceUSD();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenHourDataQuery, tokenHourDataProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenHourData> origin = response.extractValueAsObject("tokenHourDatas", new TypeRef<List<TokenHourData>>() {});
    if (origin == null) {
      return null;
    }

    List<TokenHourData> dist = IntStream.range(0, origin.size())
             .filter(index -> index % 4 == 0)
             .mapToObj(index -> origin.get(index))
             .toList();

    return dist;
  }

  /**
   * PriceHistory
   * Year: 每 1 天一个点，共 365 个点
   * 
   * @param tokenId
   * @return List<TokenDayData>
   */
  public List<TokenDayData> yearPriceHistoryByTokenId(String tokenId) {
    TokenDayDatasGraphQLQuery tokenDayDatasQuery = TokenDayDatasGraphQLQuery.newRequest()
      .first(365)
      .orderBy(TokenDayData_orderBy.date)
      .orderDirection(OrderDirection.desc)
      .where(new TokenDayData_filter() {
        {
          setToken_(new Token_filter() {
            {
              setId(tokenId);
            }
          });
        }
      })
      .queryName("TokenMarketSubgraphFetcher_yearPriceHistoryByTokenId")
      .build();

    TokenDayDatasProjectionRoot<?, ?> tokenDayDatasProjection = new TokenDayDatasProjectionRoot<>()
      .id()
      .date()
      .priceUSD();

    GraphQLQueryRequest request = new GraphQLQueryRequest(tokenDayDatasQuery, tokenDayDatasProjection);
    GraphQLResponse response = subgraphsClient.build().executeQuery(request.serialize());

    List<TokenDayData> origin = response.extractValueAsObject("tokenDayDatas", new TypeRef<List<TokenDayData>>() {});

    return origin;
  }
}
