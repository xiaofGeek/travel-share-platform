package com.travelshare.platform.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ProfileRequest(
        @NotBlank(message = "昵称不能为空") @Size(max = 30) String nickname,
        @Size(max = 40) String city,
        @Size(max = 240) String bio,
        @Size(max = 200) String preferences,
        String avatar,
        String coverImage) {}

