package br.com.joaoanisio.crypto_portfolio.controller;

import br.com.joaoanisio.crypto_portfolio.dto.AllocationResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PortfolioSummaryResponse;
import br.com.joaoanisio.crypto_portfolio.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Portfólio", description = "Consolidação da posição atual com cotações de mercado")
@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(
            summary = "Resumo consolidado do portfólio",
            description = """
                    Calcula a posição de cada ativo por custo médio ponderado e valoriza
                    pelas cotações de mercado atuais.

                    Separa o resultado em duas dimensões:
                    - **não realizado**: variação das posições ainda em aberto
                    - **realizado**: resultado já materializado nas vendas concluídas

                    Portfólio sem transações retorna 200 com valores zerados.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo calculado"),
            @ApiResponse(responseCode = "503", description = "Serviço de cotações indisponível", content = @Content)
    })
    @GetMapping("/summary")
    public PortfolioSummaryResponse getSummary() {
        return portfolioService.getSummary();
    }

    @Operation(
            summary = "Alocação percentual por ativo",
            description = "Participação de cada ativo no valor total do portfólio, em ordem decrescente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alocação calculada"),
            @ApiResponse(responseCode = "503", description = "Serviço de cotações indisponível", content = @Content)
    })
    @GetMapping("/allocation")
    public List<AllocationResponse> getAllocation() {
        return portfolioService.getAllocation();
    }
}