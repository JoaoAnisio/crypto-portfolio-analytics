package br.com.joaoanisio.crypto_portfolio.config;

import br.com.joaoanisio.crypto_portfolio.dto.PriceQuote;
import br.com.joaoanisio.crypto_portfolio.integration.CoinGeckoProperties;
import tools.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRICES_CACHE = "prices";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          CoinGeckoProperties properties) {

        ObjectMapper objectMapper = new ObjectMapper();

        RedisCacheConfiguration pricesConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.cacheTtl())
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new JacksonJsonRedisSerializer<>(objectMapper, PriceQuote.class)));

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration(PRICES_CACHE, pricesConfig)
                .build();
    }
}