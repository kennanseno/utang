package com.utang.repository;

import com.utang.domain.ReminderLog;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    boolean existsByCustomerIdAndSentOn(Long customerId, LocalDate sentOn);
}
