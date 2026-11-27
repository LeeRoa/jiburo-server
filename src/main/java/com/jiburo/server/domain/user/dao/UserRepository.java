package com.jiburo.server.domain.user.dao;

import com.jiburo.server.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // 소셜 로그인 ID로 유저를 찾는 메서드
    Optional<User> findByOauthId(String oauthId);

    Optional<User> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByNicknameAndIsDeletedFalse(String nickname);

    // 관리자 역할 보유 계정 (운영자 푸시 수신 대상 후보)
    java.util.List<User> findAllByRoleCode(com.jiburo.server.domain.user.domain.enums.RoleType roleCode);
}