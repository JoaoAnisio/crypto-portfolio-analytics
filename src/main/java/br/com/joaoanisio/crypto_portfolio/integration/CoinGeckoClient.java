package br.com.joaoanisio.crypto_portfolio.integration;

import br.com.joaoanisio.crypto_portfolio.dto.PriceQuote;
import br.com.joaoanisio.crypto_portfolio.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CoinGeckoClient {

    private static final ParameterizedTypeReference<Map<String, Map<String, BigDecimal>>> PRICE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final CoinGeckoProperties properties;

    public CoinGeckoClient(RestClient coinGeckoRestClient, CoinGeckoProperties properties) {
        this.restClient = coinGeckoRestClient;
        this.properties = properties;
    }

    //Busca a cotação de vários ativos em uma única requisição.
    public Map<String, PriceQuote> fetchPrices(Collection<String> coingeckoIds) {
        if (coingeckoIds.isEmpty()) {
            return Map.of();
        }

        String ids = String.join(",", coingeckoIds);
        String currency = properties.vsCurrency();

        log.info("Consultando CoinGecko | ativos={} | ids={}", coingeckoIds.size(), ids);

        Map<String, Map<String, BigDecimal>> body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/simple/price")
                            .queryParam("ids", ids)
                            .queryParam("vs_currencies", currency)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, (request, response) -> {
                        throw new ExternalServiceException(
                                "Limite de requisições da CoinGecko atingido. Tente novamente em instantes.");
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new ExternalServiceException(
                                "CoinGecko respondeu com status " + response.getStatusCode());
                    })
                    .body(PRICE_TYPE);

        } catch (ResourceAccessException ex) {
            throw new ExternalServiceException(
                    "Falha de comunicação com a CoinGecko: " + ex.getMessage(), ex);
        }

        if (body == null) {
            throw new ExternalServiceException("CoinGecko retornou uma resposta vazia");
        }

        Instant now = Instant.now();
        Map<String, PriceQuote> quotes = new HashMap<>();

        body.forEach((coingeckoId, pricesByCurrency) -> {
            BigDecimal price = pricesByCurrency.get(currency);
            if (price != null) {
                quotes.put(coingeckoId, new PriceQuote(coingeckoId, currency, price, now));
            }
        });

        return quotes;
    }
}