package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.config.CacheConfig;
import br.com.joaoanisio.crypto_portfolio.dto.PriceQuote;
import br.com.joaoanisio.crypto_portfolio.exception.ExternalServiceException;
import br.com.joaoanisio.crypto_portfolio.integration.CoinGeckoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    private final CoinGeckoClient coinGeckoClient;
    private final CacheManager cacheManager;

    // Cotação de um único ativo. O cache é gerenciado pelo Spring via proxy.
    @Cacheable(cacheNames = CacheConfig.PRICES_CACHE, key = "#coingeckoId")
    public PriceQuote getPrice(String coingeckoId) {
        PriceQuote quote = coinGeckoClient.fetchPrices(Set.of(coingeckoId)).get(coingeckoId);

        if (quote == null) {
            throw new ExternalServiceException("CoinGecko não retornou preço para: " + coingeckoId);
        }
        return quote;
    }

    // Cotação de vários ativos. Lê do cache o que já existe e busca apenas os ausentes — em uma única chamada à API externa.
    public Map<String, PriceQuote> getPrices(Collection<String> coingeckoIds) {
        Cache cache = cacheManager.getCache(CacheConfig.PRICES_CACHE);

        Map<String, PriceQuote> result = new HashMap<>();
        Set<String> missing = new HashSet<>();

        for (String id : coingeckoIds) {
            PriceQuote cached = (cache != null) ? cache.get(id, PriceQuote.class) : null;
            if (cached != null) {
                result.put(id, cached);
            } else {
                missing.add(id);
            }
        }

        log.debug("Cache de preços | hits={} | misses={}", result.size(), missing.size());

        if (!missing.isEmpty()) {
            coinGeckoClient.fetchPrices(missing).forEach((id, quote) -> {
                if (cache != null) {
                    cache.put(id, quote);
                }
                result.put(id, quote);
            });
        }

        return result;
    }
}