INSERT INTO assets (id, symbol, name, coingecko_id) VALUES
    (gen_random_uuid(), 'BTC',  'Bitcoin',  'bitcoin'),
    (gen_random_uuid(), 'ETH',  'Ethereum', 'ethereum'),
    (gen_random_uuid(), 'SOL',  'Solana',   'solana'),
    (gen_random_uuid(), 'ADA',  'Cardano',  'cardano'),
    (gen_random_uuid(), 'USDT', 'Tether',   'tether');