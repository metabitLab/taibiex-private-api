package com.metabitlab.taibiex.privateapi.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Currency;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.ProtocolVersion;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TimestampedAmount;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.UniswapDayDataSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData;

/**
 * This class provides historical protocol volume service.
 * @author Nix
 */
@Service
public class HistoricalProtocolVolumeService {
    @Autowired
    UniswapDayDataSubgraphFetcher uniswapDayDataSubgraphFetcher;

    /**
     * 按月汇总
     * 
     * @param chain
     * @param protocolVersion
     * @return
     */
    public List<TimestampedAmount> monthHistoricalProtocolVolume(
            Chain chain,
            ProtocolVersion protocolVersion) {
        // TODO: 参数未使用

        List<UniswapDayData> list = uniswapDayDataSubgraphFetcher.dayDataList(
                null,
                null,
                null,
                null,
                null);

        if (list == null) {
            return null;
        }

        Comparator<String> customComparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String[] arr1 = o1.split("-");
                String[] arr2 = o2.split("-");

                int year1 = Integer.parseInt(arr1[0]);
                int year2 = Integer.parseInt(arr2[0]);
                if (year1 != year2) {
                    return year1 - year2;
                }

                int month1 = Integer.parseInt(arr1[1]);
                int month2 = Integer.parseInt(arr2[1]);
                return month1 - month2;
            }
        };

        SortedMap<String, Double> map = new TreeMap<>(customComparator); 
        for (UniswapDayData item : list) {
            Instant instant = Instant.ofEpochSecond(item.getDate().longValue());
            LocalDate date = instant.atZone(ZoneId.of("UTC")).toLocalDate();

            int year = date.getYear();
            int month = date.getMonthValue();
            String key = year + "-" + month;

            Double total = map.get(key);
            if (total == null) {
                total = 0.0;
            }

            // TODO: 汇总逻辑: 累积求和
            map.put(key, total + item.getTvlUSD().doubleValue());
        }

        Encoder encoder = Base64.getEncoder();

        return map.entrySet().stream().map(item -> {
            String yearMonth = item.getKey();
            Double value = item.getValue();

            int timestamp = (int)Instant.parse(yearMonth + "-01T00:00:00Z").getEpochSecond();
            String amountId = encoder.encodeToString(
                ("TimestampedAmount:" + value + "_" + Currency.USD + "_" + timestamp).getBytes()
            );

            TimestampedAmount amount = new TimestampedAmount() {
                {
                    setId(amountId);
                    setCurrency(Currency.USD);
                    setValue(value);
                    setTimestamp(timestamp);
                }
            };

            return amount;
        }).toList();
    }
}
