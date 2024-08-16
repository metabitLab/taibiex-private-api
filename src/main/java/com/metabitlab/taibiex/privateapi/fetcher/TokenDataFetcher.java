package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenSortableField;
import com.metabitlab.taibiex.privateapi.service.TokenService;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class TokenDataFetcher {

  private final TokenService tokenService;
  public TokenDataFetcher(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @DgsQuery
  public Token token(@InputArgument Chain chain, @InputArgument String address) {
    return tokenService.token(chain, address);
  }

  @DgsQuery
  public List<Token> topTokens(@InputArgument Chain chain,
                               @InputArgument Integer page,
                               @InputArgument Integer pageSize,
                               @InputArgument TokenSortableField orderBy) {
      return tokenService.topTokens(chain, page, pageSize, orderBy);
  }
}
