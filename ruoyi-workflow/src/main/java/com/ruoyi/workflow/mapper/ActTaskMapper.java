package com.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.workflow.domain.vo.TaskWaitingVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 任务Mapper接口
 *
 * @author gssong
 * @date 2022-07-24
 */
public interface ActTaskMapper {

    /**
     * 修改审批信息
     * @param commentId 批注id
     * @param comment 批注
     */
    @Update("update act_hi_comment set message_=#{comment},full_msg_=#{comment} where id_ = #{commentId}")
    int editComment(@Param("commentId") String commentId, @Param("comment") String comment);

    /**
     * 自定义sql查询当前用户的待办任务
     * @param page
     * @param queryWrapper
     * @param assignee
     * @return
     */
    Page<TaskWaitingVo> getCustomTaskWaitByPage(@Param("page") Page<TaskWaitingVo> page, @Param(Constants.WRAPPER) Wrapper<TaskWaitingVo> queryWrapper,@Param("assignee") String assignee);
}
