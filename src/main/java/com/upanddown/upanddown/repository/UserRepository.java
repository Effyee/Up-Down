package com.upanddown.upanddown.repository;

import com.upanddown.upanddown.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 기본 CRUD 및 findAll 제공
}

