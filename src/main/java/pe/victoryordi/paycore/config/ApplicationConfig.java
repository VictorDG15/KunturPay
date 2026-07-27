package pe.victoryordi.paycore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OpenAPI kunturPayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("KunturPay Payments API")
                .description("API idempotente de autorización, captura y reembolso de pagos")
                .version("1.0.0")
                .contact(new Contact().name("Víctor Yordi Díaz González"))
                .license(new License().name("MIT")));
    }
}
