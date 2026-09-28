package pl.commercelink.receipts.api.testing;

import pl.commercelink.receipts.api.Receipt;
import pl.commercelink.receipts.api.ReceiptProvider;
import pl.commercelink.receipts.api.ReceiptState;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

class InMemoryAsyncReceiptProviderContractTest extends ReceiptProviderContractTest {

    private final InMemoryReceiptProvider provider = new InMemoryReceiptProvider(InMemoryReceiptProvider.Mode.ASYNC, true);

    @Override
    protected ReceiptProvider provider() {
        return provider;
    }

    @Override
    protected String uniqueReceiptKey() {
        return UUID.randomUUID() + ":R1";
    }

    @Override
    protected void settle(Receipt pending, ReceiptState target) {
        provider.settle(pending.providerReceiptId(), target);
    }

    @Override
    protected Optional<ReceiptProvider> providerWithFailingTransport() {
        return Optional.of(new InMemoryReceiptProvider(InMemoryReceiptProvider.Mode.FAILING_TRANSPORT));
    }

    @Override
    protected Optional<ReceiptProvider> providerWithRejectingBackend() {
        return Optional.of(new InMemoryReceiptProvider(InMemoryReceiptProvider.Mode.REJECTING));
    }

    @Override
    protected Optional<ReceiptProvider> providerLosingNextResponse() {
        return Optional.of(provider.loseNextResponse());
    }

    @Override
    protected OptionalInt createCalls() {
        return OptionalInt.of(provider.createCalls());
    }

    @Override
    protected OptionalInt remoteCalls() {
        return OptionalInt.of(provider.remoteCalls());
    }
}
