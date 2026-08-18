package com.travelshare.platform.mapper;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;
public interface AdminContentMapper {
    @Select("select * from travel_recommendation order by position_code,sort_order limit 200") List<Map<String,Object>> recommendations();
    @Select("select * from sys_config order by config_key limit 200") List<Map<String,Object>> configs();
    @Select("select * from sys_operation_log order by create_time desc limit 200") List<Map<String,Object>> logs();
    @Select("select * from travel_recommendation where id=#{id}") Map<String,Object> recommendation(Long id);
    @Select("select * from sys_config where id=#{id}") Map<String,Object> config(Long id);
    @Select("select * from sys_operation_log where id=#{id}") Map<String,Object> log(Long id);
    @Insert("insert into travel_recommendation(position_code,target_type,target_id,title,sort_order,enabled,create_time) values(#{positionCode},#{targetType},#{targetId},#{title},#{sortOrder},#{enabled},now())")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertRecommendation(Map<String,Object> values);
    @Update("update travel_recommendation set position_code=#{positionCode},target_type=#{targetType},target_id=#{targetId},title=#{title},sort_order=#{sortOrder},enabled=#{enabled} where id=#{id}") int updateRecommendation(Map<String,Object> values);
    @Insert("insert into sys_config(config_key,config_value,config_name,config_type,remark,update_time) values(#{configKey},#{configValue},#{configName},#{configType},#{remark},now())")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertConfig(Map<String,Object> values);
    @Update("update sys_config set config_key=#{configKey},config_value=#{configValue},config_name=#{configName},config_type=#{configType},remark=#{remark},update_time=now() where id=#{id}") int updateConfig(Map<String,Object> values);
    @Insert("insert into sys_operation_log(user_id,username,module,operation,method,request_uri,ip,status,duration_ms,detail,create_time) values(#{userId},#{username},#{module},#{operation},'AdminController',#{requestUri},'127.0.0.1',1,0,#{detail},now())") int addLog(Map<String,Object> values);
}
