package pl.commercelink.receipts.api.testing;

import pl.commercelink.provider.api.EventBinding;
import pl.commercelink.provider.api.EventBinding.WebhookBinding;
import pl.commercelink.provider.api.ProviderField;
import pl.commercelink.provider.api.WebhookContext;
import pl.commercelink.provider.api.WebhookOutcome;
import pl.commercelink.provider.api.WebhookStatusResponse;
import pl.commercelink.receipts.api.FiscalData;
import pl.commercelink.receipts.api.Receipt;
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

    /** Descriptor whose webhook body is "key|providerId|register|number|fiscalisedAt|url", signed with HMAC-SHA256. */
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
            String[] parts = payload.split("\\|", -1);
            Receipt receipt = Receipt.fiscalised(parts[0], parts[1],
                    new FiscalData(parts[2], parts[3], Instant.parse(parts[4])), parts[5]);
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
        FiscalData fiscal = receipt.fiscal();
        String payload = String.join("|", receipt.receiptKey(), receipt.providerReceiptId(),
                fiscal.cashRegisterUniqueNumber(), fiscal.receiptNumber(), fiscal.fiscalisedAt().toString(), receipt.documentUrl());
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
