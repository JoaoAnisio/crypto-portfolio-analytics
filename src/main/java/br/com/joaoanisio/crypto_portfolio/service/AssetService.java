package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.domain.Asset;
import br.com.joaoanisio.crypto_portfolio.dto.AssetResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PriceQuote;
import br.com.joaoanisio.crypto_portfolio.dto.PriceResponse;
import br.com.joaoanisio.crypto_portfolio.exception.ResourceNotFoundException;
import br.com.joaoanisio.crypto_portfolio.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final PriceService priceService;

    @Transactional(readOnly = true)
    public List<AssetResponse> findAll() {
        return assetRepository.findAll().stream()
                .sorted(Comparator.comparing(a -> a.getSymbol()))
                .map(AssetResponse::from)
                .toList();
    }

    // Sem @Transactional de propósito: a chamada HTTP à CoinGecko não deve acontecer com uma conexão de banco reservada
    public PriceResponse getCurrentPrice(String symbol) {
        Asset asset = assetRepository.findBySymbolIgnoreCase(symbol)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ativo não encontrado para o símbolo: " + symbol));

        PriceQuote quote = priceService.getPrice(asset.getCoingeckoId());

        return new PriceResponse(
                asset.getSymbol(),
                asset.getName(),
                quote.currency().toUpperCase(),
                quote.price(),
                quote.fetchedAt());
    }
}
