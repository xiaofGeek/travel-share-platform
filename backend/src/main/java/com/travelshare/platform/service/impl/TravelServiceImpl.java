package com.travelshare.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelshare.platform.common.PageResult;
import com.travelshare.platform.entity.*;
import com.travelshare.platform.exception.BusinessException;
import com.travelshare.platform.mapper.*;
import com.travelshare.platform.query.GuideQuery;
import com.travelshare.platform.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TravelServiceImpl implements TravelService {
    private final BannerMapper bannerMapper;
    private final DestinationMapper destinationMapper;
    private final GuideMapper guideMapper;
    private final RouteMapper routeMapper;
    private final TopicMapper topicMapper;
    private final UserMapper userMapper;
    private final ContentMapper contentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> home() {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("banners", bannerMapper.selectList(new LambdaQueryWrapper<Banner>().eq(Banner::getEnabled,1).orderByAsc(Banner::getSortOrder).last("limit 8")));
        data.put("destinations", destinationMapper.selectList(new LambdaQueryWrapper<Destination>().eq(Destination::getEnabled,1).eq(Destination::getRecommended,1).orderByAsc(Destination::getSortOrder).last("limit 8")));
        data.put("featuredGuides", guideMaps(guideMapper.selectList(new LambdaQueryWrapper<Guide>().eq(Guide::getStatus,"PUBLISHED").eq(Guide::getFeatured,1).orderByDesc(Guide::getPinned).orderByDesc(Guide::getPublishedAt).last("limit 12"))));
        data.put("routes", routeMapper.selectList(new LambdaQueryWrapper<TravelRoute>().eq(TravelRoute::getIsPublic,1).eq(TravelRoute::getStatus,"PUBLISHED").orderByDesc(TravelRoute::getFavoriteCount).last("limit 6")));
        data.put("topics", topicMapper.selectList(new LambdaQueryWrapper<Topic>().eq(Topic::getEnabled,1).eq(Topic::getRecommended,1).orderByAsc(Topic::getSortOrder).last("limit 10")));
        data.put("creators", safeUsers(userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole,"CREATOR").eq(User::getStatus,1).orderByDesc(User::getReceivedLikes).last("limit 8"))));
        data.put("latestGuides", guideMaps(guideMapper.selectList(new LambdaQueryWrapper<Guide>().eq(Guide::getStatus,"PUBLISHED").orderByDesc(Guide::getPublishedAt).last("limit 12"))));
        data.put("hotKeywords", contentMapper.hotKeywords());
        data.put("announcements", contentMapper.announcements());
        return data;
    }

    @Override
    public PageResult<Map<String,Object>> guides(GuideQuery query) {
        LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<Guide>().eq(Guide::getStatus,"PUBLISHED");
        if (text(query.getKeyword())) wrapper.and(w -> w.like(Guide::getTitle,query.getKeyword()).or().like(Guide::getSummary,query.getKeyword()));
        if (query.getDestinationId()!=null) wrapper.eq(Guide::getDestinationId,query.getDestinationId());
        if (query.getTopicId()!=null) wrapper.eq(Guide::getTopicId,query.getTopicId());
        if (query.getDays()!=null) wrapper.eq(Guide::getDays,query.getDays());
        if (query.getMaxBudget()!=null) wrapper.le(Guide::getBudget,query.getMaxBudget());
        if (text(query.getMonth())) wrapper.like(Guide::getMonths,query.getMonth());
        if (text(query.getAudience())) wrapper.like(Guide::getAudience,query.getAudience());
        switch (query.getSort()==null?"recommended":query.getSort()) {
            case "latest" -> wrapper.orderByDesc(Guide::getPublishedAt);
            case "views" -> wrapper.orderByDesc(Guide::getViewCount);
            case "likes" -> wrapper.orderByDesc(Guide::getLikeCount);
            case "favorites" -> wrapper.orderByDesc(Guide::getFavoriteCount);
            case "comments" -> wrapper.orderByDesc(Guide::getCommentCount);
            default -> wrapper.orderByDesc(Guide::getFeatured).orderByDesc(Guide::getLikeCount).orderByDesc(Guide::getPublishedAt);
        }
        Page<Guide> page = guideMapper.selectPage(new Page<>(query.getPage(),query.getSize()), wrapper);
        return PageResult.of(guideMaps(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public Map<String,Object> guide(Long id) {
        Guide guide = guideMapper.selectById(id);
        if (guide==null || !"PUBLISHED".equals(guide.getStatus())) throw BusinessException.notFound("攻略不存在或暂未发布");
        guide.setViewCount(guide.getViewCount()+1); guideMapper.updateById(guide);
        Map<String,Object> data = guideMap(guide);
        data.put("images",contentMapper.guideImages(id)); data.put("tags",contentMapper.guideTags(id));
        data.put("comments",contentMapper.comments(id,0,20));
        data.put("related",guideMaps(guideMapper.selectList(new LambdaQueryWrapper<Guide>().eq(Guide::getStatus,"PUBLISHED").eq(Guide::getDestinationId,guide.getDestinationId()).ne(Guide::getId,id).last("limit 6"))));
        return data;
    }

    @Override
    public List<?> destinations(String keyword, String type) {
        LambdaQueryWrapper<Destination> w = new LambdaQueryWrapper<Destination>().eq(Destination::getEnabled,1);
        if(text(keyword)) w.and(x->x.like(Destination::getName,keyword).or().like(Destination::getNameEn,keyword).or().like(Destination::getTags,keyword));
        if(text(type)) w.eq(Destination::getType,type);
        return destinationMapper.selectList(w.orderByDesc(Destination::getRecommended).orderByAsc(Destination::getSortOrder));
    }

    @Override
    public Map<String,Object> destination(Long id) {
        Destination d=destinationMapper.selectById(id);
        if(d==null || !Integer.valueOf(1).equals(d.getEnabled())) throw BusinessException.notFound("目的地不存在");
        Map<String,Object> data=toMap(d); data.put("gallery",contentMapper.destinationImages(id)); data.put("scenicSpots",contentMapper.scenicSpots(id));
        data.put("guides",guideMaps(guideMapper.selectList(new LambdaQueryWrapper<Guide>().eq(Guide::getStatus,"PUBLISHED").eq(Guide::getDestinationId,id).orderByDesc(Guide::getFeatured).last("limit 8"))));
        data.put("routes",routeMapper.selectList(new LambdaQueryWrapper<TravelRoute>().eq(TravelRoute::getIsPublic,1).eq(TravelRoute::getDestinationId,id).last("limit 6")));
        return data;
    }

    @Override public List<?> routes(){return routeMapper.selectList(new LambdaQueryWrapper<TravelRoute>().eq(TravelRoute::getIsPublic,1).eq(TravelRoute::getStatus,"PUBLISHED").orderByDesc(TravelRoute::getFavoriteCount));}

    @Override
    public Map<String,Object> route(Long id){
        TravelRoute r=routeMapper.selectById(id); if(r==null||!Integer.valueOf(1).equals(r.getIsPublic())) throw BusinessException.notFound("路线不存在或未公开");
        Map<String,Object> data=toMap(r); List<Map<String,Object>> days=contentMapper.routeDays(id);
        days.forEach(day->day.put("items",contentMapper.routeItems(((Number)day.get("id")).longValue()))); data.put("days",days);
        data.put("destination",destinationMapper.selectById(r.getDestinationId())); data.put("creator",safeUser(userMapper.selectById(r.getUserId()))); return data;
    }

    @Override public List<?> topics(){return topicMapper.selectList(new LambdaQueryWrapper<Topic>().eq(Topic::getEnabled,1).orderByAsc(Topic::getSortOrder));}

    @Override public Map<String,Object> topic(Long id){Topic t=topicMapper.selectById(id); if(t==null) throw BusinessException.notFound("专题不存在"); Map<String,Object>d=toMap(t);d.put("guides",guideMaps(guideMapper.selectList(new LambdaQueryWrapper<Guide>().eq(Guide::getStatus,"PUBLISHED").eq(Guide::getTopicId,id))));return d;}

    @Override public Map<String,Object> creator(Long id){User u=userMapper.selectById(id);if(u==null)throw BusinessException.notFound("用户不存在");Map<String,Object>d=safeUser(u);d.put("guides",guideMaps(guideMapper.selectList(new LambdaQueryWrapper<Guide>().eq(Guide::getAuthorId,id).eq(Guide::getStatus,"PUBLISHED").orderByDesc(Guide::getPublishedAt))));d.put("routes",routeMapper.selectList(new LambdaQueryWrapper<TravelRoute>().eq(TravelRoute::getUserId,id).eq(TravelRoute::getIsPublic,1)));return d;}

    @Override public Map<String,Object> search(String keyword){if(!text(keyword))throw BusinessException.badRequest("请输入搜索关键词");GuideQuery q=new GuideQuery();q.setKeyword(keyword);q.setSize(20);Map<String,Object>d=new LinkedHashMap<>();d.put("guides",guides(q));d.put("destinations",destinations(keyword,null));d.put("routes",routeMapper.selectList(new LambdaQueryWrapper<TravelRoute>().eq(TravelRoute::getIsPublic,1).like(TravelRoute::getName,keyword).last("limit 20")));d.put("topics",topicMapper.selectList(new LambdaQueryWrapper<Topic>().eq(Topic::getEnabled,1).like(Topic::getName,keyword).last("limit 20")));return d;}

    private List<Map<String,Object>> guideMaps(List<Guide> guides){return guides.stream().map(this::guideMap).toList();}
    private Map<String,Object> guideMap(Guide g){Map<String,Object>d=toMap(g);d.put("author",safeUser(userMapper.selectById(g.getAuthorId())));d.put("destination",destinationMapper.selectById(g.getDestinationId()));return d;}
    private List<Map<String,Object>> safeUsers(List<User> users){return users.stream().map(this::safeUser).toList();}
    private Map<String,Object> safeUser(User u){if(u==null)return null;Map<String,Object>d=toMap(u);d.remove("password");d.remove("email");d.remove("phone");return d;}
    @SuppressWarnings("unchecked") private Map<String,Object> toMap(Object value){return objectMapper.convertValue(value,LinkedHashMap.class);}
    private boolean text(String value){return value!=null&&!value.isBlank();}
}

