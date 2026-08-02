package br.com.joaoanisio.crypto_portfolio.controller;

import br.com.joaoanisio.crypto_portfolio.dto.SnapshotResponse;
import br.com.joaoanisio.crypto_portfolio.service.SnapshotService;
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

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class SnapshotController {

    private final SnapshotService snapshotService;

    @GetMapping("/history")
    public List<SnapshotResponse> getHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return snapshotService.findHistory(from, to);
    }

    // Dispara a captura manualmente. Útil para testes e para forçar uma atualização fora do horário agendado
    @PostMapping("/snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    public SnapshotResponse capture() {
        return snapshotService.captureToday();
    }
}