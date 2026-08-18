package com.travelshare.platform.mapper;

import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface ContentMapper {
    @Select("select image_url,caption,sort_order from travel_guide_image where guide_id=#{guideId} order by sort_order")
    List<Map<String,Object>> guideImages(Long guideId);
    @Select("select image_url,caption,sort_order from travel_destination_image where destination_id=#{destinationId} order by sort_order")
    List<Map<String,Object>> destinationImages(Long destinationId);
    @Select("select id,name,cover_image,summary,recommended_season,suggested_hours from travel_scenic_spot where destination_id=#{destinationId} and enabled=1 order by sort_order limit 20")
    List<Map<String,Object>> scenicSpots(Long destinationId);
    @Select("select c.*,u.nickname,u.avatar from travel_comment c join sys_user u on u.id=c.user_id where c.guide_id=#{guideId} and c.status='NORMAL' and c.deleted=0 order by c.create_time desc limit #{offset},#{size}")
    List<Map<String,Object>> comments(Long guideId, long offset, long size);
    @Select("select rd.*,coalesce(sum(ri.cost),0) calculated_cost from travel_route_day rd left join travel_route_item ri on ri.route_day_id=rd.id where rd.route_id=#{routeId} group by rd.id order by rd.day_number")
    List<Map<String,Object>> routeDays(Long routeId);
    @Select("select * from travel_route_item where route_day_id=#{dayId} order by sort_order,start_time")
    List<Map<String,Object>> routeItems(Long dayId);
    @Select("select t.name from travel_guide_tag gt join travel_tag t on t.id=gt.tag_id where gt.guide_id=#{guideId} order by t.use_count desc")
    List<String> guideTags(Long guideId);
    @Select("select * from travel_announcement where enabled=1 order by pinned desc,publish_time desc limit 10")
    List<Map<String,Object>> announcements();
    @Select("select keyword,search_count from travel_hot_keyword where enabled=1 order by sort_order,search_count desc limit 20")
    List<Map<String,Object>> hotKeywords();
}

