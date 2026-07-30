package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.dto.AssetResponse;
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

    @Transactional(readOnly = true)
    public List<AssetResponse> findAll() {
        return assetRepository.findAll().stream()
                .sorted(Comparator.comparing(a -> a.getSymbol()))
                .map(AssetResponse::from)
                .toList();
    }
}
