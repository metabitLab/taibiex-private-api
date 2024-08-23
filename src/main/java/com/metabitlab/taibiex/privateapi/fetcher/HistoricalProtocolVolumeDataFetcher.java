package com.metabitlab.taibiex.privateapi.fetcher;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.metabitlab.taibiex.privateapi.errors.UnSupportDurationException;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.HistoryDuration;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.ProtocolVersion;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TimestampedAmount;
import com.metabitlab.taibiex.privateapi.service.HistoricalProtocolVolumeService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

/**
 * This is the HistoricalProtocolVolumeDataFetcher class.
 * 
 * @author: nix
 */
@DgsComponent
public class HistoricalProtocolVolumeDataFetcher {
    @Autowired
    HistoricalProtocolVolumeService historicalProtocolVolumeService;

    @DgsQuery
    public List<TimestampedAmount> historicalProtocolVolume(
            @InputArgument Chain chain,
            @InputArgument("version") ProtocolVersion protocolVersion,
            @InputArgument HistoryDuration duration) {
        switch (duration) {
            case DAY:
            case WEEK:
            case YEAR:
                // TODO: 未实现 duration 为 DAY, WEEK, YEAR 的逻辑
                return null;
            case MONTH:
                return historicalProtocolVolumeService.monthHistoricalProtocolVolume(chain, protocolVersion);
            default:
                throw new UnSupportDurationException("This duration is not supported", duration);
        }
    }
}
