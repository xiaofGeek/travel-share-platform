package com.travelshare.platform.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record CommentRequest(@NotNull Long guideId, Long parentId, Long replyUserId,
                             @NotBlank(message = "评论内容不能为空") @Size(max = 500, message = "评论不能超过 500 字") String content) {}

