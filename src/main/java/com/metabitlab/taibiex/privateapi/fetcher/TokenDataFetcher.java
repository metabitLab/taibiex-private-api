package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenSortableField;
import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PriceSource;
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
  public TokenDataFetcher(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Autowired
  BundleSubgraphFetcher bundleSubgraphFetcher;

  @Autowired
  TokenSubgraphFetcher tokenSubgraphFetcher;

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

  @DgsData(parentType = "Token", field = "market")
  public TokenMarket market(@InputArgument Currency currency, DgsDataFetchingEnvironment env) {
    if (currency != Currency.USD) {
      return null;
    }

    com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t 
      = env.getLocalContext();

    Bundle bundle = bundleSubgraphFetcher.bundle();
    double price = bundle.getEthPriceUSD().multiply(t.getDerivedETH()).doubleValue();

    return new TokenMarket() {
      {
        setId("uuid");
        setPrice(new Amount() {
          {
            setId("uuid");
            setCurrency(Currency.USD);
            setValue(price);
          }
        });
        // 仅支持 Subgraph V3
        setPriceSource(PriceSource.SUBGRAPH_V3);
        // TODO: 仍有剩余字段未填值
      }
    };
  }

  @DgsData(parentType = "Token", field = "project")
  public TokenProject project(DgsDataFetchingEnvironment env) {
    com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t 
      = env.getLocalContext();

    return new TokenProject() {
      {
        setId("uuid");
        // TODO: 仍有剩余字段未填值
      }
    };
  }

  @DgsData(parentType = "TokenProject", field = "markets")
  public List<TokenProjectMarket> markets(@InputArgument List<Currency> currencies, 
                                          DgsDataFetchingEnvironment env) {
    Token t = env.getLocalContext();

    System.out.println(t);
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

  @DgsQuery
  public List<Token> topTokens(@InputArgument Chain chain,
                               @InputArgument Integer page,
                               @InputArgument Integer pageSize,
                               @InputArgument TokenSortableField orderBy) {
      return tokenService.topTokens(chain, page, pageSize, orderBy);
  }
}
