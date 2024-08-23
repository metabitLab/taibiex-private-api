package com.metabitlab.taibiex.privateapi;

import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolMinuteDataSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolsSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.*;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.util.DateUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class PrivateapiApplicationTests {

	@Autowired
	TokenSubgraphFetcher tokenSubgraphFetcher;
	@Autowired
	PoolsSubgraphFetcher poolsSubgraphFetcher;
	@Autowired
	PoolMinuteDataSubgraphFetcher poolMinuteDataSubgraphFetcher;

	@Test
	void contextLoads() {
		long[] fiveMinuteTimestamp = DateUtil.getFiveMinuteTimestamp(12);
		List<String> ids = new ArrayList<>();
		for (long timestamp : fiveMinuteTimestamp) {
			ids.add("0x6cec4df7ad64d5d06860a397c17edebc5f311ae3" + "-" + timestamp/60);
		}
		List<PoolMinuteData> poolMinuteData = poolMinuteDataSubgraphFetcher.poolMinuteDatas(0, null, PoolMinuteData_orderBy.periodStartUnix, OrderDirection.desc,
				new PoolMinuteData_filter() {{
					setId_in(ids);
				}});
		System.out.println(1);
	}

}
