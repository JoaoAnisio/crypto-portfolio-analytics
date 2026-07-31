package br.com.joaoanisio.crypto_portfolio.controller;

import br.com.joaoanisio.crypto_portfolio.dto.AssetResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PriceResponse;
import br.com.joaoanisio.crypto_portfolio.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public List<AssetResponse> findAll() {
        return assetService.findAll();
    }

    @GetMapping("/{symbol}/price")
    public PriceResponse getPrice(@PathVariable String symbol) {
        return assetService.getCurrentPrice(symbol);
    }
}
