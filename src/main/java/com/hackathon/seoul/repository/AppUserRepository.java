package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}
