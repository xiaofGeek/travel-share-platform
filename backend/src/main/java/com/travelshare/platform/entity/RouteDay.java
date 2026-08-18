package com.travelshare.platform.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("travel_route_day")
public class RouteDay {
    @TableId(type = IdType.AUTO) private Long id;
    private Long routeId;
    private Integer dayNumber;
    private String title;
    private String summary;
    private BigDecimal dailyCost;
    private LocalDateTime createTime;
    public RouteDay() {}
    public RouteDay(Long routeId, Integer dayNumber, String title, String summary) {
        this.routeId=routeId; this.dayNumber=dayNumber; this.title=title; this.summary=summary; this.dailyCost=BigDecimal.ZERO;
    }
}
