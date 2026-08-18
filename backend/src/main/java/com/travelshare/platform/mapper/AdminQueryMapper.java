package com.travelshare.platform.mapper;

import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface AdminQueryMapper {
    @Select("select (select count(*) from sys_user where deleted=0) users,(select count(*) from sys_user where role='CREATOR' and deleted=0) creators,(select count(*) from travel_guide where deleted=0) guides,(select count(*) from travel_guide where status='PUBLISHED' and deleted=0) publishedGuides,(select count(*) from travel_guide where audit_status='PENDING' and deleted=0) pendingGuides,(select count(*) from travel_route where deleted=0) routes,(select count(*) from travel_destination where deleted=0 and type in ('CITY','SCENIC','ISLAND','ANCIENT_TOWN','NATURE')) destinations,(select count(*) from travel_report where status='PENDING') pendingReports,(select coalesce(sum(view_count),0) from travel_guide where deleted=0) totalViews")
    Map<String,Object> summary();
    @Select("select date(create_time) day,count(*) value from sys_user where create_time>=date_sub(curdate(),interval 13 day) and deleted=0 group by date(create_time) order by day")
    List<Map<String,Object>> userTrend();
    @Select("select date(create_time) day,count(*) value from travel_guide where create_time>=date_sub(curdate(),interval 13 day) and deleted=0 group by date(create_time) order by day")
    List<Map<String,Object>> guideTrend();
    @Select("select d.name,coalesce(sum(g.view_count),0) value from travel_destination d left join travel_guide g on g.destination_id=d.id and g.status='PUBLISHED' where d.deleted=0 group by d.id order by value desc limit 8")
    List<Map<String,Object>> destinationRanking();
    @Select("select g.id,g.title,g.view_count value,u.nickname author from travel_guide g join sys_user u on u.id=g.author_id where g.status='PUBLISHED' and g.deleted=0 order by g.view_count desc limit 8")
    List<Map<String,Object>> guideRanking();
    @Select("select u.id,u.nickname,u.avatar,u.received_likes value from sys_user u where u.role='CREATOR' and u.deleted=0 order by u.received_likes desc limit 8")
    List<Map<String,Object>> creatorRanking();
    @Select("select r.*,reporter.nickname reporter_name," +
            "case when r.target_type='GUIDE' then g.title when r.target_type='COMMENT' then concat('评论 #',r.target_id) else concat(r.target_type,' #',r.target_id) end target_title," +
            "case when r.target_type='GUIDE' then g.summary when r.target_type='COMMENT' then c.content else null end target_content," +
            "case when r.target_type='GUIDE' then g.status when r.target_type='COMMENT' then c.status else null end target_status " +
            "from travel_report r left join sys_user reporter on reporter.id=r.reporter_id " +
            "left join travel_guide g on r.target_type='GUIDE' and g.id=r.target_id " +
            "left join travel_comment c on r.target_type='COMMENT' and c.id=r.target_id " +
            "order by r.create_time desc limit 100")
    List<Map<String,Object>> reports();
    @Select("select ar.*,g.title,u.nickname auditor_name from travel_audit_record ar left join travel_guide g on g.id=ar.target_id left join sys_user u on u.id=ar.auditor_id where ar.target_type='GUIDE' order by ar.create_time desc limit 10")
    List<Map<String,Object>> recentAudits();
}
