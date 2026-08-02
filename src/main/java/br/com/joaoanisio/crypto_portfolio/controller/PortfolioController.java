package br.com.joaoanisio.crypto_portfolio.controller;

import br.com.joaoanisio.crypto_portfolio.dto.AllocationResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PortfolioSummaryResponse;
import br.com.joaoanisio.crypto_portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/summary")
    public PortfolioSummaryResponse getSummary() {
        return portfolioService.getSummary();
    }

    @GetMapping("/allocation")
    public List<AllocationResponse> getAllocation() {
        return portfolioService.getAllocation();
    }
}