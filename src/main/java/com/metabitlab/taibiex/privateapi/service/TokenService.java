package com.metabitlab.taibiex.privateapi.service;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenSortableField;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenStandard;
import com.metabitlab.taibiex.privateapi.mapper.CGlibMapper;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_orderBy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class TokenService {

  private final TokenSubgraphFetcher tokenSubgraphFetcher;
  private final PoolsSubgraphFetcher poolsSubgraphFetcher;
    private final TokenProjectRepository tokenProjectRepository;

    private final TokenSubgraphFetcher tokenSubgraphFetcher;

  public TokenService(
    TokenSubgraphFetcher tokenSubgraphFetcher,
    PoolsSubgraphFetcher poolsSubgraphFetcher) {
    this.tokenSubgraphFetcher = tokenSubgraphFetcher;
    this.poolsSubgraphFetcher = poolsSubgraphFetcher;
  }
    public TokenService(TokenProjectRepository tokenProjectRepository, TokenSubgraphFetcher tokenSubgraphFetcher) {
        this.tokenProjectRepository = tokenProjectRepository;
        this.tokenSubgraphFetcher = tokenSubgraphFetcher;
    }

    public List<Token> topTokens(Chain chain, Integer page, Integer pageSize, TokenSortableField orderBy) {

        Token_orderBy tokenOrderBy = switch (orderBy) {
            case TOTAL_VALUE_LOCKED -> Token_orderBy.totalValueLocked;
            case POPULARITY -> Token_orderBy.txCount;
            default -> Token_orderBy.volume;
        };

        List<com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token> subGraphTokens =
                tokenSubgraphFetcher.tokens((page - 1) * pageSize, pageSize, tokenOrderBy, OrderDirection.desc, null);

        List<Token> tokenList = new ArrayList<>();
        for (com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token subGraphToken : subGraphTokens) {
            Token token = CGlibMapper.mapper(subGraphToken, Token.class);
            token.setAddress(subGraphToken.getId());
            token.setStandard(subGraphToken.getSymbol().equalsIgnoreCase("TABI")? TokenStandard.NATIVE:TokenStandard.ERC20);
            token.setChain(Chain.TABI);
            token.setId(Base64.getEncoder().encodeToString(("Token:TABI_" + subGraphToken.getId()).getBytes()));

            TokenProjectEntity projectEntity = tokenProjectRepository.findByAddress(subGraphToken.getId());
            if (projectEntity != null){
                TokenProject tokenProject = CGlibMapper.mapper(projectEntity, TokenProject.class);
                tokenProject.setId(Base64.getEncoder().encodeToString(("TokenProject:TABI_" + subGraphToken.getId() + "_" + subGraphToken.getSymbol()).getBytes()));
                Image logo = Image.newBuilder().url(projectEntity.getLogoUrl()).dimensions(Dimensions.newBuilder().width(projectEntity.getLogoWidth()).height(projectEntity.getLogoHeight()).build()).build();
                logo.setId(Base64.getEncoder().encodeToString(("Image:" + logo.getUrl()).getBytes()));
                tokenProject.setLogo(logo);
                token.setProject(tokenProject);
            }
            tokenList.add(token);
        }

        return tokenList;

  }

  public Token token(Chain chain, String address) {

    List<Pool> pools = poolsSubgraphFetcher.pools(0, 100);
    Optional<Pool> target = pools.stream()
      .filter(item -> "0x6cec4df7ad64d5d06860a397c17edebc5f311ae3".equals(item.getId()))
      .findFirst();

    double price = target.orElse(null).getToken0Price().doubleValue();

    return new Token() {
      {
        setId("uuid");
        setChain(chain);
        setAddress(address);
        setName("Ethereum");
        setSymbol("ETH");
        setProject(new TokenProject() {
          {
            setId("uuid");
            setMarkets(Arrays.asList(
              new TokenProjectMarket() {
                {
                  setId("uuid");
                  setPrice(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(price);
                    }
                  });
                }
              }
            ));
          }
        });
        setMarket(new TokenMarket() {
          {
            setId("uuid");
            setPrice(new Amount() {
              {
                setId("uuid");
                setCurrency(Currency.USD);
                setValue(price);
              }
            });
            setOhlc(Arrays.asList(
              new TimestampedOhlc() {
                {
                  setId("uuid");
                  setTimestamp((int) Instant.now().getEpochSecond());
                  setOpen(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(price);
                    }
                  });
                  setHigh(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(price);
                    }
                  });
                  setLow(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(price);
                    }
                  });
                  setClose(new Amount() {
                    {
                      setId("uuid");
                      setCurrency(Currency.USD);
                      setValue(price);
                    }
                  });
                }
              }
            ));
            setPriceHistory(Arrays.asList(
              new TimestampedAmount() {
                {
                  setId("uuid");
                  setCurrency(Currency.USD);
                  setValue(price);
                  setTimestamp((int) Instant.now().getEpochSecond());
                }
              }
            ));
          }
        });
      }
    };
  }
}
