package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.OAuthAttributes;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 소셜 로그인 사용자 조회·가입 (카카오 앱 로그인, 웹 OAuth2 로그인 공통).
 *
 * 같은 계정으로 첫 로그인이 동시에 들어오면 둘 다 "없음"으로 보고 가입을 시도한다. oauth_id 유니크 제약에 걸린 쪽은
 * 이미 가입된 사용자를 다시 조회해 정상 로그인시켜야 하는데, 이를 위해 다음을 지킨다.
 * - 가입은 saveAndFlush로 즉시 INSERT한다. User PK가 애플리케이션 생성 UUID라 save()만 하면 INSERT가 커밋 때로
 *   미뤄져, 제약 위반을 이 자리에서 잡을 수 없다.
 * - 조회·가입은 호출자와 분리된 새 트랜잭션(REQUIRES_NEW)에서 한다. 제약 위반이 난 트랜잭션은 재사용할 수 없고,
 *   MariaDB 기본 격리 수준(REPEATABLE READ)에서는 같은 트랜잭션 안에서 다시 조회해도 상대가 넣은 행이 보이지 않는다.
 *   → 충돌하면 그 트랜잭션은 버리고 새 트랜잭션에서 다시 조회한다.
 *
 * 반환되는 User는 트랜잭션이 끝난 detached 상태다 (필드 값만 읽고, 변경은 각 서비스가 merge/조회 후 처리).
 */
@Slf4j
@Service
public class OAuthUserService {

    private final UserRepository userRepository;
    private final TransactionTemplate newTransaction;

    public OAuthUserService(UserRepository userRepository, PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 있으면 프로필(닉네임·사진)을 갱신해 반환, 없으면 가입시킨다 */
    public User findOrCreate(OAuthAttributes attributes) {
        try {
            return newTransaction.execute(status -> userRepository.findByOauthId(attributes.oauthId())
                    .map(user -> user.updateProfile(attributes.nickname(), attributes.profileImageUrl()))
                    .orElseGet(() -> userRepository.saveAndFlush(attributes.toEntity())));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 계정이 먼저 가입됨 → 새 트랜잭션에서 그 사용자를 조회해 로그인 처리
            log.info("동시 첫 로그인 감지, 기존 가입자로 처리: oauthId={}", attributes.oauthId());
            return newTransaction.execute(status -> userRepository.findByOauthId(attributes.oauthId())
                    .orElseThrow(() -> new JiburoException(ErrorCode.OAUTH_PROVIDER_FAILED)));
        }
    }
}
