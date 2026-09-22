package pl.commercelink.receipts.api.testing;

import pl.commercelink.provider.api.EventBinding;
import pl.commercelink.provider.api.EventBinding.WebhookBinding;
import pl.commercelink.provider.api.ProviderField;
import pl.commercelink.provider.api.WebhookContext;
import pl.commercelink.provider.api.WebhookOutcome;
import pl.commercelink.provider.api.WebhookStatusResponse;
import pl.commercelink.receipts.api.FiscalData;
import pl.commercelink.receipts.api.Receipt;
import pl.commercelink.receipts.api.ReceiptFailure;
import pl.commercelink.receipts.api.ReceiptProvider;
import pl.commercelink.receipts.api.ReceiptProviderDescriptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryReceiptWebhookContractTest extends ReceiptWebhookContractTest {

    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final Map<String, String> CONFIG = Map.of("webhookSecret", "s3cret");

    /**
     * Descriptor whose webhook body is signed with HMAC-SHA256 and starts with a state field:
     * {@code FISCALISED|key|providerId|register|number|fiscalisedAt|url},
     * {@code FAILED|key|providerId|code|message}, or the literal {@code IGNORE} for an event about
     * something that is not one of this store's receipts.
     */
    static class Descriptor implements ReceiptProviderDescriptor {

        @Override
        public String name() {
            return "in-memory";
        }

        @Override
        public String displayName() {
            return "In-memory";
        }

        @Override
        public List<ProviderField> configurationFields() {
            return List.of(new ProviderField("webhookSecret", "Webhook secret", ProviderField.FieldType.PASSWORD, true, null));
        }

        @Override
        public ReceiptProvider create(Map<String, String> configuration) {
            return new InMemoryReceiptProvider(InMemoryReceiptProvider.Mode.ASYNC) {
                @Override
                public boolean pushesStatusUpdates() {
                    return true;
                }
            };
        }

        @Override
        public List<EventBinding<?>> bindings() {
            return List.of(new WebhookBinding<Receipt>("in-memory", Descriptor::handle));
        }

        private static WebhookOutcome<Receipt> handle(String payload, WebhookContext context) {
            String signature = context.header(SIGNATURE_HEADER);
            String expected = hmac(payload, context.providerConfig().get("webhookSecret"));
            if (signature == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                return WebhookOutcome.of(null, new WebhookStatusResponse(REJECTED));
            }
            if ("IGNORE".equals(payload)) {
                // An authentic call about a document that is not one of this store's receipts.
                return WebhookOutcome.empty();
            }
            String[] parts = payload.split("\\|", -1);
            Receipt receipt = switch (parts[0]) {
                case "FISCALISED" -> Receipt.fiscalised(parts[1], parts[2],
                        new FiscalData(parts[3], parts[4], Instant.parse(parts[5])), parts[6]);
                case "FAILED" -> Receipt.failed(parts[1], parts[2],
                        new ReceiptFailure(parts[3].isEmpty() ? null : parts[3], parts[4]));
                default -> throw new IllegalArgumentException("Unknown webhook state: " + parts[0]);
            };
            return WebhookOutcome.of(receipt, new WebhookStatusResponse("OK"));
        }
    }

    @Override
    protected ReceiptProviderDescriptor descriptor() {
        return new Descriptor();
    }

    @Override
    protected Map<String, String> providerConfig() {
        return CONFIG;
    }

    @Override
    protected SignedWebhook validWebhook(Receipt receipt) {
        String payload = switch (receipt.state()) {
            case FISCALISED -> {
                FiscalData fiscal = receipt.fiscal();
                yield String.join("|", "FISCALISED", receipt.receiptKey(), receipt.providerReceiptId(),
                        fiscal.cashRegisterUniqueNumber(), fiscal.receiptNumber(), fiscal.fiscalisedAt().toString(),
                        receipt.documentUrl());
            }
            case FAILED -> {
                ReceiptFailure failure = receipt.failure();
                yield String.join("|", "FAILED", receipt.receiptKey(), receipt.providerReceiptId(),
                        failure.code() == null ? "" : failure.code(), failure.message());
            }
            case PENDING -> throw new IllegalArgumentException("This fixture has no webhook payload for PENDING");
        };
        return new SignedWebhook(payload, Map.of(SIGNATURE_HEADER, hmac(payload, CONFIG.get("webhookSecret"))));
    }

    @Override
    protected SignedWebhook unauthenticated(SignedWebhook valid) {
        return new SignedWebhook(valid.payload(), Map.of());
    }

    @Override
    protected Optional<SignedWebhook> tampered(SignedWebhook valid) {
        return Optional.of(new SignedWebhook(valid.payload().replace("000001", "000002"), valid.headers()));
    }

    @Override
    protected Optional<SignedWebhook> irrelevantWebhook() {
        String payload = "IGNORE";
        return Optional.of(new SignedWebhook(payload, Map.of(SIGNATURE_HEADER, hmac(payload, CONFIG.get("webhookSecret")))));
    }

    private static String hmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
