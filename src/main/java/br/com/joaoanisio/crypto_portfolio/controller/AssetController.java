package br.com.joaoanisio.crypto_portfolio.controller;

import br.com.joaoanisio.crypto_portfolio.dto.AssetResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PriceResponse;
import br.com.joaoanisio.crypto_portfolio.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Ativos", description = "Consulta de criptoativos suportados e suas cotações")
@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @Operation(
            summary = "Lista os ativos suportados",
            description = "Retorna todos os criptoativos disponíveis para registro de transações, ordenados por símbolo.")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    @GetMapping
    public List<AssetResponse> findAll() {
        return assetService.findAll();
    }

    @Operation(
            summary = "Consulta a cotação atual de um ativo",
            description = """
                    Retorna o preço de mercado em BRL. O resultado é cacheado por 60 segundos;
                    o campo `fetchedAt` indica o momento em que a cotação foi obtida na origem.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cotação retornada"),
            @ApiResponse(responseCode = "404", description = "Ativo não encontrado", content = @Content),
            @ApiResponse(responseCode = "503", description = "Serviço de cotações indisponível", content = @Content)
    })
    @GetMapping("/{symbol}/price")
    public PriceResponse getPrice(
            @Parameter(description = "Símbolo do ativo", example = "BTC")
            @PathVariable String symbol) {
        return assetService.getCurrentPrice(symbol);
    }
}