package com.metabitlab.taibiex.privateapi.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.OrderDirection;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.UniswapDayData_orderBy;

import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 * This class provides historical protocol volume service.
 * @author Nix
 */
@Service
public class HistoricalProtocolVolumeService {
    @Autowired
    UniswapDayDataSubgraphFetcher uniswapDayDataSubgraphFetcher;

    /**
     * 按天汇总
     * @param chain
     * @param protocolVersion
     * @return
     */
    public List<TimestampedAmount> dayHistoricalProtocolVolume(
            Chain chain,
            ProtocolVersion protocolVersion) {
        List<UniswapDayData> list = uniswapDayDataSubgraphFetcher.dayDataList(
                null,
                30,
                UniswapDayData_orderBy.date,
                OrderDirection.desc,
                null);

        if (list == null) {
            return null;
        }

        Encoder encoder = Base64.getEncoder();

        return list.stream()
                .sorted(new Comparator<UniswapDayData>() {
                    @Override
                    public int compare(UniswapDayData o1, UniswapDayData o2) {
                        return o1.getDate() - o2.getDate();
                    }
                })
                .map(item -> {
                    String amountId = encoder.encodeToString(
                        ("TimestampedAmount:" + item.getVolumeUSD().doubleValue() + "_" + Currency.USD + "_" + item.getDate()).getBytes()
                    );
                    TimestampedAmount amount = new TimestampedAmount() {
                        {
                            setId(amountId);
                            setCurrency(Currency.USD);
                            setValue(item.getVolumeUSD().doubleValue());
                            setTimestamp(item.getDate());
                        }
                    };

                    return amount;
                })
                .toList();
    }

    /**
     * 按周汇总
     * @param chain
     * @param protocolVersion
     * @return
     */
    public List<TimestampedAmount> weekHistoricalProtocolVolume(
            Chain chain,
            ProtocolVersion protocolVersion) {
        List<UniswapDayData> list = uniswapDayDataSubgraphFetcher.dayDataList(
                null,
                364,
                UniswapDayData_orderBy.date,
                OrderDirection.desc,
                null);

        if (list == null) {
            return null;
        }

        Comparator<Integer> customComparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        };

        SortedMap<Integer, Double> map = new TreeMap<>(customComparator);
        for (UniswapDayData item : list) {
            Instant instant = Instant.ofEpochSecond(item.getDate().longValue());
            LocalDate date = instant.atZone(ZoneId.of("UTC")).toLocalDate();
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            Instant mondayInstant = monday.atStartOfDay(ZoneId.of("UTC")).toInstant();

            int mondayKey = (int)mondayInstant.getEpochSecond();

            Double total = map.get(mondayKey);
            if (total == null) {
                total = 0.0;
            }

            // 汇总逻辑: 累积求和
            map.put(mondayKey, total + item.getTvlUSD().doubleValue());
        }

        Encoder encoder = Base64.getEncoder();

        return map.entrySet().stream().map(item -> {
            int timestamp = item.getKey();
            double value = item.getValue();

            String amountId = encoder.encodeToString(
                ("TimestampedAmount:" + value + "_" + Currency.USD + "_" + timestamp).getBytes()
            );

            TimestampedAmount amount = new TimestampedAmount() {
                {
                    setId(amountId);
                    setCurrency(Currency.USD);
                    setValue(value);
                    setTimestamp((int)timestamp);
                }
            };

            return amount;
        }).toList();
    }

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
        // NOTE: [已确认] 参数 chain 未使用, 仅支持 TABI 
        // NOTE: [已确认] protocolVersion 参数未使用, 仅支持 V3

        List<UniswapDayData> list = uniswapDayDataSubgraphFetcher.dayDataList(
                null,
                null,
                UniswapDayData_orderBy.date,
                OrderDirection.desc,
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
            String key = year + "-" + String.format("%02d", month);

            Double total = map.get(key);
            if (total == null) {
                total = 0.0;
            }

            // 汇总逻辑: 累积求和
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
