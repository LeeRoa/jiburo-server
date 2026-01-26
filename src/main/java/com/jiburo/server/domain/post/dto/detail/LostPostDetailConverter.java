package com.jiburo.server.domain.post.dto.detail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiburo.server.global.error.BusinessException;
import com.jiburo.server.global.error.ErrorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
@Converter
public class LostPostDetailConverter implements AttributeConverter<TargetDetailDto, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(TargetDetailDto attribute) {
        // 객체 -> DB (JSON 문자열)
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("JSON writing error", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public TargetDetailDto convertToEntityAttribute(String dbData) {
        // DB (JSON 문자열) -> 객체
        if (!StringUtils.hasText(dbData)) {
            return null;
        }
        try {
            // @JsonTypeInfo 설정 덕분에 "type" 필드를 보고 알아서 AnimalDetailDto 등으로 변환됨
            return objectMapper.readValue(dbData, TargetDetailDto.class);
        } catch (JsonProcessingException e) {
            log.error("JSON reading error", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}