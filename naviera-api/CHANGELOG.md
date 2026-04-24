# Changelog

## [1.1.2](https://github.com/freire19/Naviera_Eco/compare/api-v1.1.1...api-v1.1.2) (2026-04-24)


### Bug Fixes

* **api:** DEEP_BUGS FB1 + FB4 — HMAC constant-time, bilhete advisory_lock, TZ BR ([f21799e](https://github.com/freire19/Naviera_Eco/commit/f21799ec021cb408efc3f043db359e9e65514c9d))

## [1.1.1](https://github.com/freire19/Naviera_Eco/compare/api-v1.1.0...api-v1.1.1) (2026-04-24)


### Bug Fixes

* **api:** DEEP_LOGIC massive fixes — cargas, guards, cifra totp, PSP off-TX ([63f47dd](https://github.com/freire19/Naviera_Eco/commit/63f47ddb1d32086f60e25ba2e61efad0d2b4ec31))

## [1.1.0](https://github.com/freire19/Naviera_Eco/compare/api-v1.0.0...api-v1.1.0) (2026-04-23)


### Features

* **api:** webhook Asaas com idempotencia e propagacao de status ([ed21783](https://github.com/freire19/Naviera_Eco/commit/ed21783614a46be578c84802649391df597cb973))


### Bug Fixes

* **api:** bloqueia fallback de ownership com nome vazio em pagar() ([45ccf60](https://github.com/freire19/Naviera_Eco/commit/45ccf605ffcbdd3cce926e2b694bad88d0046a64))
* **api:** corrige schema divergente em cadastros e passagens ([aa9a2df](https://github.com/freire19/Naviera_Eco/commit/aa9a2df8a1db2e0214cf37aca7a9c9ff392d5608))
* **api:** hardening de resilience — Asaas timeouts, Firebase fail-fast, tini ([b4d11d0](https://github.com/freire19/Naviera_Eco/commit/b4d11d0289db3372fcc3cda3fd47f2f15a9bffac))


### Refactoring

* **api:** move chamada PSP Asaas para fora de @Transactional ([f7804a0](https://github.com/freire19/Naviera_Eco/commit/f7804a0c9603d022569235789c1d356b2363fbb9))
* simplify follow-ups — push-down de categoria, ApiException, slf4j ([66018c2](https://github.com/freire19/Naviera_Eco/commit/66018c263bda84ce9ab7764063e56e25d1d1df57))
