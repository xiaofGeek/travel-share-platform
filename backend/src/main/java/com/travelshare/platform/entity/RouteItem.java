package com.travelshare.platform.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("travel_route_item")
public class RouteItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long routeDayId;
    private String startTime;
    private String endTime;
    private String name;
    private String type;
    private Long destinationId;
    private String address;
    private String transport;
    private BigDecimal cost;
    private Integer durationMinutes;
    private String description;
    private String imageUrl;
    private Integer sortOrder;
    private LocalDateTime createTime;
}

