package com.loanproject.loan_app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserNotificationPreference;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    Optional<UserNotificationPreference> findByUser(User user);

    Optional<UserNotificationPreference> findByUser_Id(Long userId);

    void deleteByUser(User user);
}
