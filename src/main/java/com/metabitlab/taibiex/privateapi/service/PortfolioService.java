package com.metabitlab.taibiex.privateapi.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSONArray;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Amount;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Portfolio;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.PortfolioValueModifier;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenBalance;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.BundleSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.util.Constants;

import io.vavr.Tuple2;

/**
 * This class represents the portfolio service.
 * 
 * @author: nix
 */
@Service
public class PortfolioService {
    @Autowired
    BundleSubgraphFetcher bundleSubgraphFetcher;

    @Autowired
    TokenService tokenService;

    @Value("${tabiscan.api-domain.url}")
    private String tabiscanApiDomain;

    public List<Portfolio> portfolios(
            List<String> ownerAddresses,
            List<Chain> chains,
            List<PortfolioValueModifier> valueModifiers) {
        Encoder encoder = Base64.getEncoder();

        return ownerAddresses.stream().map(ownerAddress -> {
            String portfolioId = encoder.encodeToString(
                    ("Portfolio:" + ownerAddress).getBytes());

            Portfolio portfolio = new Portfolio();
            portfolio.setId(portfolioId);
            portfolio.setOwnerAddress(ownerAddress);

            return portfolio;
        }).toList();
    }

    public List<TokenBalance> tokenBalances(String ownerAddress, Chain chain) throws IOException {
        // NOTE: 返回 TokenBalance 的五个字段
        // - id
        // - quantity 持有的代表数量
        // - denominatedValue 代表的 USD 价值
        // - token 代币详情
        // - tokenProjectMarket
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(String.format(
                "%s/api/v2/addresses/%s/token-balances",
                tabiscanApiDomain,
                ownerAddress));

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != Constants.HTTP_SUCCESS_CODE) {
                throw new RuntimeException("Failed to fetch token balances");
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new RuntimeException("Failed to parse the token balances response");
            }

            BigDecimal ethPriceUSD = bundleSubgraphFetcher.bundle().getEthPriceUSD();
            if (ethPriceUSD == null) {
                throw new RuntimeException("Failed to fetch ETH price in USD");
            }

            String str = EntityUtils.toString(entity);
            List<PortfolioService.Balance> balances = JSONArray.parseArray(str, PortfolioService.Balance.class);

            Encoder encoder = Base64.getEncoder();

            return balances.stream()
                    // NOTE: 过滤掉没有 decimals 的代币，比如 NFT
                    .filter(item -> item.token.decimals != null)
                    .map(item -> {

                        Double quantify = Double.parseDouble(item.value)
                                / Math.pow(10, Double.parseDouble(item.token.decimals));

                        Tuple2<
                            com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token, 
                            com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token
                        > tuple = tokenService
                                .getTokenFromSubgraphs(chain, item.token.address);

                        BigDecimal derivedETH = tuple._2.getDerivedETH();
                        double denominatedValue = derivedETH.multiply(ethPriceUSD).multiply(BigDecimal.valueOf(quantify)).doubleValue();

                        String tokenId = encoder.encodeToString(
                                ("Token:" + chain + "_" + item.token.address).getBytes());
                        String tokenBalanceId = encoder.encodeToString(
                                ("TokenBalance:" + ownerAddress + "_" + tokenId + "_" + Currency.USD).getBytes());
                        String amountId = encoder.encodeToString(
                                ("Amount:" + denominatedValue + "_" + Currency.USD).getBytes());

                        TokenBalance tokenBalance = new TokenBalance();
                        tokenBalance.setId(tokenBalanceId);
                        tokenBalance.setQuantity(quantify);
                        tokenBalance.setToken(tuple._1);
                        tokenBalance.setDenominatedValue(new Amount() {
                            {
                                setId(amountId);
                                setCurrency(Currency.USD);
                                setValue(denominatedValue);
                            }
                        });

                        // NOTE: 以下字段由其他 dgsDataFetcher 填充
                        // TokenProjectMarket

                        return tokenBalance;
                    }).toList();
        } finally {
            if (response != null) {
                response.close();
            }

            if (request != null) {
                request.releaseConnection();
            }

            httpClient.close();
        }
    }

    private class Balance {
        public String value;
        public Token token;
    }

    private class Token {
        public String address;
        public String decimals;
        // 可以获得，但是暂时不需要
        // public String name;
        // public String symbol;
        // public String type; // ERC-20
    }
}
