package com.gnagnoohc.travel.reservation.mapper;

import com.gnagnoohc.travel.reservation.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMapper {
    void insert(Payment payment);
    Payment findById(Long paymentId);
    Payment findByOrderId(String orderId);
    List<Payment> findByReservationId(Long reservationId);
    void updateStatus(@Param("paymentId") Long paymentId, @Param("status") String status);
}
