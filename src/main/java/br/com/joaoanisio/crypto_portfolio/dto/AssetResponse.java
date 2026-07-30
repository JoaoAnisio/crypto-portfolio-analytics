package br.com.joaoanisio.crypto_portfolio.dto;

import br.com.joaoanisio.crypto_portfolio.domain.Asset;

import java.util.UUID;

public record AssetResponse(UUID id, String symbol, String name) {

    public static AssetResponse from(Asset asset) {
        return new AssetResponse(asset.getId(), asset.getSymbol(), asset.getName());
    }
}