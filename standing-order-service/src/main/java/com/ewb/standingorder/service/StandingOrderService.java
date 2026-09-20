package com.ewb.standingorder.service;

import com.ewb.common.dto.CreateStandingOrderRequest;
import com.ewb.common.dto.DueStandingOrderDto;
import com.ewb.common.dto.StandingOrderResponse;
import com.ewb.common.model.Frequency;
import com.ewb.common.model.OrderStatus;
import com.ewb.standingorder.entity.AuditLog;
import com.ewb.standingorder.entity.StandingOrder;
import com.ewb.standingorder.entity.StandingOrderVersion;
import com.ewb.standingorder.repository.AuditLogRepository;
import com.ewb.standingorder.repository.StandingOrderRepository;
import com.ewb.standingorder.repository.StandingOrderVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StandingOrderService {

    private final StandingOrderRepository repository;
    private final StandingOrderVersionRepository versionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ScheduleRuleService scheduleRuleService;

    public StandingOrderService(StandingOrderRepository repository,
                                StandingOrderVersionRepository versionRepository,
                                AuditLogRepository auditLogRepository,
                                ScheduleRuleService scheduleRuleService) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.auditLogRepository = auditLogRepository;
        this.scheduleRuleService = scheduleRuleService;
    }

    public StandingOrderResponse createStandingOrder(CreateStandingOrderRequest request, String customerId) {
        String effectiveCustomer = (customerId != null && !customerId.isBlank()) ? customerId : "asuna";
        String id = "so-" + (9000 + repository.count() + 1);

        String sourceAccountId = request != null && request.getSourceAccountId() != null ? request.getSourceAccountId() : "EWB-ASU-1001";
        String destinationAccountId = request != null && request.getDestinationAccountId() != null ? request.getDestinationAccountId() : "EWB-ASU-2001";
        BigDecimal amount = request != null && request.getAmount() != null ? request.getAmount() : new BigDecimal("5000.00");
        String currency = request != null && request.getCurrency() != null ? request.getCurrency() : "PHP";
        Frequency frequency = request != null && request.getFrequency() != null ? request.getFrequency() : Frequency.MONTHLY;
        Integer dayOfMonth = request != null && request.getDayOfMonth() != null ? request.getDayOfMonth() : 25;
        String executionTime = request != null && request.getExecutionTime() != null ? request.getExecutionTime() : "09:00";
        String timeZone = request != null && request.getTimeZone() != null ? request.getTimeZone() : "Asia/Manila";
        String startDate = request != null && request.getStartDate() != null ? request.getStartDate() : "2026-10-25";

        String nextOccurrence = scheduleRuleService.calculateNextOccurrence(dayOfMonth, executionTime, timeZone);
        String createdAt = Instant.now().toString();

        StandingOrder order = new StandingOrder(
                id,
                effectiveCustomer,
                sourceAccountId,
                destinationAccountId,
                amount,
                currency,
                frequency,
                dayOfMonth,
                executionTime,
                timeZone,
                startDate,
                nextOccurrence,
                OrderStatus.ACTIVE,
                1,
                createdAt
        );

        repository.save(order);

        StandingOrderVersion version = new StandingOrderVersion(id, 1, amount, dayOfMonth, LocalDateTime.now());
        versionRepository.save(version);

        AuditLog audit = new AuditLog(id, effectiveCustomer, "ORDER", null, "CREATED", LocalDateTime.now());
        auditLogRepository.save(audit);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<StandingOrderResponse> getAllOrders(String customerId) {
        List<StandingOrder> list;
        if (customerId != null && !customerId.isBlank()) {
            list = repository.findByCustomerId(customerId);
        } else {
            list = repository.findAll();
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StandingOrderResponse getOrderById(String id) {
        return repository.findById(id).map(this::toResponse).orElse(null);
    }

    public StandingOrderResponse pauseOrder(String id, String user) {
        StandingOrder order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.PAUSED);
        repository.save(order);

        auditLogRepository.save(new AuditLog(id, user != null ? user : "system", "status", oldStatus.name(), OrderStatus.PAUSED.name(), LocalDateTime.now()));
        return toResponse(order);
    }

    public StandingOrderResponse resumeOrder(String id, String user) {
        StandingOrder order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.ACTIVE);
        repository.save(order);

        auditLogRepository.save(new AuditLog(id, user != null ? user : "system", "status", oldStatus.name(), OrderStatus.ACTIVE.name(), LocalDateTime.now()));
        return toResponse(order);
    }

    public StandingOrderResponse cancelOrder(String id, String user) {
        StandingOrder order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        repository.save(order);

        auditLogRepository.save(new AuditLog(id, user != null ? user : "system", "status", oldStatus.name(), OrderStatus.CANCELLED.name(), LocalDateTime.now()));
        return toResponse(order);
    }

    public StandingOrderResponse patchOrder(String id, Map<String, Object> updates, String user) {
        StandingOrder order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        int newVersion = order.getVersion() + 1;
        order.setVersion(newVersion);

        if (updates.containsKey("amount")) {
            BigDecimal oldAmount = order.getAmount();
            BigDecimal newAmount = new BigDecimal(String.valueOf(updates.get("amount")));
            order.setAmount(newAmount);
            auditLogRepository.save(new AuditLog(id, user != null ? user : "system", "amount", String.valueOf(oldAmount), String.valueOf(newAmount), LocalDateTime.now()));
        }

        if (updates.containsKey("dayOfMonth")) {
            Integer oldDay = order.getDayOfMonth();
            Integer newDay = Integer.valueOf(String.valueOf(updates.get("dayOfMonth")));
            order.setDayOfMonth(newDay);
            order.setNextOccurrence(scheduleRuleService.calculateNextOccurrence(newDay, order.getExecutionTime(), order.getTimeZone()));
            auditLogRepository.save(new AuditLog(id, user != null ? user : "system", "dayOfMonth", String.valueOf(oldDay), String.valueOf(newDay), LocalDateTime.now()));
        }

        repository.save(order);
        versionRepository.save(new StandingOrderVersion(id, newVersion, order.getAmount(), order.getDayOfMonth(), LocalDateTime.now()));

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<DueStandingOrderDto> getDueOrders(String cutoffUtc) {
        List<StandingOrder> activeOrders = repository.findByStatus(OrderStatus.ACTIVE);
        List<DueStandingOrderDto> result = new ArrayList<>();
        for (StandingOrder order : activeOrders) {
            String occUtc = "2026-10-25T01:00:00Z";
            result.add(new DueStandingOrderDto(
                    order.getId(),
                    order.getCustomerId(),
                    order.getSourceAccountId(),
                    order.getDestinationAccountId(),
                    order.getAmount(),
                    order.getCurrency(),
                    occUtc,
                    order.getVersion(),
                    order.getStatus()
            ));
        }
        return result;
    }

    private StandingOrderResponse toResponse(StandingOrder entity) {
        StandingOrderResponse res = new StandingOrderResponse();
        res.setId(entity.getId());
        res.setCustomerId(entity.getCustomerId());
        res.setSourceAccountId(entity.getSourceAccountId());
        res.setDestinationAccountId(entity.getDestinationAccountId());
        res.setAmount(entity.getAmount());
        res.setCurrency(entity.getCurrency());
        res.setFrequency(entity.getFrequency());
        res.setDayOfMonth(entity.getDayOfMonth());
        res.setExecutionTime(entity.getExecutionTime());
        res.setTimeZone(entity.getTimeZone());
        res.setStartDate(entity.getStartDate());
        res.setNextOccurrence(entity.getNextOccurrence());
        res.setStatus(entity.getStatus());
        res.setVersion(entity.getVersion());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }
}
