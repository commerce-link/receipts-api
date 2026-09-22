# Receipts API

Contract for issuing Polish **fiscal e-receipts** (e-paragony) through external providers
(Fakturownia/paragony.pl, eparagony.pl, fiscal device bridges). A receipt becomes fiscal only when an
online fiscal device registers it; providers accept the request and report the outcome later.

## Contract

- **`ReceiptProviderDescriptor`** — service-loaded entry point (`META-INF/services/pl.commercelink.receipts.api.ReceiptProviderDescriptor`), extends `ProviderDescriptor<ReceiptProvider>`.
- **`ReceiptProvider`** — `issue(request)`, `find(receiptKey)` (read-only probe), `fetch(providerReceiptId)`, capability defaults `supportedMedia()`, `maxLineNameLength()`, `requiresBuyerEmail()`, `pushesStatusUpdates()`.
- **`ReceiptRequest`** — built with `ReceiptRequest.builder()`; `build()` validates (key format, lines, totals = payments to the grosz).
- **`Receipt`** — `PENDING → FISCALISED | FAILED`; `FiscalData` (unique cash register number, receipt number, time), `documentUrl` (e-receipt link, may arrive later), `ReceiptFailure`.
- **`Money`** (grosze, PLN), **`VatRate`** (adapters map to PTU letters), **`PaymentForm`**, **`LineKind`**, **`ReceiptLineNames.normalize`** (Windows-1250, device length).

## Idempotency and failures

The receipt key identifies one issuing attempt and is sent to the provider as its idempotency key.

| Exception | Meaning | Consumer action |
|---|---|---|
| `ReceiptValidationException` | invalid input, nothing sent | fix the data |
| `ReceiptRejectedException` | provider refused, nothing created | fix the cause, issue with a NEW key |
| `ReceiptOutcomeUnknownException` | may have been created | `find(key)`; if empty retry with the SAME key |
| `ReceiptException` | failed before sending | retry with the SAME key |

Catch the subclasses before `ReceiptException`. Never retry an unknown outcome with a new key — that
registers the sale twice.

## Evolving enums

`LineKind`, `PaymentForm`, `ReceiptMedium` and `VatRate` may gain constants in later releases. An adapter
that does not map a constant it receives must refuse the request with `ReceiptValidationException` before
any remote call, rather than guessing or silently dropping data. Future discount lines, for example, will
be gated behind a capability so adapters that do not support them keep refusing safely.

## Status updates

Providers that push status declare a `WebhookBinding` whose `WebhookExecutor<Receipt>` authenticates the
call and returns the receipt, or `WebhookStatusResponse("REJECTED")`. Consumers still poll `PENDING`
receipts with `fetch`, because some providers do not retry webhooks.

## Contract tests

Adapters extend the kits shipped in `pl.commercelink.receipts.api.testing` (JUnit 5 is `provided`):

```java
class MyProviderContractTest extends ReceiptProviderContractTest {
    protected ReceiptProvider provider() { ... }                       // bound to a fake backend
    protected String uniqueReceiptKey() { return UUID.randomUUID() + ":R1"; }
    protected void settle(Receipt pending, ReceiptState target) { ... } // flip state on the fake backend
}
```

`ReceiptWebhookContractTest` covers providers with `pushesStatusUpdates()`.
