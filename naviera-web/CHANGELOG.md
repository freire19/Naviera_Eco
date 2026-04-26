# Changelog

## [1.7.6](https://github.com/freire19/Naviera_Eco/compare/web-v1.7.5...web-v1.7.6) (2026-04-26)


### Performance

* **web:** DEEP_PERFORMANCE V5.0 MEDIO/BAIXO — useMemo, batch POST, dir cache ([82af664](https://github.com/freire19/Naviera_Eco/commit/82af664c38b28b2f733bbdb4a73e8f5c592b4ccc))

## [1.7.5](https://github.com/freire19/Naviera_Eco/compare/web-v1.7.4...web-v1.7.5) (2026-04-25)


### Bug Fixes

* **web:** DEEP_RESILIENCE V6.0 MEDIO — pool, health, OCR tx, error endpoint ([41dbdf3](https://github.com/freire19/Naviera_Eco/commit/41dbdf31abbb032e9a6508b2ec1622c502a2e3b6))


### Performance

* **web:** DEEP_PERFORMANCE V5.0 ALTO — ListaFretes concurrency limit ([eebf11e](https://github.com/freire19/Naviera_Eco/commit/eebf11e7e18136db31f2134820bbec09a048e947))

## [1.7.4](https://github.com/freire19/Naviera_Eco/compare/web-v1.7.3...web-v1.7.4) (2026-04-25)


### Bug Fixes

* **web:** DEEP_RESILIENCE ALTOs — 429 retry, graceful shutdown ([99e32f8](https://github.com/freire19/Naviera_Eco/commit/99e32f82490b1089b29ac91b608f3e35697b8ecb))


### Refactoring

* **web:** drop globalThis shutdown hack — register handler after definition ([cafaa09](https://github.com/freire19/Naviera_Eco/commit/cafaa093300c7eca317a055cafd00560ecb8adec))

## [1.7.3](https://github.com/freire19/Naviera_Eco/compare/web-v1.7.2...web-v1.7.3) (2026-04-25)


### Bug Fixes

* **web:** DEEP_SECURITY V5.0 closeout — error redaction + LIKE limit ([512063e](https://github.com/freire19/Naviera_Eco/commit/512063e1865b4510de629b64e9c128915200c753))

## [1.7.2](https://github.com/freire19/Naviera_Eco/compare/web-v1.7.1...web-v1.7.2) (2026-04-25)


### Bug Fixes

* **web:** DEEP_SECURITY BFF — helmet, JWT alg, prompt sanitization, bounded caches ([7b6593a](https://github.com/freire19/Naviera_Eco/commit/7b6593a8ee28adeeed23e682f055233ae11f590e))

## [1.7.1](https://github.com/freire19/Naviera_Eco/compare/web-v1.7.0...web-v1.7.1) (2026-04-25)


### Bug Fixes

* **web:** AUDIT_V1.3 closeout — rate limit, tenant cache, tolerance PAGO ([910c827](https://github.com/freire19/Naviera_Eco/commit/910c827f82e23ca07ee281b34e8c83a42ce422ec))

## [1.7.0](https://github.com/freire19/Naviera_Eco/compare/web-v1.6.0...web-v1.7.0) (2026-04-25)


### Features

* **extrato-cliente:** comprovante de pagamento + impressao do extrato ([b7c29c3](https://github.com/freire19/Naviera_Eco/commit/b7c29c3c6cc53d4311178631b1727e146e59357f))

## [1.6.0](https://github.com/freire19/Naviera_Eco/compare/web-v1.5.0...web-v1.6.0) (2026-04-25)


### Features

* **web/financeiro:** Extrato de Cliente consolidado (frete + encomenda + passagem) ([8e7a2d4](https://github.com/freire19/Naviera_Eco/commit/8e7a2d4bfb26ba1c750b745f9d4c3968225934b6))


### Bug Fixes

* **web/extrato-cliente:** authMiddleware + busca por CONTEM + cadastros + agenda ([eacaac3](https://github.com/freire19/Naviera_Eco/commit/eacaac3f67efab8650852d8918fa922abbebae0c))
* **web/extrato-cliente:** coluna passageiros.nome_passageiro + carrega viagens ([bb14d1c](https://github.com/freire19/Naviera_Eco/commit/bb14d1cc9b90345d6a0c0b68ebc4de65702b0095))
* **web/extrato-cliente:** remove validacao de papel + selecao multipla ([b1760b4](https://github.com/freire19/Naviera_Eco/commit/b1760b42265f64964f02dc4abaa3059bcf59540f))
* **web:** AUDIT_V1.3 CRITs [#100](https://github.com/freire19/Naviera_Eco/issues/100)/[#114](https://github.com/freire19/Naviera_Eco/issues/114) [#102](https://github.com/freire19/Naviera_Eco/issues/102) [#106](https://github.com/freire19/Naviera_Eco/issues/106) [#108](https://github.com/freire19/Naviera_Eco/issues/108) [#650](https://github.com/freire19/Naviera_Eco/issues/650) — tenant isolation ([3343b52](https://github.com/freire19/Naviera_Eco/commit/3343b5279e6f5c6761de4f41b96857c36ddbf173))

## [1.5.0](https://github.com/freire19/Naviera_Eco/compare/web-v1.4.3...web-v1.5.0) (2026-04-24)


### Features

* **web/confere-viagem:** data da viagem no cabecalho + paginacao no rodape ([750efcc](https://github.com/freire19/Naviera_Eco/commit/750efcc6774f64a303b8a0709d9e0e596ec8533e))

## [1.4.3](https://github.com/freire19/Naviera_Eco/compare/web-v1.4.2...web-v1.4.3) (2026-04-24)


### Bug Fixes

* **web/confere-viagem:** assinatura dentro da caixa do destinatario ([370d905](https://github.com/freire19/Naviera_Eco/commit/370d905b56c55eb8248d9f4393036754494bf971))

## [1.4.2](https://github.com/freire19/Naviera_Eco/compare/web-v1.4.1...web-v1.4.2) (2026-04-24)


### Bug Fixes

* **web/confere-viagem:** uma assinatura por destinatario, sem total geral ([6be02ad](https://github.com/freire19/Naviera_Eco/commit/6be02adaeb679578e4a3cb549326652b9a1eddc8))

## [1.4.1](https://github.com/freire19/Naviera_Eco/compare/web-v1.4.0...web-v1.4.1) (2026-04-24)


### Bug Fixes

* **web/confere-viagem:** uma so assinatura no rodape, com espaco decente ([24eb762](https://github.com/freire19/Naviera_Eco/commit/24eb762349d8f0a55a63404125ab884441da7907))

## [1.4.0](https://github.com/freire19/Naviera_Eco/compare/web-v1.3.0...web-v1.4.0) (2026-04-24)


### Features

* **web/confere-viagem:** agrupa por destinatario, separa nota por nota ([d5c3ccc](https://github.com/freire19/Naviera_Eco/commit/d5c3cccba3e03331e456e09e68eed4751f3dabae))

## [1.3.0](https://github.com/freire19/Naviera_Eco/compare/web-v1.2.0...web-v1.3.0) (2026-04-24)


### Features

* **web/relatorio-fretes:** Confere Viagem dedicado + impressao P&B ([bfb93a6](https://github.com/freire19/Naviera_Eco/commit/bfb93a6d274e465a4199adb99ed74bdaa555fa33))

## [1.2.0](https://github.com/freire19/Naviera_Eco/compare/web-v1.1.1...web-v1.2.0) (2026-04-24)


### Features

* **web/agenda:** calendario com colunas iguais e visual modernizado ([9ba8a8f](https://github.com/freire19/Naviera_Eco/commit/9ba8a8f7024b6c49471919a3fb06a129a1f5fc3f))


### Bug Fixes

* **web/agenda:** calendario mais compacto, texto dos eventos legivel ([c7164df](https://github.com/freire19/Naviera_Eco/commit/c7164df6f218dec2d0df9bd8ece16b63b0382020))


### Refactoring

* **web/fretes:** remove secao Nota Fiscal de Fretes.jsx ([47b65dd](https://github.com/freire19/Naviera_Eco/commit/47b65dde8eee7d3a19b52485f5e27e1bade4c57a))

## [1.1.1](https://github.com/freire19/Naviera_Eco/compare/web-v1.1.0...web-v1.1.1) (2026-04-24)


### Bug Fixes

* **web:** DEEP_BUGS FB2 + FB3 — admin hosts whitelist, folha transacional, TZ BR ([da59630](https://github.com/freire19/Naviera_Eco/commit/da5963092a83329669ab7e05ee68428b8b951236))
* **web:** DEEP_LOGIC fixes — guards financeiro/viagens, JWT invalidation, OCR block ([6522207](https://github.com/freire19/Naviera_Eco/commit/6522207479e3893ef1ace5949cb10159ce87ce5a))

## [1.1.0](https://github.com/freire19/Naviera_Eco/compare/web-v1.0.0...web-v1.1.0) (2026-04-23)


### Features

* **recibo:** ajustes apos feedback — caixa alta, cidade+data negrito, fix filtro, modal reimpressao ([5c09ca4](https://github.com/freire19/Naviera_Eco/commit/5c09ca4c66625f713c77eb7f673ad5a24717809c))
* **web:** tela de Recibos Avulsos igual ao desktop ([2d4b30f](https://github.com/freire19/Naviera_Eco/commit/2d4b30f94df0344607d5e6f6745f5acffed1721c))


### Bug Fixes

* **ocr:** Gemini agora extrai itens de NFC-e/cupom fiscal tambem ([19a473d](https://github.com/freire19/Naviera_Eco/commit/19a473d79609652970dfcc4ca79cd33ee5c5025e))
* **recibo:** espacamento entre data/assinatura na termica + cidade vazia sem virgula ([c43a5db](https://github.com/freire19/Naviera_Eco/commit/c43a5dbdfcb772b2a99c5c9df5ee320037a146fd))
* **web:** adiciona claim tipo=OPERADOR no JWT do BFF ([080d370](https://github.com/freire19/Naviera_Eco/commit/080d370f07219c1e7a08c4272120df8d14a781bf))
* **web:** adiciona import randomUUID em routes/ocr.js ([c73e195](https://github.com/freire19/Naviera_Eco/commit/c73e195a784c98b3ae52e9e87287770cc8c0bb10))
* **web:** migra estorno para /api/estornos com audit correto ([5895321](https://github.com/freire19/Naviera_Eco/commit/5895321779d11a33f1b23489e65e80bf2916f755))


### Performance

* **web:** dashboard financeiro agrega no Postgres em vez de JS ([06f2460](https://github.com/freire19/Naviera_Eco/commit/06f2460a53e22183770be89ce19cccfca840eaeb))


### Refactoring

* simplify follow-ups — push-down de categoria, ApiException, slf4j ([66018c2](https://github.com/freire19/Naviera_Eco/commit/66018c263bda84ce9ab7764063e56e25d1d1df57))
