# Changelog

## [1.1.3](https://github.com/freire19/Naviera_Eco/compare/api-v1.1.2...api-v1.1.3) (2026-04-25)


### Bug Fixes

* **api:** AUDIT_V1.3 closeout — timing, RBAC, race, locks, headers ([b795988](https://github.com/freire19/Naviera_Eco/commit/b79598899e26c2bc47d5c5a5734e3e85f23e58f7))
* **api:** AUDIT_V1.3 CRITs [#100](https://github.com/freire19/Naviera_Eco/issues/100)/[#114](https://github.com/freire19/Naviera_Eco/issues/114) + [#105](https://github.com/freire19/Naviera_Eco/issues/105) — super_admin + /rotas tenant ([e5c080d](https://github.com/freire19/Naviera_Eco/commit/e5c080d6c61d851595d1965898f979a8dd9109e0))
* **api:** DEEP_RESILIENCE ALTOs — retry, split, audit, correlationId ([01630dd](https://github.com/freire19/Naviera_Eco/commit/01630dd063be0bc86ac7fecf60d29c79dae413bf))
* **api:** DEEP_SECURITY hardening — headers, RBAC, WS revalidation, RL TOTP ([cd5044c](https://github.com/freire19/Naviera_Eco/commit/cd5044c2696afd0cf1416de7f9fafbd9a6d84f12))
* **api:** DEEP_SECURITY V5.0 closeout — IDOR, audit, role enum, slug ([bb312a0](https://github.com/freire19/Naviera_Eco/commit/bb312a0a621b4bf94a4f024b8ac2a5b089bc08d9))

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
