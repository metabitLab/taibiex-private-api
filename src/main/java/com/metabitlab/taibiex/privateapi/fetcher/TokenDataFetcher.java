package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;

import com.metabitlab.taibiex.privateapi.service.TokenProjectService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenSortableField;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenStandard;
import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenMarketSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.TokenDayData;
import com.metabitlab.taibiex.privateapi.errors.UnSupportCurrencyException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
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

  public TokenDataFetcher(TokenService tokenService,
                          TokenProjectService tokenProjectService) {
    this.tokenService = tokenService;
    this.tokenProjectService = tokenProjectService;
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
        // TODO: 仍有剩余字段未填值
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

    com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t 
      = env.getLocalContext();
    com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token token 
      = env.getSource();

    Bundle bundle = bundleSubgraphFetcher.bundle();
    double price = bundle.getEthPriceUSD().multiply(t.getDerivedETH()).doubleValue();

    return new TokenMarket() {
      {
        setId("uuid");
        setToken(token);
        setPrice(new Amount() {
          {
            setId("uuid");
            setCurrency(Currency.USD);
            setValue(price);
          }
        });
        // 仅支持 Subgraph V3
        setPriceSource(PriceSource.SUBGRAPH_V3);
        setTotalValueLocked(new Amount() {
          {
            setId("uuid");
            setCurrency(Currency.USD);
            setValue(t.getTotalValueLocked().doubleValue());
          }
        });
        setFullyDilutedValuation(new Amount() {
          {
            // TODO: 需要填值
          }
        });
        setVolume(new Amount() {
          {
            setId("uuid");
            setCurrency(Currency.USD);
            setValue(t.getVolume().doubleValue());
          }
        });
        setPricePercentChange(new Amount() {
          {
            // TODO: 需要填值
          }
        });
        setPriceHighLow(new Amount() {
          {
            // TODO: 需要填值
          }
        });
      }
    };
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
                                    DgsDataFetchingEnvironment env) throws Exception {
    if (duration != HistoryDuration.DAY) {
      throw new Exception("This duration is not supported");
    }

    com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t 
      = env.getLocalContext();


    List<TokenDayData> tokenDayDataList = tokenMarketSubgraphFetcher.tokenDayDatasById(t.getId());
    if (tokenDayDataList == null) {
      return null;
    }

    return tokenDayDataList.stream()
                .map(item -> {
                  TimestampedOhlc ohlc = new TimestampedOhlc();
                  ohlc.setId(item.getId());
                  ohlc.setTimestamp(item.getDate());
                  ohlc.setClose(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(item.getClose().doubleValue());
                    }
                  });
                  ohlc.setHigh(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(item.getHigh().doubleValue());
                    }
                  });
                  ohlc.setLow(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(item.getLow().doubleValue());
                    }
                  });
                  ohlc.setOpen(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(item.getOpen().doubleValue());
                    }
                  });
                  return ohlc;
                })
                .toList();
  }

  @DgsData(parentType = DgsConstants.TOKENMARKET.TYPE_NAME, field = DgsConstants.TOKENMARKET.PriceHistory)
  public List<TimestampedAmount> priceHistory(@InputArgument HistoryDuration duration) {
    System.out.println(duration);

    return Arrays.asList();
  }

  @DgsData(parentType = "Query", field = "topTokens")
  public List<Token> topTokens(@InputArgument Chain chain,
                               @InputArgument Integer page,
                               @InputArgument Integer pageSize,
                               @InputArgument TokenSortableField orderBy) {
    return tokenService.topTokens(chain, page, pageSize, orderBy);
  }

}
