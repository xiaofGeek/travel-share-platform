package com.travelshare.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelshare.platform.dto.*;
import com.travelshare.platform.entity.*;
import com.travelshare.platform.exception.BusinessException;
import com.travelshare.platform.mapper.*;
import com.travelshare.platform.service.UserActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {
    private final UserMapper userMapper;
    private final GuideMapper guideMapper;
    private final DestinationMapper destinationMapper;
    private final TopicMapper topicMapper;
    private final CommentMapper commentMapper;
    private final RouteMapper routeMapper;
    private final RouteDayMapper routeDayMapper;
    private final RouteItemMapper routeItemMapper;
    private final ActionMapper actionMapper;
    private final ObjectMapper objectMapper;

    @Override public Map<String,Object> profile(String username){return safeUser(user(username));}

    @Override @Transactional
    public Map<String,Object> updateProfile(String username, ProfileRequest r){User u=user(username);u.setNickname(r.nickname());u.setCity(r.city());u.setBio(r.bio());u.setPreferences(r.preferences());if(text(r.avatar()))u.setAvatar(r.avatar());if(text(r.coverImage()))u.setCoverImage(r.coverImage());u.setUpdateTime(LocalDateTime.now());userMapper.updateById(u);return safeUser(u);}

    @Override public Object myGuides(String username,String status){User u=user(username);LambdaQueryWrapper<Guide>w=new LambdaQueryWrapper<Guide>().eq(Guide::getAuthorId,u.getId()).orderByDesc(Guide::getUpdateTime);if(text(status))w.eq(Guide::getStatus,status);return guideMapper.selectPage(new Page<>(1,100),w);}

    @Override
    public Map<String,Object> myGuide(String username,Long id){Guide g=ownedGuide(username,id);Map<String,Object>d=toMap(g);d.put("destination",destinationMapper.selectById(g.getDestinationId()));d.put("topic",g.getTopicId()==null?null:topicMapper.selectById(g.getTopicId()));return d;}

    @Override @Transactional
    public Object createGuide(String username,GuideRequest request){User u=user(username);Guide g=new Guide();copyGuide(g,request);g.setGuideNo("G"+System.currentTimeMillis()+String.format("%03d",new Random().nextInt(1000)));g.setAuthorId(u.getId());g.setStatus("DRAFT");g.setAuditStatus("NOT_SUBMITTED");g.setFeatured(0);g.setPinned(0);g.setViewCount(0);g.setLikeCount(0);g.setFavoriteCount(0);g.setCommentCount(0);g.setDeleted(0);g.setCreateTime(LocalDateTime.now());g.setUpdateTime(LocalDateTime.now());guideMapper.insert(g);u.setGuideCount(u.getGuideCount()+1);userMapper.updateById(u);return g;}

    @Override @Transactional
    public Object updateGuide(String username,Long id,GuideRequest request){Guide g=ownedGuide(username,id);if(!Set.of("DRAFT","REJECTED","OFFLINE").contains(g.getStatus()))throw BusinessException.badRequest("当前状态不能修改攻略");copyGuide(g,request);g.setUpdateTime(LocalDateTime.now());guideMapper.updateById(g);return g;}

    @Override @Transactional
    public void submitGuide(String username,Long id){Guide g=ownedGuide(username,id);if(!Set.of("DRAFT","REJECTED","OFFLINE").contains(g.getStatus()))throw BusinessException.badRequest("当前状态不能提交审核");g.setStatus("PENDING");g.setAuditStatus("PENDING");g.setAuditOpinion(null);g.setPublishedAt(null);g.setUpdateTime(LocalDateTime.now());guideMapper.updateById(g);}

    @Override @Transactional
    public void deleteGuide(String username,Long id){User u=user(username);Guide g=guideMapper.selectById(id);if(g==null)throw BusinessException.notFound("攻略不存在或已经删除");if(!g.getAuthorId().equals(u.getId()))throw BusinessException.forbidden("不能删除他人的攻略");if(guideMapper.softDeleteOwned(id,u.getId(),g.getStatus())==0)throw BusinessException.badRequest("攻略状态已经变化，请刷新后重试");userMapper.decrementGuideCount(u.getId());actionMapper.addAudit("GUIDE",id,u.getId(),"AUTHOR_DELETED","作者主动删除，删除前状态："+g.getStatus());}

    @Override @Transactional
    public Map<String,Object> toggleLike(String username,Long guideId){User u=user(username);publishedGuide(guideId);boolean exists=actionMapper.hasGuideLike(u.getId(),guideId)>0;if(exists){actionMapper.removeGuideLike(u.getId(),guideId);actionMapper.changeGuideLikes(guideId,-1);}else{actionMapper.addGuideLike(u.getId(),guideId);actionMapper.changeGuideLikes(guideId,1);}Guide g=guideMapper.selectById(guideId);return Map.of("liked",!exists,"likeCount",g.getLikeCount());}

    @Override @Transactional
    public Map<String,Object> toggleFavorite(String username,String type,Long targetId){User u=user(username);String targetType=targetType(type);favoriteTarget(targetType,targetId);boolean exists=actionMapper.hasFavorite(u.getId(),targetType,targetId)>0;if(exists)actionMapper.removeFavorite(u.getId(),targetType,targetId);else actionMapper.addFavorite(u.getId(),targetType,targetId);int delta=exists?-1:1;int count;if("GUIDE".equals(targetType)){actionMapper.changeGuideFavorites(targetType,targetId,delta);count=Objects.requireNonNullElse(guideMapper.selectById(targetId).getFavoriteCount(),0);}else{actionMapper.changeRouteFavorites(targetId,delta);count=Objects.requireNonNullElse(routeMapper.selectById(targetId).getFavoriteCount(),0);}return Map.of("favorited",!exists,"favoriteCount",count);}

    @Override @Transactional
    public Map<String,Object> toggleFollow(String username,Long targetUserId){User u=user(username);if(u.getId().equals(targetUserId))throw BusinessException.badRequest("不能关注自己");if(userMapper.selectById(targetUserId)==null)throw BusinessException.notFound("用户不存在");boolean exists=actionMapper.hasFollow(u.getId(),targetUserId)>0;if(exists){actionMapper.removeFollow(u.getId(),targetUserId);actionMapper.changeFollowing(u.getId(),-1);actionMapper.changeFollowers(targetUserId,-1);}else{actionMapper.addFollow(u.getId(),targetUserId);actionMapper.changeFollowing(u.getId(),1);actionMapper.changeFollowers(targetUserId,1);}User target=userMapper.selectById(targetUserId);return Map.of("followed",!exists,"followerCount",Objects.requireNonNullElse(target.getFollowerCount(),0));}

    @Override
    public Map<String,Object> guideInteractionState(String username,Long guideId){User u=user(username);Guide guide=publishedGuide(guideId);boolean followed=!u.getId().equals(guide.getAuthorId())&&actionMapper.hasFollow(u.getId(),guide.getAuthorId())>0;return Map.of("liked",actionMapper.hasGuideLike(u.getId(),guideId)>0,"favorited",actionMapper.hasFavorite(u.getId(),"GUIDE",guideId)>0,"followed",followed);}

    @Override
    public Map<String,Object> favoriteState(String username,String type,Long targetId){User u=user(username);String targetType=targetType(type);favoriteTarget(targetType,targetId);return Map.of("favorited",actionMapper.hasFavorite(u.getId(),targetType,targetId)>0);}

    @Override
    public Map<String,Object> followState(String username,Long targetUserId){User u=user(username);if(userMapper.selectById(targetUserId)==null)throw BusinessException.notFound("用户不存在");return Map.of("followed",!u.getId().equals(targetUserId)&&actionMapper.hasFollow(u.getId(),targetUserId)>0);}

    @Override @Transactional
    public Object addComment(String username,CommentRequest request){User u=user(username);Guide g=publishedGuide(request.guideId());if(request.parentId()!=null){Comment parent=commentMapper.selectById(request.parentId());if(parent==null||!"NORMAL".equals(parent.getStatus()))throw BusinessException.badRequest("被回复的评论不可用");}Comment c=new Comment();c.setGuideId(request.guideId());c.setUserId(u.getId());c.setParentId(request.parentId());c.setReplyUserId(request.replyUserId());c.setContent(request.content().trim());c.setLikeCount(0);c.setStatus("NORMAL");c.setDeleted(0);c.setCreateTime(LocalDateTime.now());c.setUpdateTime(LocalDateTime.now());commentMapper.insert(c);guideMapper.update(null,new LambdaUpdateWrapper<Guide>().eq(Guide::getId,g.getId()).setSql("comment_count=comment_count+1"));return c;}

    @Override @Transactional
    public Object createRoute(String username,RouteRequest request){User u=user(username);TravelRoute route=new TravelRoute();route.setRouteNo("R"+System.currentTimeMillis());route.setName(request.name());route.setCoverImage(text(request.coverImage())?request.coverImage():"/uploads/demo/routes/route-001.png");route.setUserId(u.getId());route.setDestinationId(request.destinationId());route.setTotalDays(request.totalDays());route.setBudget(request.budget());route.setStartPoint(request.startPoint());route.setEndPoint(request.endPoint());route.setSeason(request.season());route.setAudience(request.audience());route.setSummary(request.summary());route.setStatus("PUBLISHED");route.setIsPublic(Boolean.TRUE.equals(request.publicRoute())?1:0);route.setFavoriteCount(0);route.setViewCount(0);route.setDeleted(0);route.setCreateTime(LocalDateTime.now());route.setUpdateTime(LocalDateTime.now());routeMapper.insert(route);saveRouteDays(route.getId(),request.days());u.setRouteCount(u.getRouteCount()+1);userMapper.updateById(u);return route;}

    @Override @Transactional
    public Object copyRoute(String username,Long routeId){User u=user(username);TravelRoute source=routeMapper.selectById(routeId);if(source==null||!Integer.valueOf(1).equals(source.getIsPublic()))throw BusinessException.notFound("公开路线不存在");TravelRoute copy=objectMapper.convertValue(source,TravelRoute.class);copy.setId(null);copy.setRouteNo("R"+System.currentTimeMillis());copy.setName(source.getName()+" · 我的副本");copy.setUserId(u.getId());copy.setIsPublic(0);copy.setFavoriteCount(0);copy.setViewCount(0);copy.setCreateTime(LocalDateTime.now());copy.setUpdateTime(LocalDateTime.now());routeMapper.insert(copy);for(RouteDay day:routeDayMapper.selectList(new LambdaQueryWrapper<RouteDay>().eq(RouteDay::getRouteId,routeId).orderByAsc(RouteDay::getDayNumber))){Long oldDay=day.getId();day.setId(null);day.setRouteId(copy.getId());routeDayMapper.insert(day);for(RouteItem item:routeItemMapper.selectList(new LambdaQueryWrapper<RouteItem>().eq(RouteItem::getRouteDayId,oldDay))){item.setId(null);item.setRouteDayId(day.getId());routeItemMapper.insert(item);}}return copy;}

    @Override public List<Map<String,Object>> messages(String username){return actionMapper.messages(user(username).getId());}
    @Override public void readMessage(String username,Long id){actionMapper.readMessage(user(username).getId(),id);}
    @Override public void readAllMessages(String username){actionMapper.readAllMessages(user(username).getId());}
    @Override
    @Transactional
    public void report(String username,ReportRequest request){
        User reporter=user(username);
        String type=request.targetType().trim().toUpperCase(Locale.ROOT);
        if(!Set.of("GUIDE","COMMENT").contains(type))throw BusinessException.badRequest("当前仅支持举报攻略或评论");
        String reason=request.reason().trim();
        if(!Set.of("内容不实","广告营销","不友善内容","侵权或抄袭","危险行为","重复内容","其他").contains(reason))throw BusinessException.badRequest("请选择有效的举报原因");
        if("GUIDE".equals(type)){
            Guide guide=publishedGuide(request.targetId());
            if(guide.getAuthorId().equals(reporter.getId()))throw BusinessException.badRequest("不能举报自己发布的攻略");
        }else{
            Comment comment=commentMapper.selectById(request.targetId());
            if(comment==null||!"NORMAL".equals(comment.getStatus()))throw BusinessException.notFound("评论不存在或已被处理");
            publishedGuide(comment.getGuideId());
            if(comment.getUserId().equals(reporter.getId()))throw BusinessException.badRequest("不能举报自己的评论");
        }
        if(actionMapper.hasPendingReport(reporter.getId(),type,request.targetId())>0)throw BusinessException.badRequest("你已举报过该内容，请等待处理结果");
        String description=request.description()==null?"":request.description().trim();
        actionMapper.addReport(reporter.getId(),type,request.targetId(),reason,description);
    }
    @Override public void recordSearch(String username,String keyword){if(text(keyword))actionMapper.addSearch(user(username).getId(),keyword.trim());}
    @Override public void clearSearch(String username){actionMapper.clearSearch(user(username).getId());}
    @Override public Object centerData(String username,String section){Long id=user(username).getId();return switch(section){case "favorites"->actionMapper.favorites(id);case "likes"->actionMapper.likes(id);case "following"->actionMapper.following(id);case "followers"->actionMapper.followers(id);case "history"->actionMapper.history(id);case "searches"->actionMapper.searchHistory(id);case "routes"->actionMapper.routes(id);case "reports"->actionMapper.userReports(id);case "messages"->actionMapper.messages(id);default->List.of();};}

    private void saveRouteDays(Long routeId,List<RouteRequest.RouteDayRequest> days){if(days==null)return;for(var d:days){RouteDay day=new RouteDay(routeId,d.dayNumber(),d.title(),d.summary());routeDayMapper.insert(day);if(d.items()==null)continue;int sort=1;for(var x:d.items()){RouteItem item=new RouteItem();item.setRouteDayId(day.getId());item.setStartTime(x.startTime());item.setEndTime(x.endTime());item.setName(x.name());item.setType(x.type());item.setDestinationId(x.destinationId());item.setAddress(x.address());item.setTransport(x.transport());item.setCost(x.cost());item.setDurationMinutes(x.durationMinutes());item.setDescription(x.description());item.setImageUrl(x.imageUrl());item.setSortOrder(sort++);item.setCreateTime(LocalDateTime.now());routeItemMapper.insert(item);}}}
    private void copyGuide(Guide g,GuideRequest r){g.setTitle(r.title());g.setSubtitle(r.subtitle());g.setCoverImage(r.coverImage());g.setSummary(r.summary());g.setDestinationId(r.destinationId());g.setTopicId(r.topicId());g.setDays(r.days());g.setBudget(r.budget());g.setMonths(r.months());g.setTravelMode(r.travelMode());g.setAudience(r.audience());g.setContent(r.content());g.setExpenses(r.expenses());g.setTips(r.tips());}
    private User user(String username){User u=userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername,username));if(u==null)throw BusinessException.unauthorized("登录状态已失效");return u;}
    private Guide ownedGuide(String username,Long id){Guide g=guideMapper.selectById(id);if(g==null)throw BusinessException.notFound("攻略不存在");if(!g.getAuthorId().equals(user(username).getId()))throw BusinessException.forbidden("不能修改他人的攻略");return g;}
    private Guide publishedGuide(Long id){Guide g=guideMapper.selectById(id);if(g==null||!"PUBLISHED".equals(g.getStatus()))throw BusinessException.notFound("攻略不存在或暂未发布");return g;}
    private String targetType(String type){String value=type==null?"GUIDE":type.toUpperCase(Locale.ROOT);if(!Set.of("GUIDE","ROUTE").contains(value))throw BusinessException.badRequest("不支持的收藏类型");return value;}
    private void favoriteTarget(String type,Long id){if("GUIDE".equals(type))publishedGuide(id);else{TravelRoute route=routeMapper.selectById(id);if(route==null||!Integer.valueOf(1).equals(route.getIsPublic()))throw BusinessException.notFound("公开路线不存在");}}
    @SuppressWarnings("unchecked") private Map<String,Object> safeUser(User u){Map<String,Object>d=objectMapper.convertValue(u,LinkedHashMap.class);d.remove("password");return d;}
    @SuppressWarnings("unchecked") private Map<String,Object> toMap(Object value){return objectMapper.convertValue(value,LinkedHashMap.class);}
    private boolean text(String value){return value!=null&&!value.isBlank();}
}
