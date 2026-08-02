package br.com.joaoanisio.crypto_portfolio.controller;

import br.com.joaoanisio.crypto_portfolio.AbstractIntegrationTest;
import br.com.joaoanisio.crypto_portfolio.dto.PriceQuote;
import br.com.joaoanisio.crypto_portfolio.repository.TransactionRepository;
import br.com.joaoanisio.crypto_portfolio.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@DisplayName("API de transacoes")
class TransactionControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * A API externa e substituida por um dublê: teste nao pode depender
     * de rede nem de preco que muda a cada minuto.
     */
    @MockitoBean
    private PriceService priceService;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();

        when(priceService.getPrice(anyString()))
                .thenReturn(new PriceQuote("bitcoin", "brl", new BigDecimal("400000"), Instant.now()));

        when(priceService.getPrices(anyCollection())).thenReturn(Map.of(
                "bitcoin", new PriceQuote("bitcoin", "brl", new BigDecimal("400000"), Instant.now()),
                "ethereum", new PriceQuote("ethereum", "brl", new BigDecimal("20000"), Instant.now())));
    }

    private String body(String symbol, String type, String qty, String price, String date) {
        return """
                {
                  "symbol": "%s",
                  "type": "%s",
                  "quantity": "%s",
                  "unitPrice": "%s",
                  "executedAt": "%s"
                }
                """.formatted(symbol, type, qty, price, date);
    }

    @Test
    @DisplayName("cria transacao e devolve 201 com header Location")
    void createsTransaction() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("BTC", "BUY", "0.5", "300000", "2026-01-10T12:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.totalValue").value(150000.00));
    }

    @Test
    @DisplayName("rejeita payload invalido com 400 e detalha os campos")
    void rejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "BUY", "-1", "100", "2030-01-01T00:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.fields.symbol").exists())
                .andExpect(jsonPath("$.fields.quantity").exists())
                .andExpect(jsonPath("$.fields.executedAt").exists());
    }

    @Test
    @DisplayName("devolve 404 para ativo inexistente")
    void rejectsUnknownAsset() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DOGE", "BUY", "100", "1.5", "2026-01-10T12:00:00Z")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("devolve 422 ao vender acima do saldo")
    void rejectsOversell() throws Exception {
        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("BTC", "BUY", "0.5", "300000", "2026-01-10T12:00:00Z")));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("BTC", "SELL", "2", "500000", "2026-02-10T12:00:00Z")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    @DisplayName("calcula o resumo do portfolio de ponta a ponta")
    void calculatesSummaryEndToEnd() throws Exception {
        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                .content(body("BTC", "BUY", "0.5", "300000", "2026-01-10T12:00:00Z")));
        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                .content(body("BTC", "BUY", "0.5", "400000", "2026-02-15T12:00:00Z")));
        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                .content(body("BTC", "SELL", "0.25", "500000", "2026-03-20T12:00:00Z")));

        mockMvc.perform(get("/portfolio/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvested").value(262500.00))
                .andExpect(jsonPath("$.realizedPnl").value(37500.00))
                .andExpect(jsonPath("$.openPositions").value(1))
                .andExpect(jsonPath("$.positions[0].averageCost").value(350000.00000000))
                .andExpect(jsonPath("$.positions[0].allocationPercent").value(100.00));
    }

    @Test
    @DisplayName("lista os ativos do seed aplicado pelo Flyway")
    void listsSeededAssets() throws Exception {
        mockMvc.perform(get("/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }
}