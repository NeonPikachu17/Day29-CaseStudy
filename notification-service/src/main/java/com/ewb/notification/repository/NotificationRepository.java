package com.ewb.notification.repository;

import com.ewb.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByDeliveryId(String deliveryId);
    Optional<Notification> findByEventId(String eventId);
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<Notification> findByStandingOrderIdOrderByCreatedAtDesc(String standingOrderId);
}
