package com.unibond.stats.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.security.UserPrincipal;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.service.CoupleService;
import com.unibond.stats.dto.*;
import com.unibond.stats.service.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {
    private final StatsService statsService;
    private final CoupleService coupleService;

    public StatsController(StatsService statsService, CoupleService coupleService) {
        this.statsService = statsService;
        this.coupleService = coupleService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> overview(
            @AuthenticationPrincipal UserPrincipal principal) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        return ApiResponse.ok(statsService.getOverview(couple.getId()));
    }

    @GetMapping("/achievements")
    public ApiResponse<List<AchievementResponse>> achievements(
            @AuthenticationPrincipal UserPrincipal principal) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        return ApiResponse.ok(statsService.getAchievements(couple.getId()));
    }

    @GetMapping("/weekly")
    public ApiResponse<WeeklyResponse> weekly(
            @AuthenticationPrincipal UserPrincipal principal) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        return ApiResponse.ok(statsService.getWeekly(couple.getId()));
    }
}
