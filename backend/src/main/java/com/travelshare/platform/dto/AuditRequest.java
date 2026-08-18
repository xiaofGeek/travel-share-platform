package com.travelshare.platform.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AuditRequest(@NotBlank(message = "审核结果不能为空") String decision,
                           @NotBlank(message = "必须填写审核意见") @Size(max = 500, message = "审核意见不能超过 500 个字符") String opinion) {}
