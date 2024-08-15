package com.metabitlab.taibiex.privateapi.fetcher;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenSortableField;
import com.metabitlab.taibiex.privateapi.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;

import com.metabitlab.taibiex.privateapi.configuration.SubgraphsClient;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import java.util.List;

@DgsComponent
public class TokenDataFetcher {

  private final SubgraphsClient subgraphsClient;

  private final TokenService tokenService;

    public TokenDataFetcher(SubgraphsClient subgraphsClient, TokenService tokenService) {
        this.subgraphsClient = subgraphsClient;
        this.tokenService = tokenService;
    }

    @DgsQuery
  public List<Token> topTokens(@InputArgument Chain chain,
                               @InputArgument Integer page,
                               @InputArgument Integer pageSize,
                               @InputArgument TokenSortableField orderBy) {
      return tokenService.topTokens(chain, page, pageSize, orderBy);
  }
}
