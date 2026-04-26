# Changelog

## [1.0.2](https://github.com/freire19/Naviera_Eco/compare/app-v1.0.1...app-v1.0.2) (2026-04-26)


### Bug Fixes

* **app:** AUDIT_V1.3 closeout — hooks ordering, sanitizacao, defensive renders ([6a3b8cf](https://github.com/freire19/Naviera_Eco/commit/6a3b8cf7864fbde598ca8af90eea28bf9821df33))
* **app:** audit-front-deep — 7 criticos resolvidos (a11y + arquitetura) ([1007dbb](https://github.com/freire19/Naviera_Eco/commit/1007dbbdcc9605ade4657dcc19b69ba3462feade))
* **app:** DEEP_RESILIENCE ALTOs — heartbeat, unmount, double-submit, parse ([c8451d7](https://github.com/freire19/Naviera_Eco/commit/c8451d7cb6a7669135e098e2e547d9f7c8bc44b6))
* **app:** DEEP_RESILIENCE V6.0 MEDIO/BAIXO — schema validation + ref pattern ([2a2fbfa](https://github.com/freire19/Naviera_Eco/commit/2a2fbfabce8d9b2c539d9e3bc0b10204c801bb7a))
* **app:** DEEP_SECURITY mobile — sessionStorage, CSP, URL sanitization, 401-only logout ([c209b56](https://github.com/freire19/Naviera_Eco/commit/c209b56270639469aaebd0607c9359e0d81bdce2))


### Performance

* **app:** memoiza context values e unifica useApi com authFetch ([2a8009a](https://github.com/freire19/Naviera_Eco/commit/2a8009a4dde6f3f625fdb5aa04c7a814c9c8b686))


### Refactoring

* **app:** extract lerRespostaJson — DRY raw text + JSON.parse pattern ([ec89805](https://github.com/freire19/Naviera_Eco/commit/ec898055cbbc01ecc438c29f3c9b5de685a4af0b))
* **app:** extrai usePagamento hook + PagamentoSucesso (audit medios) ([7128196](https://github.com/freire19/Naviera_Eco/commit/7128196be7958151c3a5bcf7d1ddb1150339b9e9))
* **app:** split removerAmigo + fix import order (pos-/simplify) ([8352d01](https://github.com/freire19/Naviera_Eco/commit/8352d01334c4e31e463594e0f72a22f2ef5b3a2b))

## [1.0.1](https://github.com/freire19/Naviera_Eco/compare/app-v1.0.0...app-v1.0.1) (2026-04-25)


### Bug Fixes

* **app:** DEEP_BUGS FB4 — usar valorDevedor do servidor em FinanceiroCNPJ ([31b288b](https://github.com/freire19/Naviera_Eco/commit/31b288b71809efb825fba47e089416f8e9786fc4))
* **app:** DEEP_LOGIC — data local sv-SE, fallback id_viagem, grupo por empresa ([3535bcf](https://github.com/freire19/Naviera_Eco/commit/3535bcf43b404ee27f44247519d3ea28ea59afb6))
