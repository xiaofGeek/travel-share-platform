package com.travelshare.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelshare.platform.entity.Comment;
import org.apache.ibatis.annotations.Update;

public interface CommentMapper extends BaseMapper<Comment> {
    @Update("update travel_comment set parent_id=null,reply_user_id=null,update_time=now() where parent_id=#{id} and deleted=0")
    int detachReplies(Long id);
}
