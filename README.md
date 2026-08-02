# Crypto Portfolio Analytics API

[![CI](https://github.com/JoaoAnisio/crypto-portfolio-analytics/actions/workflows/ci.yml/badge.svg)](https://github.com/JoaoAnisio/crypto-portfolio-analytics/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green)
![License](https://img.shields.io/badge/license-MIT-blue)

API REST para acompanhamento de portfólio de criptoativos. Calcula posição por
**custo médio ponderado** (o método adotado pela Receita Federal para apuração de
ganho de capital), separa resultado **realizado** de **não realizado**, e mantém
histórico diário automatizado com cotações de mercado em tempo real.

🔗 **[Documentação interativa (Swagger)](https://sua-app.onrender.com/swagger-ui.html)**

---

## O problema

Ferramentas simples de portfólio mostram apenas "quanto vale hoje". Isso é
enganoso: um investidor que vendeu no topo e ficou com uma posição menor pode
aparecer no vermelho, mesmo tendo lucro consolidado.

Esta API resolve isso separando as duas dimensões do resultado:

| Métrica | Pergunta que responde |
|---|---|
| Não realizado | Quanto eu ganharia se vendesse tudo agora? |
| Realizado | Quanto eu já ganhei nas vendas que fiz? |
| Total | Qual meu resultado de fato? |

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistência | PostgreSQL 16 + Spring Data JPA |
| Migrations | Flyway |
| Cache | Redis |
| Cotações | CoinGecko API |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, AssertJ, Testcontainers |
| CI | GitHub Actions |
| Container | Docker (multi-stage) |

---

## Arquitetura

```
┌─────────────┐
│ Controller  │  HTTP, validação de formato, status codes
└──────┬──────┘
       │
┌──────▼──────┐      ┌──────────────┐      ┌───────────┐
│   Service   │─────▶│ PriceService │─────▶│   Redis   │
│ regras de   │      │  (cache)     │      │  TTL 60s  │
│  negócio    │      └──────┬───────┘      └───────────┘
└──────┬──────┘             │
       │                    ▼
┌──────▼──────┐      ┌──────────────┐
│ Repository  │      │  CoinGecko   │
└──────┬──────┘      │  (timeout)   │
       │             └──────────────┘
┌──────▼──────┐
│ PostgreSQL  │
└─────────────┘

┌─────────────┐
│  Scheduler  │  snapshot diário às 23:50 (idempotente)
└─────────────┘
```

---

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/transactions` | Registra compra ou venda |
| `GET` | `/transactions` | Lista transações |
| `GET` | `/transactions/{id}` | Detalha uma transação |
| `DELETE` | `/transactions/{id}` | Remove transação |
| `GET` | `/assets` | Lista ativos suportados |
| `GET` | `/assets/{symbol}/price` | Cotação atual (com cache) |
| `GET` | `/portfolio/summary` | Resumo consolidado |
| `GET` | `/portfolio/allocation` | Alocação percentual |
| `GET` | `/portfolio/history` | Série histórica |
| `POST` | `/portfolio/snapshots` | Captura snapshot manual |

---

## Como rodar

**Pré-requisitos:** Docker e Docker Compose.

```bash
git clone https://github.com/JoaoAnisio/crypto-portfolio-analytics.git
cd crypto-portfolio-analytics

# Opcional: chave gratuita da CoinGecko para limites maiores
echo "COINGECKO_API_KEY=sua_chave" > .env

docker compose --profile full up
```

Acesse http://localhost:8080/swagger-ui.html

**Desenvolvimento** (aplicação na IDE, dependências em container):

```bash
docker compose up -d
./mvnw spring-boot:run
```

**Testes:**

```bash
./mvnw test              # tudo (Testcontainers sobe Postgres real)
./mvnw test -Dtest='*Test'   # só os unitários, sem Docker
```

---

## Decisões técnicas

### `BigDecimal` e `NUMERIC` para todos os valores monetários

`double` é ponto flutuante binário e não representa `0.1` exatamente — os erros
acumulam. Com ETH tendo 18 casas decimais, isso vira divergência real. Todas as
comparações usam `compareTo` e não `equals`, porque `BigDecimal.equals` considera
a escala: `1.0` e `1.00` seriam "diferentes".

### Flyway com `ddl-auto: validate`

O schema é versionado em SQL e o Hibernate atua como fiscal, não como criador.
Divergência entre entidade e tabela derruba a aplicação no boot, não em produção.

### Cache com TTL curto na camada de serviço

Cotações ficam 60s no Redis. O `@Cacheable` fica no `PriceService` e não no
`CoinGeckoClient` de propósito: o cache do Spring funciona por proxy, e
auto-invocação dentro do mesmo bean não passaria pelo proxy — o cache falharia
silenciosamente.

O método em lote (`getPrices`) não usa `@Cacheable`, porque a anotação cachearia
a coleção inteira sob uma única chave. Ele acessa o `CacheManager` diretamente
para ter granularidade por ativo e fazer **uma** requisição externa por cálculo
de portfólio, independentemente do número de ativos.

### Timeout obrigatório na integração externa

Sem timeout, o padrão do Java é esperar indefinidamente. Uma API externa lenta
prenderia threads do Tomcat até esgotar o pool, derrubando toda a aplicação.
Falha externa retorna **503**, não 500 — o cliente sabe que pode tentar de novo.

### Custo médio ponderado com acumulador sequencial

`PositionAccumulator` aplica transações em ordem cronológica mantendo estado.
Optei deliberadamente por laço explícito em vez de Stream nesse ponto: o cálculo
é sequencial e dependente de ordem, e forçar `reduce` prejudicaria a legibilidade
sem ganho. Streams são usados nas agregações, onde de fato brilham.

### Snapshots idempotentes

Constraint `UNIQUE (snapshot_date)` no banco somada a estratégia de upsert no
service. O job pode rodar múltiplas vezes sem duplicar, e duas instâncias
simultâneas são barradas pelo banco.

### `Clock` injetado

Torna o código dependente de data testável: os testes usam `Clock.fixed` e
afirmam datas absolutas. O fuso é fixado em `America/Sao_Paulo` porque
containers rodam em UTC por padrão, o que faria um job das 23h gravar com a
data errada.

### Testcontainers em vez de H2

H2 não é PostgreSQL: não valida migrations com sintaxe específica, difere no
tratamento de `NUMERIC` e `TIMESTAMPTZ`. Testar contra um banco diferente do de
produção significa testes verdes com aplicação quebrada.

### Respostas de erro no padrão RFC 7807

`ProblemDetail` nativo do Spring, com distinção semântica entre 400 (formato
inválido), 404 (recurso ausente), 422 (regra de negócio violada) e 503
(dependência externa indisponível).

---

## Testes

18 testes em três níveis:

- **Unitários puros** — `PositionAccumulator`, a matemática do custo médio, em milissegundos
- **Unitários com mock** — services, orquestração isolada com Mockito
- **Integração** — stack completa com PostgreSQL real via Testcontainers

Destaque: um teste verifica explicitamente que as cotações são buscadas em
**uma única chamada** ao serviço externo. Se alguém refatorar para chamar em
loop, o build quebra.

---

## Autor

**João Anísio** — [LinkedIn](https://www.linkedin.com/in/jo%C3%A3o-gnd/) · [GitHub](https://github.com/JoaoAnisio)

## Licença

MIT