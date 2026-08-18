package com.travelshare.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("travel_route")
public class TravelRoute {
    @TableId(type = IdType.AUTO) private Long id;
    private String routeNo;
    private String name;
    private String coverImage;
    private Long userId;
    private Long destinationId;
    private Integer totalDays;
    private BigDecimal budget;
    private String startPoint;
    private String endPoint;
    private String season;
    private String audience;
    private String summary;
    private String status;
    private Integer isPublic;
    private Integer favoriteCount;
    private Integer viewCount;
    @TableLogic private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

