package com.unibond.couple.dto;
import java.time.Instant;
import java.time.LocalDate;
public record CoupleResponse(Long id, Long partnerUserId, String partnerNickname,
    LocalDate anniversaryDate, Instant bindAt) {}
