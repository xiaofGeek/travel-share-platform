package com.travelshare.platform.query;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class GuideQuery {
    @Min(1) private long page = 1;
    @Min(1) @Max(50) private long size = 12;
    private String keyword;
    private Long destinationId;
    private Long topicId;
    private Integer days;
    private BigDecimal maxBudget;
    private String month;
    private String audience;
    private String sort = "recommended";
}

