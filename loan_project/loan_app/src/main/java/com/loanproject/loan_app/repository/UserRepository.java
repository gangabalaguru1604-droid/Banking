
        package com.loanproject.loan_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.loanproject.loan_app.bean.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmailAndPassword(String email, String password);

    boolean existsByEmail(String email);
}
