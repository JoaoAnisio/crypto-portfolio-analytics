package br.com.joaoanisio.crypto_portfolio;

import org.springframework.boot.SpringApplication;

public class TestCryptoPortfolioApplication {

	public static void main(String[] args) {
		SpringApplication.from(CryptoPortfolioApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
