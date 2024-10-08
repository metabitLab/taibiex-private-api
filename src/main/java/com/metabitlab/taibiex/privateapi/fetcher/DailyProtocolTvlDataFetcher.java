package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.ProtocolVersion;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TimestampedAmount;
import com.metabitlab.taibiex.privateapi.service.UniswapDayDataService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

/**
 * This is the DailyProtocolTVLDataFetcher class.
 * 
 * @author: nix
 */
@DgsComponent
public class DailyProtocolTvlDataFetcher {
    @Autowired
    UniswapDayDataService uniswapDayDataService;

    @DgsQuery
    public List<TimestampedAmount> dailyProtocolTvl(
            @InputArgument Chain chain,
            @InputArgument("version") ProtocolVersion protocolVersion) {
        // NOTE: [已确认] 参数 chain 未使用, 仅支持 TABI 
        // NOTE: [已确认] protocolVersion 参数未使用, 仅支持 V3
        List<TimestampedAmount> list = uniswapDayDataService.dayDataList(null, null, null, null, null);

        return list;
    }
}
