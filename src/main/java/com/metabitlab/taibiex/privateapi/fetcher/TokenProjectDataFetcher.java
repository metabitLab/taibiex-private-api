package com.metabitlab.taibiex.privateapi.fetcher;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Chain;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.ContractInput;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProject;
import com.metabitlab.taibiex.privateapi.service.TokenProjectService;
import com.metabitlab.taibiex.privateapi.service.TokenService;
import com.netflix.graphql.dgs.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@DgsComponent
public class TokenProjectDataFetcher {

    private final TokenProjectService tokenProjectService;

    public TokenProjectDataFetcher(TokenProjectService tokenProjectService) {
        this.tokenProjectService = tokenProjectService;
    }

    @DgsQuery
    public List<TokenProject> tokenProjects(@InputArgument(name = "contracts") List<ContractInput> contractInputs) {
        return tokenProjectService.tokenProjects(contractInputs);
    }
}
