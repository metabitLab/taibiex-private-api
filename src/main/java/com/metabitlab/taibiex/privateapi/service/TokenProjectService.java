package com.metabitlab.taibiex.privateapi.service;

import com.metabitlab.taibiex.privateapi.entity.TokenProjectEntity;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Dimensions;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Image;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.Token;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.TokenProject;
import com.metabitlab.taibiex.privateapi.mapper.CGlibMapper;
import com.metabitlab.taibiex.privateapi.repository.TokenProjectRepository;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class TokenProjectService {

    private final TokenProjectRepository tokenProjectRepository;

    public TokenProjectService(TokenProjectRepository tokenProjectRepository) {
        this.tokenProjectRepository = tokenProjectRepository;
    }

    public TokenProject findByAddress(Token token) {
        TokenProject tokenProject = new TokenProject();
        tokenProject.setId(Base64.getEncoder().encodeToString(("TokenProject:TABI_" + token.getAddress() + "_" + token.getSymbol()).getBytes()));
        TokenProjectEntity projectEntity = tokenProjectRepository.findByAddress(token.getAddress());
        if (projectEntity != null){
            CGlibMapper.mapper(projectEntity, TokenProject.class);
            Image logo = Image.newBuilder().url(projectEntity.getLogoUrl()).dimensions(Dimensions.newBuilder().width(projectEntity.getLogoWidth()).height(projectEntity.getLogoHeight()).build()).build();
            logo.setId(Base64.getEncoder().encodeToString(("Image:" + logo.getUrl()).getBytes()));
            tokenProject.setLogo(logo);
        }
        return tokenProject;
    }
}
