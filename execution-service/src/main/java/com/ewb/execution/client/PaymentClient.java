package com.ewb.execution.client;

import com.ewb.common.dto.TransferRequest;
import com.ewb.common.dto.TransferResponse;

public interface PaymentClient {
    TransferResponse submitTransfer(TransferRequest request, String idempotencyKey);
    TransferResponse getTransferByReference(String reference);
}
