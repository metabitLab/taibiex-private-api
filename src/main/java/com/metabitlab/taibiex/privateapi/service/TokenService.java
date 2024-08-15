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

    public TokenService(TokenSubgraphFetcher tokenSubgraphFetcher) {
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
            tokenList.add(token);
        }

        tokenList.forEach(token -> {
            token.setChain(Chain.TABI);
            token.setId(Base64.getEncoder().encodeToString(("Token:TABI_" + token.getId()).getBytes()));
        });

        return tokenList;

    }
}
