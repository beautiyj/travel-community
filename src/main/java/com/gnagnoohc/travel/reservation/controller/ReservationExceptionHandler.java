package com.gnagnoohc.travel.reservation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * 예약/결제의 예외를 요청 종류에 맞는 형태로 변환한다.
 *
 * 기본 동작으로 두면 상태 검증 실패까지 HTTP 500 + 스택트레이스가 그대로 응답에 실려
 * 호출하는 쪽(관리자 페이지 등)이 쓰기 어렵고 내부 구조도 노출된다.
 *
 * API 요청은 { "result": "FAIL", "message": "..." } 로 통일하고,
 * 화면 요청은 실패 페이지를 보여준다. 둘을 구분하지 않으면 폼 제출 실패 시
 * 브라우저에 JSON 날것이 그대로 뜬다.
 *
 * basePackages를 예약 파트로 한정했으므로 다른 파트의 응답에는 영향이 없다.
 */
@Slf4j
@ControllerAdvice(basePackages = "com.gnagnoohc.travel.reservation")
public class ReservationExceptionHandler {

    /**
     * 상태가 맞지 않아 거부된 요청. (예: 취소요청이 아닌 예약에 승인 호출)
     * 서버 잘못이 아니라 요청이 잘못된 것이므로 400을 준다.
     */
    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException e, HandlerMethod handler) {
        log.warn("[요청 거부] {}", e.getMessage());
        return respond(handler, HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** 존재하지 않는 예약/결제를 지정한 경우 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleNotFound(IllegalArgumentException e, HandlerMethod handler) {
        log.warn("[대상 없음] {}", e.getMessage());
        return respond(handler, HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * PG 통신 실패 등 예상치 못한 예외. 실제 서버 오류이므로 500을 유지한다.
     * 응답에는 일반 문구만 내보내되, 마지막 인자로 예외 객체를 넘겨
     * 스택트레이스 전체를 서버 로그(STS 콘솔)에 남긴다. 디버깅 정보는 잃지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception e, HandlerMethod handler) {
        log.error("[처리 실패] {}", e.getMessage(), e);
        return respond(handler, HttpStatus.INTERNAL_SERVER_ERROR,
                "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    /**
     * API 요청이면 JSON, 화면 요청이면 실패 페이지로 응답한다.
     *
     * Accept 헤더가 아니라 실제로 호출된 컨트롤러 메서드에 @ResponseBody가 붙었는지로 판단한다.
     * fetch()는 기본적으로 Accept: 별표/별표 를 보내 헤더만으로는 폼 제출과 구분되지 않기 때문이다.
     */
    private Object respond(HandlerMethod handler, HttpStatus status, String message) {
        if (handler.hasMethodAnnotation(ResponseBody.class)) {
            return ResponseEntity.status(status).body(fail(message));
        }
        ModelAndView mv = new ModelAndView("reservation/fail");
        mv.addObject("message", message);
        mv.setStatus(status);
        return mv;
    }

    private Map<String, String> fail(String message) {
        return Map.of("result", "FAIL", "message", message == null ? "" : message);
    }
}
