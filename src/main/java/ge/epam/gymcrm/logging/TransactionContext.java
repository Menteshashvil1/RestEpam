package ge.epam.gymcrm.logging;

import org.slf4j.MDC;

/** Access point for the transaction id of the call currently being handled. */
public final class TransactionContext {

    public static final String TRANSACTION_ID = "transactionId";
    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";

    private TransactionContext() {
    }

    /** The transaction id put into the MDC by the transaction logging filter. */
    public static String currentTransactionId() {
        return MDC.get(TRANSACTION_ID);
    }
}
