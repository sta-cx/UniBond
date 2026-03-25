package com.unibond.couple.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record BindRequest(@NotBlank @Size(min = 6, max = 6) String inviteCode) {}
