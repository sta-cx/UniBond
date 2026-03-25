package com.unibond.mood.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.security.UserPrincipal;
import com.unibond.mood.dto.MoodResponse;
import com.unibond.mood.dto.MoodUpdateRequest;
import com.unibond.mood.entity.MoodStatus;
import com.unibond.mood.service.MoodService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mood")
public class MoodController {
    private final MoodService moodService;

    public MoodController(MoodService moodService) {
        this.moodService = moodService;
    }

    @PostMapping
    public ApiResponse<MoodResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MoodUpdateRequest req) {
        MoodStatus mood = moodService.updateMood(principal.userId(), req.emoji(), req.text());
        return ApiResponse.ok(new MoodResponse(mood.getMoodEmoji(), mood.getMoodText(), mood.getUpdatedAt()));
    }

    @GetMapping("/partner")
    public ApiResponse<MoodResponse> partnerMood(@AuthenticationPrincipal UserPrincipal principal) {
        MoodStatus mood = moodService.getPartnerMood(principal.userId());
        if (mood == null) return ApiResponse.ok(null);
        return ApiResponse.ok(new MoodResponse(mood.getMoodEmoji(), mood.getMoodText(), mood.getUpdatedAt()));
    }
}
