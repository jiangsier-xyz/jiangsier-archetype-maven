#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ${package}.util.AppMetaUtils;

import java.util.Objects;

@Configuration
@SuppressWarnings("unused")
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI(
            @Value("${symbol_dollar}{auth.token.headerName:${symbol_pound}{null}}}") String securityHeaderName,
            @Value("${symbol_dollar}{auth.token.parameterName:${symbol_pound}{null}}}") String securityParameterName) {
        OpenAPI openAPI = new OpenAPI();
        if (Objects.isNull(securityHeaderName) && Objects.isNull(securityParameterName)) {
            openAPI.components(new Components()
                    .addSecuritySchemes("bearerAuth", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")))
                    .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        } else {
            Components components = new Components();
            SecurityRequirement securityRequirement = new SecurityRequirement();
            if (Objects.nonNull(securityHeaderName)) {
                components.addSecuritySchemes("SysApiTokenHeader", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(securityHeaderName));
                securityRequirement.addList("SysApiTokenHeader");
            }

            if (Objects.nonNull(securityParameterName)) {
                components.addSecuritySchemes("SysApiTokenParameter", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.QUERY)
                        .name(securityParameterName));
                securityRequirement.addList("SysApiTokenParameter");
            }
            openAPI.components(components)
                    .addSecurityItem(securityRequirement);
        }

        openAPI.info(new Info()
                        .title(AppMetaUtils.getName() + " OpenAPI Definition")
                        .description(AppMetaUtils.getUrl())
                        .version(AppMetaUtils.getVersion()))
                .addServersItem(new Server().url("/"));

        return openAPI;
    }
}
