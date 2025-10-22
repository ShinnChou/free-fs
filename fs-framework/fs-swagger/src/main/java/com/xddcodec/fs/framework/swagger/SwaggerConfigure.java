package com.xddcodec.fs.framework.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置
 *
 * @Author: hao.ding@insentek.com
 * @Date: 2024/11/18 14:08
 */
@Configuration
public class SwaggerConfigure {

    @Bean
    public OpenAPI springShopOpenAPI() {
        final String loginToken = "BearerAuth";
        return new OpenAPI()
                .info(new Info().title("FreeFs API")
                        .description("FreeFs API文档")
                        .version("v2.0.0")
                        .license(new License().name("Apache 2.0").url("https://github.com/xddcode/free-fs/blob/master/LICENSE")))
                .externalDocs(new ExternalDocumentation()
                        .description("项目文档")
                        .url("https://doc.elites.chat"))
                .components(new Components()
                        .addSecuritySchemes(loginToken,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("Bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name(loginToken)))
                .addSecurityItem(new SecurityRequirement().addList(loginToken));
    }

}
