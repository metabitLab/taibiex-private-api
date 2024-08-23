package com.metabitlab.taibiex.privateapi.service;

import com.metabitlab.taibiex.privateapi.entity.TokenProjectEntity;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.mapper.CGlibMapper;
import com.metabitlab.taibiex.privateapi.repository.TokenProjectRepository;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
public class TokenProjectService {

    private final TokenService tokenService;

    private final TokenProjectRepository tokenProjectRepository;

    public TokenProjectService(TokenService tokenService, TokenProjectRepository tokenProjectRepository) {
        this.tokenService = tokenService;
        this.tokenProjectRepository = tokenProjectRepository;
    }

    public TokenProject findByAddress(Token token) {
        TokenProject tokenProject = new TokenProject();
        tokenProject.setTokens(List.of(token));
        tokenProject.setId(Base64.getEncoder().encodeToString(("TokenProject:TABI_" + token.getAddress() + "_" + token.getSymbol()).getBytes()));
        TokenProjectEntity projectEntity = tokenProjectRepository.findByAddress(token.getAddress());
        if (projectEntity != null){
            CGlibMapper.mapperObject(projectEntity, tokenProject);
            Image logo = Image.newBuilder().url(projectEntity.getLogoUrl()).dimensions(Dimensions.newBuilder().width(projectEntity.getLogoWidth()).height(projectEntity.getLogoHeight()).build()).build();
            logo.setId(Base64.getEncoder().encodeToString(("Image:" + logo.getUrl()).getBytes()));
            tokenProject.setLogo(logo);
        }
        return tokenProject;
    }

    public List<TokenProject> tokenProjects(List<ContractInput> contractInput) {
        List<Token> tokens = tokenService.tokens(contractInput);
        List<TokenProject> tokenProjects = tokens.stream().map(this::findByAddress).toList();
        return tokenProjects;
    }
}
