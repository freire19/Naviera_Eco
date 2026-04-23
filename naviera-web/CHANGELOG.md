# Changelog

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
