package com.metabitlab.taibiex.privateapi.service;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.mapper.CGlibMapper;
import com.metabitlab.taibiex.privateapi.repository.TokenProjectRepository;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_orderBy;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.Base64.Encoder;

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

        if (null == orderBy){
            orderBy = TokenSortableField.VOLUME;
        }

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

            /*TokenProjectEntity projectEntity = tokenProjectRepository.findByAddress(subGraphToken.getId());
            if (projectEntity != null){
                TokenProject tokenProject = CGlibMapper.mapper(projectEntity, TokenProject.class);
                tokenProject.setId(Base64.getEncoder().encodeToString(("TokenProject:TABI_" + subGraphToken.getId() + "_" + subGraphToken.getSymbol()).getBytes()));
                Image logo = Image.newBuilder().url(projectEntity.getLogoUrl()).dimensions(Dimensions.newBuilder().width(projectEntity.getLogoWidth()).height(projectEntity.getLogoHeight()).build()).build();
                logo.setId(Base64.getEncoder().encodeToString(("Image:" + logo.getUrl()).getBytes()));
                tokenProject.setLogo(logo);
                token.setProject(tokenProject);
            }*/
            tokenList.add(token);
        }
        return tokenList;
    }

    public List<Token> tokens(List<ContractInput> contractInputs) {

        List<com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token> subGraphTokens =
                tokenSubgraphFetcher.tokens(null, null, null, null,
                        ObjectUtils.isEmpty(contractInputs)?null:
                        new Token_filter(){{
                            setId_in(contractInputs.stream().map(ContractInput::getAddress).map(String::toLowerCase).toList());
                        }});

        List<Token> tokenList = new ArrayList<>();
        for (com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token subGraphToken : subGraphTokens) {
            Token token = CGlibMapper.mapper(subGraphToken, Token.class);
            token.setAddress(subGraphToken.getId());
            token.setStandard(subGraphToken.getSymbol().equalsIgnoreCase("TABI")? TokenStandard.NATIVE:TokenStandard.ERC20);
            token.setChain(Chain.TABI);
            token.setId(Base64.getEncoder().encodeToString(("Token:TABI_" + subGraphToken.getId()).getBytes()));
            token.setDecimals(subGraphToken.getDecimals().intValue());
            tokenList.add(token);
        }
        return tokenList;
    }

    public Token token(Chain chain, String address) {
        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token subGraphToken = tokenSubgraphFetcher
                .token(address);
        if (subGraphToken == null) {
            return null;
        }
        Token token = CGlibMapper.mapper(subGraphToken, Token.class);
        token.setAddress(subGraphToken.getId());
        token.setStandard(subGraphToken.getSymbol().equalsIgnoreCase("TABI")? TokenStandard.NATIVE:TokenStandard.ERC20);
        token.setChain(Chain.TABI);
        token.setId(Base64.getEncoder().encodeToString(("Token:TABI_" + subGraphToken.getId()).getBytes()));
        return token;
    }

    public Tuple2<com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token> getTokenFromSubgraphs(
            Chain chain, String address) {
        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t = tokenSubgraphFetcher
                .token(address);

        Encoder encoder = Base64.getEncoder();

        String tokenId = encoder.encodeToString(
                ("Token:" + chain + "_" + address).getBytes());

        Token token = new Token() {
            {
                setId(tokenId);
                setChain(chain);
                setAddress(address);
                setStandard(TokenStandard.ERC20);
                setName(t.getName());
                setSymbol(t.getSymbol());
                setDecimals(t.getDecimals().intValue());
                // NOTE: [已确认] 从 Subgraphs 的 Token 中获取, 暂时可以不关心
                setFeeData(null);
                // NOTE: [已确认] 不支持 V2Transactions
                setV2Transactions(null);
            }
        };

        Tuple2<com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token> tuple = Tuple
                .of(token, t);

        return tuple;
    }
}
