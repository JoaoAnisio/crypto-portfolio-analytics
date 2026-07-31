package br.com.joaoanisio.crypto_portfolio.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "coingecko")
public record CoinGeckoProperties(
        String baseUrl,
        String apiKey,
        String vsCurrency,
        Duration connectTimeout,
        Duration readTimeout,
        Duration cacheTtl
) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}