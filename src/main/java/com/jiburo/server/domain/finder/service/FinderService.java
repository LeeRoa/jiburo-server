package com.jiburo.server.domain.finder.service;

import com.jiburo.server.domain.finder.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface FinderService {

    String createSession(UUID finderId, FinderSessionCreateRequestDto dto);

    FinderSessionResponseDto getSession(UUID finderId, Long sessionId);

    Slice<FinderSessionListResponseDto> getMySessions(UUID finderId, Pageable pageable);

    List<FinderMediaResponseDto> addMedia(UUID finderId, Long sessionId, FinderMediaAddRequestDto dto);

    void deleteMedia(UUID finderId, Long sessionId, Long mediaId);

    void submitSession(UUID finderId, Long sessionId, FinderSessionSubmitRequestDto dto);

    void cancelSession(UUID finderId, Long sessionId);

    Slice<FinderSessionResponseDto> getSessionsByPost(UUID postOwnerId, Long postId, Pageable pageable);

    void recordRoute(UUID finderId, Long sessionId, FinderRouteRecordRequestDto dto);

    List<FinderRoutePointResponseDto> getRoute(UUID finderId, Long sessionId);
}
