package pl.commercelink.receipts.api.testing;

import pl.commercelink.receipts.api.FiscalData;
import pl.commercelink.receipts.api.Receipt;
import pl.commercelink.receipts.api.ReceiptException;
import pl.commercelink.receipts.api.ReceiptFailure;
import pl.commercelink.receipts.api.ReceiptKeys;
import pl.commercelink.receipts.api.ReceiptOutcomeUnknownException;
import pl.commercelink.receipts.api.ReceiptProvider;
import pl.commercelink.receipts.api.ReceiptRejectedException;
import pl.commercelink.receipts.api.ReceiptRequest;
import pl.commercelink.receipts.api.ReceiptState;
import pl.commercelink.receipts.api.ReceiptValidationException;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Reference provider proving the contract kit; the "backend" is a pair of maps. */
class InMemoryReceiptProvider implements ReceiptProvider {

    enum Mode { SYNC, ASYNC, FAILING_TRANSPORT, REJECTING }

    private final Mode mode;
    private final boolean requiresBuyerEmail;
    private final Map<String, Receipt> byProviderId = new ConcurrentHashMap<>();
    private final Map<String, String> providerIdByKey = new ConcurrentHashMap<>();
    private final AtomicInteger createCalls = new AtomicInteger();
    private final AtomicInteger remoteCalls = new AtomicInteger();
    private volatile boolean loseNextResponse;

    InMemoryReceiptProvider(Mode mode) {
        this(mode, false);
    }

    InMemoryReceiptProvider(Mode mode, boolean requiresBuyerEmail) {
        this.mode = mode;
        this.requiresBuyerEmail = requiresBuyerEmail;
    }

    @Override
    public boolean requiresBuyerEmail() {
        return requiresBuyerEmail;
    }

    /**
     * Arms the next {@code issue} to store the receipt on this same backend but report
     * {@link ReceiptOutcomeUnknownException}, as if the response were lost on the way back. Returns this
     * instance so callers can share it with {@link #provider()}.
     */
    InMemoryReceiptProvider loseNextResponse() {
        this.loseNextResponse = true;
        return this;
    }

    @Override
    public synchronized Receipt issue(ReceiptRequest request) {
        if (request == null) {
            throw new ReceiptValidationException("request is required");
        }
        if (requiresBuyerEmail && (request.buyer().email() == null || request.buyer().email().isBlank())) {
            throw new ReceiptValidationException("buyer email is required");
        }
        remoteCalls.incrementAndGet();
        String existing = providerIdByKey.get(request.receiptKey());
        if (existing != null) {
            return byProviderId.get(existing);
        }
        if (mode == Mode.REJECTING) {
            throw new ReceiptRejectedException("43", "Unknown point of sale");
        }
        String providerId = "mem-" + (createCalls.incrementAndGet()) + "-" + request.receiptKey();
        Receipt receipt = mode == Mode.SYNC ? fiscalised(request.receiptKey(), providerId)
                : Receipt.pending(request.receiptKey(), providerId);
        providerIdByKey.put(request.receiptKey(), providerId);
        byProviderId.put(providerId, receipt);
        if (mode == Mode.FAILING_TRANSPORT || loseNextResponse) {
            loseNextResponse = false;
            // The backend stored the receipt but the response was lost on the way back.
            throw new ReceiptOutcomeUnknownException("response lost",
                    new UncheckedIOException(new IOException("connection reset")));
        }
        return receipt;
    }

    @Override
    public Optional<Receipt> find(String receiptKey) {
        ReceiptKeys.requireValid(receiptKey);
        remoteCalls.incrementAndGet();
        return Optional.ofNullable(providerIdByKey.get(receiptKey)).map(byProviderId::get);
    }

    @Override
    public Receipt fetch(String providerReceiptId) {
        remoteCalls.incrementAndGet();
        Receipt receipt = byProviderId.get(providerReceiptId);
        if (receipt == null) {
            throw new ReceiptException("Unknown receipt " + providerReceiptId);
        }
        return receipt;
    }

    /** Simulates the fiscal device finishing a PENDING receipt. */
    void settle(String providerReceiptId, ReceiptState target) {
        Receipt current = byProviderId.get(providerReceiptId);
        Receipt settled = switch (target) {
            case FISCALISED -> fiscalised(current.receiptKey(), providerReceiptId);
            case FAILED -> Receipt.failed(current.receiptKey(), providerReceiptId, new ReceiptFailure("16", "Printer rejected the line name"));
            case PENDING -> current;
        };
        byProviderId.put(providerReceiptId, settled);
    }

    int createCalls() {
        return createCalls.get();
    }

    int remoteCalls() {
        return remoteCalls.get();
    }

    private static Receipt fiscalised(String receiptKey, String providerId) {
        return Receipt.fiscalised(receiptKey, providerId,
                new FiscalData("MEM0000001", providerId, Instant.parse("2026-09-22T10:00:00Z")),
                "https://receipts.example/view/" + providerId);
    }
}
