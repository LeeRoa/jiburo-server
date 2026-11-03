package com.jiburo.server.global.setting;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, SettingKey> {
}
