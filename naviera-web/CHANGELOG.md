# Changelog

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
