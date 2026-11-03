package com.jiburo.server.global.i18n;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface I18nMessageRepository extends JpaRepository<I18nMessage, Long> {

    Optional<I18nMessage> findByMessageKeyAndLocale(String messageKey, String locale);

    List<I18nMessage> findAllByMessageKey(String messageKey);
}
