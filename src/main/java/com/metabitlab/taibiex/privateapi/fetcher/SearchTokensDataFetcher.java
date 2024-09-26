package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.metabitlab.taibiex.privateapi.util.RedisService;
import com.alibaba.fastjson2.JSON;
import com.metabitlab.taibiex.privateapi.errors.ParseCacheException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportChainException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import static com.metabitlab.taibiex.privateapi.util.Constants.TABI;

/**
 * Data fetcher for searching tokens.
 * 
 * @author nix
 */
@DgsComponent
public class SearchTokensDataFetcher {

    @Autowired
    TokenService tokenService;

    @Autowired
    RedisService redisService;

    /**
     * The time-to-live for the V3 transactions cache in seconds.
     */
    private final int SEARCH_QUERY_CACHE_TTL = 10;

	@DgsQuery
    public List<Token> searchTokens(
            @InputArgument String searchQuery,
            @InputArgument List<Chain> chains) {

        // NOTE: [已确认] 参数 chain 未使用, 仅支持 TABI
        if (chains != null && (chains.size() > 1 || chains.get(0) != TABI)) {
            throw new UnSupportChainException("Those chains are not supported", chains);
        }

        String cacheKey = "tokens_search_" + searchQuery;
        try {
            Object wrappedSearchResult = redisService.get(cacheKey);
            if (wrappedSearchResult != null) {
                return JSON.parseArray((String) wrappedSearchResult, Token.class);
            }

            List<Token> result = tokenService.tokens(searchQuery, chains);
            redisService.set(cacheKey, JSON.toJSONString(result), SEARCH_QUERY_CACHE_TTL, TimeUnit.SECONDS);

            return result;
        } catch (Exception e) {
            throw new ParseCacheException(String.format("Error occurs on search result cache with search query: {}", e.fillInStackTrace()),
            searchQuery);
        }
    }
}
