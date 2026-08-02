package br.com.joaoanisio.crypto_portfolio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cryptoPortfolioOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crypto Portfolio Analytics API")
                        .version("1.0.0")
                        .description("""
                                API REST para acompanhamento de portfólio de criptoativos.

                                Calcula posição por custo médio ponderado, resultado realizado
                                e não realizado, alocação percentual e histórico diário
                                automatizado, com cotações de mercado em tempo real.
                                """)
                        .contact(new Contact()
                                .name("João Anísio")
                                .url("https://github.com/SEU_USUARIO"))
                        .license(new License().name("MIT")));
    }
}
