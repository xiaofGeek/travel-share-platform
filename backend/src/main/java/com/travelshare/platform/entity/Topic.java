package com.travelshare.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("travel_topic")
public class Topic {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String subtitle;
    private String coverImage;
    private String summary;
    private String content;
    private Integer recommended;
    private Integer enabled;
    private Integer sortOrder;
    @TableLogic private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

