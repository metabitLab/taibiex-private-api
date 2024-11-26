package com.metabitlab.taibiex.privateapi.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.metabitlab.taibiex.privateapi.entity.TokenProjectEntity;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.metabitlab.taibiex.privateapi.mapper.CGlibMapper;
import com.metabitlab.taibiex.privateapi.repository.TokenProjectRepository;
import com.metabitlab.taibiex.privateapi.util.RedisService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class TokenProjectService {

    private final RedisService redisService;

    private final TokenService tokenService;

    private final TokenProjectRepository tokenProjectRepository;

    public TokenProjectService(RedisService redisService, TokenService tokenService, TokenProjectRepository tokenProjectRepository) {
        this.redisService = redisService;
        this.tokenService = tokenService;
        this.tokenProjectRepository = tokenProjectRepository;
    }

    public TokenProject findByAddress(Token token) {
        String cacheKey = "token_project:" + token.getId() + "_" + token.getChain().name() + "_" + token.getSymbol();

        try {
            Object tokenProject = redisService.get(cacheKey);
            if (null != tokenProject) {
                return JSONObject.parseObject(tokenProject.toString(), TokenProject.class);
            }
        } catch (Exception e) {
            log.error("tokenProject redis read error：{}", e.getMessage());
        }

        TokenProject tokenProject = new TokenProject();
        tokenProject.setTokens(List.of(token));
        tokenProject.setId(Base64.getEncoder().encodeToString(("TokenProject:TABI_" + token.getAddress() + "_" + token.getSymbol()).getBytes()));
        TokenProjectEntity projectEntity = null;
        if (StringUtils.hasLength(token.getAddress())){
            projectEntity = tokenProjectRepository.findByAddress(token.getAddress());
        }
        if (projectEntity != null){
            CGlibMapper.mapperObject(projectEntity, tokenProject);
            Image logo = Image.newBuilder().url(projectEntity.getLogoUrl()).dimensions(Dimensions.newBuilder().width(projectEntity.getLogoWidth()).height(projectEntity.getLogoHeight()).build()).build();
            logo.setId(Base64.getEncoder().encodeToString(("Image:" + logo.getUrl()).getBytes()));
            tokenProject.setLogo(logo);
            try {
                String pvoStr = JSON.toJSONString(tokenProject, SerializerFeature.WriteNullStringAsEmpty);
                redisService.set(cacheKey, pvoStr, 10, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("topTokens redis write error：{}", e.getMessage());
            }
        }

        return tokenProject;
    }

    public List<TokenProject> tokenProjects(List<ContractInput> contractInput) {
        List<Token> tokens = tokenService.tokens(contractInput);
        List<TokenProject> tokenProjects = tokens.stream().map(this::findByAddress).toList();
        return tokenProjects;
    }
}
