package com.unibond.couple.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
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
import java.time.LocalDate;
import java.util.Map;

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
        String partnerToken = null;
        if (me.getPartnerId() != null) {
            User partner = userRepository.findById(me.getPartnerId()).orElse(null);
            if (partner != null && partner.getDeviceToken() != null) {
                partnerToken = partner.getDeviceToken();
            }
        }

        coupleService.unbind(principal.userId());

        if (partnerToken != null) {
            pushService.sendNotification(partnerToken, "UniBond", "你的情侣关系已被解除", "COUPLE_UNBIND", null);
        }
        return ApiResponse.ok(null);
    }

    @PutMapping("/anniversary")
    public ApiResponse<CoupleResponse> updateAnniversary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        String dateStr = body.get("anniversaryDate");
        if (dateStr == null || dateStr.isBlank()) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        LocalDate date = LocalDate.parse(dateStr);
        Couple couple = coupleService.updateAnniversary(principal.userId(), date);
        Long partnerId = couple.getUserAId().equals(principal.userId())
            ? couple.getUserBId() : couple.getUserAId();
        User partner = userRepository.findById(partnerId).orElseThrow();
        return ApiResponse.ok(new CoupleResponse(
            couple.getId(), partnerId, partner.getNickname(),
            couple.getAnniversaryDate(), couple.getBindAt()));
    }
}
