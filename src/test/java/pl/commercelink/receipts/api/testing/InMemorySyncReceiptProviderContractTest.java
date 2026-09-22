package pl.commercelink.receipts.api.testing;

import pl.commercelink.receipts.api.Receipt;
import pl.commercelink.receipts.api.ReceiptProvider;
import pl.commercelink.receipts.api.ReceiptState;

import java.util.OptionalInt;
import java.util.UUID;

class InMemorySyncReceiptProviderContractTest extends ReceiptProviderContractTest {

    private final InMemoryReceiptProvider provider = new InMemoryReceiptProvider(InMemoryReceiptProvider.Mode.SYNC);

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
        throw new AssertionError("A synchronous provider never returns PENDING");
    }

    @Override
    protected OptionalInt createCalls() {
        return OptionalInt.of(provider.createCalls());
    }
}
