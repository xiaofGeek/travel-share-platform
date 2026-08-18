package com.travelshare.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("travel_destination")
public class Destination {
    @TableId(type = IdType.AUTO) private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String type;
    private Long parentId;
    private String coverImage;
    private String summary;
    private String description;
    private String season;
    private Integer suggestedDays;
    private BigDecimal averageBudget;
    private String tags;
    private String locationText;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer guideCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private Integer recommended;
    private Integer enabled;
    private Integer sortOrder;
    @TableLogic private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

