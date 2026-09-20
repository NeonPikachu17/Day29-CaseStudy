package com.ewb.execution.client;

import com.ewb.common.dto.DueStandingOrderDto;
import com.ewb.common.dto.StandingOrderResponse;

import java.util.List;

public interface StandingOrderClient {
    List<DueStandingOrderDto> getDueOrders(String cutoffUtc);
    StandingOrderResponse getStandingOrder(String standingOrderId);
}
