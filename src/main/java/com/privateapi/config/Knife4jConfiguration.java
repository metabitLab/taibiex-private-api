package com.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

//reslove: when the knife4j is false https://stackoverflow.com/questions/64500674/classcastexception-org-springframework-beans-factory-support-nullbean-cannot-be


//@Profile("dev|bsc|test")
//@Profile("!prod")
//@ConditionalOnProperty(name = "app.knife4j", matchIfMissing = false)
@ConditionalOnProperty(name = "app.knife4j", matchIfMissing = false)
@Configuration
@EnableSwagger2WebMvc
public class Knife4jConfiguration extends WebMvcConfigurationSupport {

    //http://localhost:8802/doc.html

    private static boolean bOpenKnife4j = true;

    @Value("${app.knife4j}")
    public void setKnife4j(boolean openKnife4j) {
        Knife4jConfiguration.bOpenKnife4j = openKnife4j;
    }

    @Bean
    public Docket docket() {
        if (!bOpenKnife4j) {
            return null;
        }

        //指定使用Swagger2规范
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        //描述字段支持Markdown语法
                        .description("office-token-backend APIs")
                        .termsOfServiceUrl("")
                        .version("1.0")
                        .build())
                //分组名称
                .groupName("1.0")
                .select()
                //这里指定Controller扫描包路径
                .apis(RequestHandlerSelectors.basePackage("com.app.controller"))
                .paths(PathSelectors.any())
                .build();

        return docket;
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");

    }


}
