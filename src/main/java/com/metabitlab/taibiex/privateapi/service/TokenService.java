package com.metabitlab.taibiex.privateapi.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.mapper.CGlibMapper;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token_orderBy;

import org.springframework.beans.factory.annotation.Value;

import com.metabitlab.taibiex.privateapi.util.RedisService;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Base64.Encoder;
import java.util.concurrent.TimeUnit;

import static com.metabitlab.taibiex.privateapi.util.Constants.TABI;
import static com.metabitlab.taibiex.privateapi.util.Constants.NATIVE_TOKEN_NAME;

@Log4j2
@Service
public class TokenService {

    private final TokenSubgraphFetcher tokenSubgraphFetcher;

    private final RedisService redisService;

    @Value("${app.wtabi}")
    private String wtabi;

    public TokenService(TokenSubgraphFetcher tokenSubgraphFetcher, RedisService redisService) {
        this.tokenSubgraphFetcher = tokenSubgraphFetcher;
        this.redisService = redisService;
    }

    public List<Token> topTokens(Chain chain, Integer page, Integer pageSize, TokenSortableField orderBy) {

        String cacheKey = "topTokens_" + chain + "_" + page + "_" + pageSize + "_" + orderBy;

        try {
            Object topTokens = redisService.get(cacheKey);
            if (null != topTokens) {
                return JSONObject.parseArray(topTokens.toString(), Token.class);
            }
        } catch (Exception e) {
            log.error("topTokens redis read error：{}", e.getMessage());
        }

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
            token.setStandard(subGraphToken.getSymbol().equalsIgnoreCase(NATIVE_TOKEN_NAME)? TokenStandard.NATIVE:TokenStandard.ERC20);
            token.setChain(TABI);
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
        try {
            String pvoStr = JSON.toJSONString(tokenList, SerializerFeature.WriteNullStringAsEmpty);
            redisService.set(cacheKey, pvoStr, 1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("topTokens redis write error：{}", e.getMessage());
        }
        return tokenList;
    }

    public List<Token> tokens(List<ContractInput> contractInputs) {

        List<Token> tokenList = new ArrayList<>();

        for (ContractInput contractInput : contractInputs) {
            tokenList.add(token(contractInput.getChain(), contractInput.getAddress()));
        }

        return tokenList;
    }

    public List<Token> tokens(String searchQuery, List<Chain> chains) {
        List<com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token> tokens = tokenSubgraphFetcher
                .tokens(null, null, null, null, new Token_filter() {
                    {
                        setOr(List.of(
                            new Token_filter() {
                                {
                                    setName_contains_nocase(searchQuery);
                                }
                            },
                            new Token_filter() {
                                {
                                    setId(searchQuery);
                                }
                            }
                        ));
                    }
                });

        Encoder encoder = Base64.getEncoder();

        List<Token> list = tokens.stream()
                .map(item -> {
                    String tokenId = encoder.encodeToString(
                            ("Token:" + TABI + "_" + item.getId()).getBytes());

                    Token token = CGlibMapper.mapper(item, Token.class);
                    token.setAddress(item.getId());
                    token.setStandard(item.getSymbol().equalsIgnoreCase(NATIVE_TOKEN_NAME) ? TokenStandard.NATIVE
                            : TokenStandard.ERC20);
                    token.setChain(TABI);
                    token.setId(tokenId);

                    return token;
                })
                .toList();

        return list;
    }

    public Token token(Chain chain, String address) {
        String cacheKey = "token:" + chain + address;
        try {
            Object token = redisService.get(cacheKey);
            if (null != token) {
                return JSONObject.parseObject(token.toString(), Token.class);
            }
        } catch (Exception e) {
            log.error(e.fillInStackTrace());
        }
        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token subGraphToken = tokenSubgraphFetcher
                .token(address);
        if (subGraphToken == null) {
            return null;
        }
        Token token = CGlibMapper.mapper(subGraphToken, Token.class);
        token.setAddress(subGraphToken.getId());
        token.setStandard(subGraphToken.getSymbol().equalsIgnoreCase(NATIVE_TOKEN_NAME)? TokenStandard.NATIVE:TokenStandard.ERC20);
        token.setChain(TABI);
        token.setId(Base64.getEncoder().encodeToString(("Token:TABI_" + subGraphToken.getId()).getBytes()));

        try {
            String pvoStr = JSON.toJSONString(token, SerializerFeature.WriteNullStringAsEmpty);
            redisService.set(cacheKey, pvoStr, 2, TimeUnit.MINUTES);
        } catch (Exception e){
            log.error("tokens redis write error：{}", e.fillInStackTrace());
        }
        return token;
    }

    public Tuple2<com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token> getTokenFromSubgraphs(
                Chain chain, String address) {
        final com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token t;

        if (address == null || address.isEmpty()) {
            // NOTE: 当 Token 的地址为 null 时, 如何获取主网币的信息
            t = tokenSubgraphFetcher.token(wtabi.toLowerCase());
        } else {
            t = tokenSubgraphFetcher.token(address.toLowerCase());
        }

        if (t == null) {
            throw new RuntimeException(String.format("Token (%s %s) is not found", address, chain));
        }

        Encoder encoder = Base64.getEncoder();

        String tokenId = encoder.encodeToString(
                ("Token:" + chain + "_" + address).getBytes());

        Token token = new Token() {
            {
                setId(tokenId);
                setChain(chain);
                setAddress(address);
                setStandard(t.getSymbol().equalsIgnoreCase(NATIVE_TOKEN_NAME)? TokenStandard.NATIVE:TokenStandard.ERC20);
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
