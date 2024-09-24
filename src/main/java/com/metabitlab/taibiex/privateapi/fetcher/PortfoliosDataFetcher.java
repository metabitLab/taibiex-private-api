package com.metabitlab.taibiex.privateapi.fetcher;

import java.io.IOException;
import java.util.List;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.springframework.beans.factory.annotation.Autowired;

import com.metabitlab.taibiex.privateapi.errors.MissSourceException;
import com.metabitlab.taibiex.privateapi.errors.UnSupportChainException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.AmountChange;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.HistoryDuration;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Portfolio;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PortfolioValueModifier;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenBalance;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProjectMarket;
import com.metabitlab.taibiex.privateapi.service.PortfolioService;
import com.metabitlab.taibiex.privateapi.service.TokenProjectMarketService;
import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import static com.metabitlab.taibiex.privateapi.util.Constants.TABI;

/**
 * This class is responsible for fetching portfolios data.
 * 
 * @author: nix
 */
@DgsComponent
public class PortfoliosDataFetcher {
    @Autowired
    PortfolioService portfolioService;

    @Autowired
    TokenProjectMarketService tokenProjectMarketService;

    @Autowired
    BundleSubgraphFetcher bundleSubgraphFetcher;

    @Autowired
    TokenService tokenService;

	@DgsQuery
    public List<Portfolio> portfolios(
        @InputArgument List<String> ownerAddresses,
        @InputArgument List<Chain> chains,
        @InputArgument List<PortfolioValueModifier> valueModifiers
    ) {
        // NOTE: [已确认] 参数 chain 未使用, 仅支持 TABI 
        if (chains.size() > 1 || chains.get(0) != TABI) {
            throw new UnSupportChainException("Those chains are not supported", chains);
        }

        List<Portfolio> portfolios = portfolioService.portfolios(ownerAddresses, chains, valueModifiers);
        return portfolios.stream().map(portfolio -> {
            if (chains.size() > 1 || chains.get(0) != TABI) {
                throw new UnSupportChainException("Those chains are not supported", chains);
            }
    
            String ownerAddress = portfolio.getOwnerAddress();
    
            try {
                List<TokenBalance> balances = portfolioService.tokenBalances(ownerAddress, chains.get(0));
                double tokensTotalDenominatedValue = balances.stream()
                    .map(balance -> balance.getDenominatedValue().getValue())
                    .reduce(0.0, (sum, value) -> sum + value);

                Encoder encoder = Base64.getEncoder();

                String tokensTotalDenominatedValueAmountId = encoder.encodeToString(
                    ("Amount:" + tokensTotalDenominatedValue + "_" + Currency.USD).getBytes()
                );

                portfolio.setTokenBalances(balances);
                portfolio.setTokensTotalDenominatedValue(new Amount() {
                    {
                        setId(tokensTotalDenominatedValueAmountId);
                        setValue(tokensTotalDenominatedValue);
                        setCurrency(Currency.USD);
                    }
                });
                // NOTE: TokensTotalDenominatedValueChange 这个怎么算？
                portfolio.setTokensTotalDenominatedValueChange(new AmountChange() {
                    {
                    }
                });
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return portfolio;
        }).toList();
    }

    @DgsData(parentType = DgsConstants.TOKENBALANCE.TYPE_NAME)
    public TokenProjectMarket tokenProjectMarket (
        DgsDataFetchingEnvironment env
    ) {
        TokenBalance tokenBalance = env.getSource();
        if (tokenBalance == null) {
            throw new MissSourceException("TokenBalance is required", "tokenBalance");
        }

        Token token = tokenBalance.getToken();
        if (token == null) {
            throw new MissSourceException("Token is required", "token");
        }

        TokenProjectMarket market = tokenProjectMarketService.getMarketFromToken(token);

        return market;
    }

    @DgsData(parentType = DgsConstants.TOKENPROJECTMARKET.TYPE_NAME)
    public Amount pricePercentChange(
        @InputArgument HistoryDuration duration,
        DgsDataFetchingEnvironment env
    ) {
        TokenProjectMarket tokenProjectMarket = env.getSource();
        if (tokenProjectMarket == null) {
            throw new MissSourceException("TokenProjectMarket is required", "tokenProjectMarket");
        }

        return tokenProjectMarketService.pricePercentChange(duration, tokenProjectMarket);
    }
}
