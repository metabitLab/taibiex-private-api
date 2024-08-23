package com.metabitlab.taibiex.privateapi.service;

import java.util.List;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TimestampedAmount;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.UniswapDayDataSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData_filter;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData_orderBy;

@Service
public class UniswapDayDataService {
    @Autowired
    UniswapDayDataSubgraphFetcher uniswapDayDataSubgraphFetcher;
    
    public List<TimestampedAmount> dayDataList(
        Integer skip,
        Integer first,
        UniswapDayData_orderBy orderBy,
        OrderDirection  orderDirection,
        UniswapDayData_filter where
    ) {
        List<com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData> list = uniswapDayDataSubgraphFetcher.dayDataList(
            skip,
            first,
            orderBy,
            orderDirection,
            where
        );
        if (list == null) {
            return null;
        }

        Encoder encoder = Base64.getEncoder();
        
        return list.stream().map(item -> {
            double value = item.getTvlUSD().doubleValue();
            String amountId = encoder.encodeToString(
                ("TimestampedAmount:" + value + "_" + Currency.USD + "_" + item.getDate()).getBytes()
            );

            TimestampedAmount amount = new TimestampedAmount() {
                {
                    setId(amountId);
                    setCurrency(Currency.USD);
                    setValue(value);
                    setTimestamp(item.getDate());
                }
            };

            return amount;
        }).toList();
    }
}
