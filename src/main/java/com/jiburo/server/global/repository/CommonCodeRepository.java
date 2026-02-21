package com.jiburo.server.global.repository;

import com.jiburo.server.global.domain.CommonCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonCodeRepository extends JpaRepository<CommonCode, Long> {

    List<CommonCode> findAllByUseYnTrue();
}
