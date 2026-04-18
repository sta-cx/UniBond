package com.unibond.couple.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.security.UserPrincipal;
import com.unibond.couple.dto.BindRequest;
import com.unibond.couple.dto.CoupleResponse;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.service.CoupleService;
import com.unibond.push.service.PushService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/couple")
public class CoupleController {
    private final CoupleService coupleService;
    private final UserRepository userRepository;
    private final PushService pushService;

    public CoupleController(CoupleService coupleService, UserRepository userRepository,
                            PushService pushService) {
        this.coupleService = coupleService;
        this.userRepository = userRepository;
        this.pushService = pushService;
    }

    @GetMapping("/info")
    public ApiResponse<CoupleResponse> info(@AuthenticationPrincipal UserPrincipal principal) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        Long partnerId = couple.getUserAId().equals(principal.userId())
            ? couple.getUserBId() : couple.getUserAId();
        User partner = userRepository.findById(partnerId).orElseThrow();
        return ApiResponse.ok(new CoupleResponse(
            couple.getId(), partnerId, partner.getNickname(),
            couple.getAnniversaryDate(), couple.getBindAt()));
    }

    @PostMapping("/bind")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CoupleResponse> bind(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BindRequest req) {
        Couple couple = coupleService.bind(principal.userId(), req.inviteCode());
        Long partnerId = couple.getUserAId().equals(principal.userId())
            ? couple.getUserBId() : couple.getUserAId();
        User partner = userRepository.findById(partnerId).orElseThrow();

        // Send push notification to partner
        if (partner.getDeviceToken() != null) {
            pushService.sendPush(partner.getDeviceToken(),
                "UniBond", "你的伴侣已确认绑定！", "COUPLE_BOUND");
        }

        return ApiResponse.ok(new CoupleResponse(
            couple.getId(), partnerId, partner.getNickname(),
            couple.getAnniversaryDate(), couple.getBindAt()));
    }

    @DeleteMapping("/unbind")
    public ApiResponse<Void> unbind(@AuthenticationPrincipal UserPrincipal principal) {
        User me = userRepository.findById(principal.userId()).orElseThrow();
        User partner = userRepository.findById(me.getPartnerId()).orElse(null);

        coupleService.unbind(principal.userId());

        // Send push notification to partner
        if (partner != null && partner.getDeviceToken() != null) {
            pushService.sendPush(partner.getDeviceToken(),
                "UniBond", "你的情侣关系已被解除");
        }
        return ApiResponse.ok(null);
    }
}
