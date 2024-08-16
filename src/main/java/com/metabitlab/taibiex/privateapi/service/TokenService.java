package com.metabitlab.taibiex.privateapi.service;

import com.metabitlab.taibiex.privateapi.entity.TokenProjectEntity;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.mapper.CGlibMapper;
import com.metabitlab.taibiex.privateapi.repository.TokenProjectRepository;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_orderBy;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TokenService {

  private final TokenSubgraphFetcher tokenSubgraphFetcher;
  private final TokenProjectRepository tokenProjectRepository;

    public TokenService(TokenSubgraphFetcher tokenSubgraphFetcher,
                        TokenProjectRepository tokenProjectRepository) {
        this.tokenSubgraphFetcher = tokenSubgraphFetcher;
        this.tokenProjectRepository = tokenProjectRepository;
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
}
