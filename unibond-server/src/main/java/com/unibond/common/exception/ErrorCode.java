package com.unibond.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    AUTH_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "验证码已过期"),
    AUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "验证码错误"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token已过期"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Token无效"),
    COUPLE_ALREADY_BOUND(HttpStatus.CONFLICT, "已绑定情侣"),
    COUPLE_NOT_BOUND(HttpStatus.BAD_REQUEST, "未绑定情侣"),
    INVITE_CODE_INVALID(HttpStatus.BAD_REQUEST, "邀请码无效"),
    INVITE_CODE_SELF(HttpStatus.BAD_REQUEST, "不能绑定自己"),
    QUIZ_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "今日题目尚未生成"),
    QUIZ_ALREADY_ANSWERED(HttpStatus.CONFLICT, "已回答过"),
    QUIZ_NOT_REVEALED(HttpStatus.BAD_REQUEST, "结果未揭晓"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "请求频率超限"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "用户不存在"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "无权限访问");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
