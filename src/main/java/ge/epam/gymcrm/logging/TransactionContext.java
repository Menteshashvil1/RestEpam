package ge.epam.gymcrm.logging;

import org.slf4j.MDC;

public final class TransactionContext {

    public static final String TRANSACTION_ID = "transactionId";
    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";

    private TransactionContext() {
    }

    public static String currentTransactionId() {
        return MDC.get(TRANSACTION_ID);
    }
}
