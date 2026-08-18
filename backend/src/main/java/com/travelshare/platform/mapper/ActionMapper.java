package com.travelshare.platform.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

public interface ActionMapper {
    @Select("select count(*) from travel_guide_like where user_id=#{userId} and guide_id=#{guideId}")
    int hasGuideLike(Long userId, Long guideId);
    @Insert("insert into travel_guide_like(user_id,guide_id,create_time) values(#{userId},#{guideId},now())")
    int addGuideLike(Long userId, Long guideId);
    @Delete("delete from travel_guide_like where user_id=#{userId} and guide_id=#{guideId}")
    int removeGuideLike(Long userId, Long guideId);
    @Update("update travel_guide set like_count=greatest(like_count+#{delta},0) where id=#{guideId}")
    int changeGuideLikes(Long guideId, int delta);

    @Select("select count(*) from travel_favorite where user_id=#{userId} and target_type=#{type} and target_id=#{targetId}")
    int hasFavorite(Long userId, String type, Long targetId);
    @Insert("insert into travel_favorite(user_id,folder_id,target_type,target_id,create_time) values(#{userId},(select id from travel_favorite_folder where user_id=#{userId} order by id limit 1),#{type},#{targetId},now())")
    int addFavorite(Long userId, String type, Long targetId);
    @Delete("delete from travel_favorite where user_id=#{userId} and target_type=#{type} and target_id=#{targetId}")
    int removeFavorite(Long userId, String type, Long targetId);
    @Update("update travel_guide set favorite_count=greatest(favorite_count+#{delta},0) where id=#{targetId} and #{type}='GUIDE'")
    int changeGuideFavorites(String type, Long targetId, int delta);
    @Update("update travel_route set favorite_count=greatest(favorite_count+#{delta},0) where id=#{targetId} and deleted=0")
    int changeRouteFavorites(Long targetId, int delta);

    @Select("select count(*) from travel_user_follow where user_id=#{userId} and target_user_id=#{targetId}")
    int hasFollow(Long userId, Long targetId);
    @Insert("insert into travel_user_follow(user_id,target_user_id,create_time) values(#{userId},#{targetId},now())")
    int addFollow(Long userId, Long targetId);
    @Delete("delete from travel_user_follow where user_id=#{userId} and target_user_id=#{targetId}")
    int removeFollow(Long userId, Long targetId);
    @Update("update sys_user set following_count=greatest(following_count+#{delta},0) where id=#{userId}")
    int changeFollowing(Long userId, int delta);
    @Update("update sys_user set follower_count=greatest(follower_count+#{delta},0) where id=#{userId}")
    int changeFollowers(Long userId, int delta);

    @Insert("insert into travel_search_history(user_id,keyword,create_time) values(#{userId},#{keyword},now())")
    int addSearch(Long userId, String keyword);
    @Delete("delete from travel_search_history where user_id=#{userId}")
    int clearSearch(Long userId);
    @Select("select * from travel_message where receiver_id=#{userId} and deleted=0 order by create_time desc limit 100")
    List<Map<String,Object>> messages(Long userId);
    @Update("update travel_message set is_read=1 where id=#{id} and receiver_id=#{userId}")
    int readMessage(Long userId, Long id);
    @Update("update travel_message set is_read=1 where receiver_id=#{userId}")
    int readAllMessages(Long userId);
    @Insert("insert into travel_report(reporter_id,target_type,target_id,reason,description,status,create_time) values(#{userId},#{type},#{targetId},#{reason},#{description},'PENDING',now())")
    int addReport(Long userId, String type, Long targetId, String reason, String description);
    @Select("select count(*) from travel_report where reporter_id=#{userId} and target_type=#{type} and target_id=#{targetId} and status='PENDING'")
    int hasPendingReport(Long userId, String type, Long targetId);
    @Select("select * from travel_report where id=#{reportId}")
    Map<String,Object> reportById(Long reportId);
    @Insert("insert into travel_audit_record(target_type,target_id,auditor_id,decision,opinion,create_time) values(#{type},#{targetId},#{auditorId},#{decision},#{opinion},now())")
    int addAudit(String type, Long targetId, Long auditorId, String decision, String opinion);
    @Insert("insert into travel_message(receiver_id,sender_id,title,content,message_type,business_type,business_id,is_read,deleted,create_time) values(#{receiverId},#{senderId},#{title},#{content},#{messageType},#{businessType},#{businessId},0,0,now())")
    int addMessage(Long receiverId, Long senderId, String title, String content, String messageType, String businessType, Long businessId);
    @Update("update travel_report set status=#{result},handle_note=#{note},handler_id=#{handlerId},handle_time=now() where id=#{reportId} and status='PENDING'")
    int handleReport(Long reportId, String result, String note, Long handlerId);
    @Select("select g.id,g.title,g.cover_image,g.summary,f.create_time from travel_favorite f join travel_guide g on g.id=f.target_id and f.target_type='GUIDE' and g.status='PUBLISHED' and g.deleted=0 where f.user_id=#{userId} order by f.create_time desc limit 100")
    List<Map<String,Object>> favorites(Long userId);
    @Select("select g.id,g.title,g.cover_image,g.summary,l.create_time from travel_guide_like l join travel_guide g on g.id=l.guide_id and g.status='PUBLISHED' and g.deleted=0 where l.user_id=#{userId} order by l.create_time desc limit 100")
    List<Map<String,Object>> likes(Long userId);
    @Select("select u.id,u.nickname,u.avatar,u.bio,u.follower_count from travel_user_follow f join sys_user u on u.id=f.target_user_id where f.user_id=#{userId} order by f.create_time desc limit 100")
    List<Map<String,Object>> following(Long userId);
    @Select("select u.id,u.nickname,u.avatar,u.bio,u.follower_count from travel_user_follow f join sys_user u on u.id=f.user_id where f.target_user_id=#{userId} order by f.create_time desc limit 100")
    List<Map<String,Object>> followers(Long userId);
    @Select("select h.id,h.create_time,h.duration_seconds,g.id guide_id,g.title,g.cover_image from travel_browse_history h join travel_guide g on g.id=h.target_id and h.target_type='GUIDE' and g.status='PUBLISHED' and g.deleted=0 where h.user_id=#{userId} order by h.create_time desc limit 100")
    List<Map<String,Object>> history(Long userId);
    @Select("select * from travel_search_history where user_id=#{userId} order by create_time desc limit 100")
    List<Map<String,Object>> searchHistory(Long userId);
    @Select("select * from travel_route where user_id=#{userId} and deleted=0 order by update_time desc limit 100")
    List<Map<String,Object>> routes(Long userId);
    @Select("select r.*," +
            "case when r.target_type='GUIDE' then g.title when r.target_type='COMMENT' then concat('评论 #',r.target_id) else concat(r.target_type,' #',r.target_id) end target_title," +
            "case when r.target_type='GUIDE' then g.summary when r.target_type='COMMENT' then c.content else null end target_content," +
            "case when r.target_type='GUIDE' then g.status when r.target_type='COMMENT' then c.status else null end target_status " +
            "from travel_report r left join travel_guide g on r.target_type='GUIDE' and g.id=r.target_id " +
            "left join travel_comment c on r.target_type='COMMENT' and c.id=r.target_id " +
            "where r.reporter_id=#{userId} order by r.create_time desc limit 100")
    List<Map<String,Object>> userReports(Long userId);
}
