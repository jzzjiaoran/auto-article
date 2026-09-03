package com.autoarticle.repository;

import com.autoarticle.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {

    List<PlatformAccount> findByPlatform(String platform);

    List<PlatformAccount> findByStatus(String status);

    List<PlatformAccount> findByEnabledTrue();
}
