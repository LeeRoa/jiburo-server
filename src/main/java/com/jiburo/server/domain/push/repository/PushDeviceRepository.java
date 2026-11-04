package com.jiburo.server.domain.push.repository;

import com.jiburo.server.domain.push.domain.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByToken(String token);

    @Query("select d from PushDevice d where d.user.id in :userIds")
    List<PushDevice> findAllByUserIds(@Param("userIds") Collection<UUID> userIds);

    long countByUser_Id(UUID userId);

    // 비동기 발송 스레드(트랜잭션 없음)에서도 바로 지울 수 있도록 자체 트랜잭션
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query("delete from PushDevice d where d.token in :tokens")
    int deleteByTokens(@Param("tokens") Collection<String> tokens);

    @Modifying
    @Query("delete from PushDevice d where d.token = :token and d.user.id = :userId")
    int deleteByTokenAndUserId(@Param("token") String token, @Param("userId") UUID userId);
}
