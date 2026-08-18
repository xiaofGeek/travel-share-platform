package com.travelshare.platform.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record GuideRequest(
        @NotBlank(message = "攻略标题不能为空") @Size(max = 100) String title,
        @Size(max = 140) String subtitle,
        @NotBlank(message = "请上传封面") String coverImage,
        @NotBlank(message = "攻略摘要不能为空") @Size(max = 500) String summary,
        @NotNull(message = "请选择目的地") Long destinationId,
        Long topicId,
        @Min(1) @Max(60) Integer days,
        @DecimalMin("0") BigDecimal budget,
        String months,
        String travelMode,
        String audience,
        @NotBlank(message = "攻略正文不能为空") @Size(min = 30, max = 100000) String content,
        String expenses,
        String tips) {}

