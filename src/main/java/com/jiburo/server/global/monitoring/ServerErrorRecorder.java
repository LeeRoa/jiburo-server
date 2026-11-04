package com.jiburo.server.global.monitoring;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 예상하지 못한 서버 오류(500)를 메모리에 최근 N건 보관한다 (관리자 모니터링 화면용).
 * - 서버(인스턴스)마다 따로 쌓이고, 재시작하면 비워진다. 전체 이력은 서버 로그에서 확인.
 * - 메시지에 민감 정보가 섞일 수 있어 300자로 자르고 요청 본문은 남기지 않는다.
 */
@Component
public class ServerErrorRecorder {

    static final int MAX_ENTRIES = 50;
    private static final int MAX_MESSAGE = 300;

    public record ErrorEntry(LocalDateTime at, String method, String path, String exception, String message) {}

    private final Deque<ErrorEntry> recent = new ArrayDeque<>();
    private final AtomicLong totalSinceStart = new AtomicLong();

    public void record(String method, String path, Throwable e) {
        String message = e.getMessage() == null ? null
                : e.getMessage().length() > MAX_MESSAGE ? e.getMessage().substring(0, MAX_MESSAGE) + "…" : e.getMessage();
        ErrorEntry entry = new ErrorEntry(LocalDateTime.now(), method, path, e.getClass().getName(), message);
        totalSinceStart.incrementAndGet();
        synchronized (recent) {
            recent.addFirst(entry);
            while (recent.size() > MAX_ENTRIES) {
                recent.removeLast();
            }
        }
    }

    /** 최신순 */
    public List<ErrorEntry> recent() {
        synchronized (recent) {
            return new ArrayList<>(recent);
        }
    }

    public long totalSinceStart() {
        return totalSinceStart.get();
    }

    /** 최근 보관분 중 지정 시각 이후 건수 (보관 한도 50건 안에서만 정확) */
    public long countSince(LocalDateTime since) {
        synchronized (recent) {
            return recent.stream().filter(e -> e.at().isAfter(since)).count();
        }
    }
}
