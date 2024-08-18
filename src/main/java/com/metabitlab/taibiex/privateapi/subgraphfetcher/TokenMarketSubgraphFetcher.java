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

  public List<TokenMinuteData> ohlcByTokenId(String tokenId) {
    TokenMinuteDatasGraphQLQuery tokenMinuteDatasQuery = TokenMinuteDatasGraphQLQuery.newRequest()
      .first(1)
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
      .first(144 * 10)
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
