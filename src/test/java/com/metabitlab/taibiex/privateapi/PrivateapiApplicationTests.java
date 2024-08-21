package com.metabitlab.taibiex.privateapi;

import com.metabitlab.taibiex.privateapi.subgraphfetcher.PoolsSubgraphFetcher;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Pool;
import com.metabitlab.taibiex.privateapi.subgraphsclient.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.subgraphfetcher.TokenSubgraphFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class PrivateapiApplicationTests {

	@Autowired
	TokenSubgraphFetcher tokenSubgraphFetcher;
	@Autowired
	PoolsSubgraphFetcher poolsSubgraphFetcher;

	@Test
	void contextLoads() {
		Pool pool = poolsSubgraphFetcher.pool("0x167a3aebec6c3ad9fd66b95d66d41a883209ae60");
		System.out.println(1);
	}

}
