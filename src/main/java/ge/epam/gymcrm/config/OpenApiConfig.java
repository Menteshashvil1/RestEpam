package ge.epam.gymcrm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gymCrmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gym CRM REST API")
                        .version("1.0")
                        .description("""
                                REST API of the Gym CRM system.

                                Every endpoint except trainee/trainer registration and login requires
                                the caller's credentials in the `X-Auth-Username` and `X-Auth-Password`
                                headers. Each response carries an `X-Transaction-Id` header that ties
                                the call to its log entries."""))
                .components(new Components()
                        .addParameters("X-Auth-Username", new HeaderParameter()
                                .name(AuthenticationInterceptor.USERNAME_HEADER)
                                .description("Authenticated user's username")
                                .required(true)
                                .schema(new StringSchema()))
                        .addParameters("X-Auth-Password", new HeaderParameter()
                                .name(AuthenticationInterceptor.PASSWORD_HEADER)
                                .description("Authenticated user's password")
                                .required(true)
                                .schema(new StringSchema())));
    }
}
