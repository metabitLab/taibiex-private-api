package com.metabitlab.taibiex.privateapi.service;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.metabitlab.taibiex.privateapi.errors.UnSupportDurationException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.HistoryDuration;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProject;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProjectMarket;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Bundle;

import io.vavr.Tuple2;

/**
 * This class represents the TokenProjectMarketService.
 * 
 * @author: nix
 */
@Service
public class TokenProjectMarketService {
    @Autowired
    TokenProjectService tokenProjectService;

    @Autowired
    TokenService tokenService;

    @Autowired
    BundleSubgraphFetcher bundleSubgraphFetcher;
    
    public TokenProjectMarket getMarketFromToken(Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }

        Tuple2<
        com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, 
        com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token
    > tuple = tokenService
            .getTokenFromSubgraphs(token.getChain(), token.getAddress());

        TokenProject project = tokenProjectService.findByAddress(token);

        Encoder encoder = Base64.getEncoder();

        Bundle bundle = bundleSubgraphFetcher.bundle();
        BigDecimal price = tuple._2.getDerivedETH().multiply(bundle.getEthPriceUSD());

        String projectId = encoder.encodeToString(
            ("TokenProject:" + token.getChain() + "_" + token.getAddress() + "_" + token.getName()).getBytes());
        String projectMarketId = encoder.encodeToString(
            ("TokenProjectMarket:" + projectId + "_" + Currency.USD).getBytes());
        String priceId = encoder.encodeToString(
            ("Amount:" + price.doubleValue() + "_" + Currency.USD).getBytes());

        TokenProjectMarket onlyOneMarket = new TokenProjectMarket() {
            {
                setId(projectMarketId);
                setTokenProject(project);
                setCurrency(Currency.USD);

                setPrice(new Amount() {
                    {
                        setId(priceId);
                        setCurrency(Currency.USD);
                        setValue(price.doubleValue());
                    }
                });
                // // TODO: 需要填值，参考文档
                // setPricePercentChange(null);
                // setPricePercentChange24h(null);
                // setPriceHigh52w(null);
                // setPriceLow52w(null);
                // setPriceHistory(null);
                // setPriceHighLow(null);
            }
        };

        return onlyOneMarket;
    }

    public Amount pricePercentChange(HistoryDuration duration, TokenProjectMarket market) {
        switch (duration) {
            case FIVE_MINUTE:
                return fiveMinutesPricePercentChange(market);
            case HOUR:
                return hourPricePercentChange(market);
            case DAY:
                return dayPricePercentChange(market);
            case WEEK:
                return weekPricePercentChange(market);
            case MONTH:
                return monthPricePercentChange(market);
            case YEAR:
                return yearPricePercentChange(market);
            default:
                throw new UnSupportDurationException("This duration is not supported", duration);
        }
    }

    private Amount fiveMinutesPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount hourPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount dayPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount weekPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount monthPricePercentChange(TokenProjectMarket market) {
        return null;
    }

    private Amount yearPricePercentChange(TokenProjectMarket market) {
        return null;
    }
}
