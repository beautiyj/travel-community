package com.gnagnoohc.travel.reservation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 예약/결제 API의 예외를 일정한 JSON 형식으로 변환한다.
 *
 * 기본 동작으로 두면 상태 검증 실패까지 HTTP 500 + 스택트레이스가 그대로 응답에 실려
 * 호출하는 쪽(관리자 페이지 등)이 쓰기 어렵고 내부 구조도 노출된다.
 * 응답은 { "result": "FAIL", "message": "..." } 로 통일한다.
 *
 * basePackages를 예약 파트로 한정했으므로 다른 파트의 응답에는 영향이 없다.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.gnagnoohc.travel.reservation")
public class ReservationExceptionHandler {

    /**
     * 상태가 맞지 않아 거부된 요청. (예: 취소요청이 아닌 예약에 승인 호출)
     * 서버 잘못이 아니라 요청이 잘못된 것이므로 400을 준다.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        log.warn("[요청 거부] {}", e.getMessage());
        return ResponseEntity.badRequest().body(fail(e.getMessage()));
    }

    /** 존재하지 않는 예약/결제를 지정한 경우 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException e) {
        log.warn("[대상 없음] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(fail(e.getMessage()));
    }

    /**
     * PG 통신 실패 등 예상치 못한 예외. 실제 서버 오류이므로 500을 유지한다.
     * 응답에는 일반 문구만 내보내되, 마지막 인자로 예외 객체를 넘겨
     * 스택트레이스 전체를 서버 로그(STS 콘솔)에 남긴다. 디버깅 정보는 잃지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("[처리 실패] {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(fail("처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    private Map<String, String> fail(String message) {
        return Map.of("result", "FAIL", "message", message == null ? "" : message);
    }
}
