package com.ruoyi.workflow.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 任务Mapper接口
 *
 * @author gssong
 * @date 2022-07-24
 */
public interface TaskMapper {

    /**
     * 修改审批信息
     * @param commentId 批注id
     * @param comment 批注
     */
    @Update("update act_hi_comment set message_=#{comment},full_msg_=#{comment} where id_ = #{commentId}")
    int editComment(@Param("commentId") String commentId, @Param("comment") String comment);
}