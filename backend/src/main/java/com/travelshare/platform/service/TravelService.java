package com.travelshare.platform.service;
import com.travelshare.platform.common.PageResult;
import com.travelshare.platform.query.GuideQuery;
import java.util.List;
import java.util.Map;
public interface TravelService {
    Map<String,Object> home();
    PageResult<Map<String,Object>> guides(GuideQuery query);
    Map<String,Object> guide(Long id);
    List<?> destinations(String keyword, String type);
    Map<String,Object> destination(Long id);
    List<?> routes();
    Map<String,Object> route(Long id);
    List<?> topics();
    Map<String,Object> topic(Long id);
    Map<String,Object> creator(Long id);
    Map<String,Object> search(String keyword);
}

