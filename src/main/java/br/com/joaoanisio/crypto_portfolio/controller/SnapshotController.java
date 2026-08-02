package br.com.joaoanisio.crypto_portfolio.controller;

import br.com.joaoanisio.crypto_portfolio.dto.SnapshotResponse;
import br.com.joaoanisio.crypto_portfolio.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Histórico", description = "Série temporal da evolução do portfólio")
@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class SnapshotController {

    private final SnapshotService snapshotService;

    @Operation(summary = "Histórico de snapshots", description = """
            Retorna os registros diários do portfólio.

            Sem filtro, lista tudo em ordem decrescente (mais recente primeiro).
            Com `from` e `to`, retorna o período em ordem crescente — formato
            adequado para alimentar gráficos de linha.
            """)
    @ApiResponse(responseCode = "200", description = "Histórico retornado")
    @GetMapping("/history")
    public List<SnapshotResponse> getHistory(
            @Parameter(description = "Data inicial (inclusive)", example = "2026-07-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Data final (inclusive)", example = "2026-08-02") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return snapshotService.findHistory(from, to);
    }

    @Operation(summary = "Captura um snapshot manualmente", description = """
            Registra o estado atual do portfólio fora do horário agendado (23:50 diário).

            A operação é idempotente por data: chamadas repetidas no mesmo dia
            atualizam o registro existente em vez de criar duplicatas.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Snapshot registrado ou atualizado"),
            @ApiResponse(responseCode = "503", description = "Serviço de cotações indisponível", content = @Content)
    })
    @PostMapping("/snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    public SnapshotResponse capture() {
        return snapshotService.captureToday();
    }
}