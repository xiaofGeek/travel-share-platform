package com.travelshare.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("travel_guide")
public class Guide {
    @TableId(type = IdType.AUTO) private Long id;
    private String guideNo;
    private String title;
    private String subtitle;
    private String coverImage;
    private String summary;
    private Long authorId;
    private Long destinationId;
    private Long topicId;
    private Integer days;
    private BigDecimal budget;
    private String months;
    private String travelMode;
    private String audience;
    private String content;
    private String expenses;
    private String tips;
    private String status;
    private String auditStatus;
    private String auditOpinion;
    private Integer featured;
    private Integer pinned;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime publishedAt;
    @TableLogic private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

