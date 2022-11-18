package com.ruoyi.workflow.common.enums;

import com.ruoyi.common.exception.ServiceException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @description: 业务枚举
 * @author: gssong
 * @date: 2021/10/10 11:13
 */
@Getter
@AllArgsConstructor
public enum BusinessStatusEnum {
    /**
     * 已撤回
     */
    CANCEL("cancel", "已撤回"),
    /**
     * 草稿
     */
    DRAFT("draft", "草稿"),
    /**
     * 待审批
     */
    WAITING("waiting", "待审批"),
    /**
     * 已完成
     */
    FINISH("finish", "已完成"),
    /**
     * 已作废
     */
    INVALID("invalid", "已作废"),
    /**
     * 已删除
     */
    DELETE("delete", "已删除"),
    /**
     * 已退回
     */
    BACK("back", "已退回"),
    /**
     * 已终止
     */
    TERMINATION("termination", "已终止");

    /**
     * 状态
     */
    private final String status;

    /**
     * 描述
     */
    private final String desc;

    /**
     * @description: 获取业务状态
     * @param: status 状态
     * @return: void
     * @author: gssong
     * @date: 2022/09/02
     */
    public static BusinessStatusEnum getEumByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return null;
        }

        for (BusinessStatusEnum statusEnum : BusinessStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * @description: 启动流程校验
     * @param: status 状态
     * @return: void
     * @author: gssong
     * @date: 2022/7/28
     */
    public static void checkStatus(String status) {
        if (status.equals(WAITING.getStatus())) {
            throw new ServiceException("该单据已提交过申请,正在审批中");
        } else if (status.equals(FINISH.getStatus())) {
            throw new ServiceException("该单据已完成申请");
        } else if (status.equals(INVALID.getStatus())) {
            throw new ServiceException("该单据已作废");
        } else if (status.equals(DELETE.getStatus())) {
            throw new ServiceException("该单据已删除");
        } else if (status.equals(TERMINATION.getStatus())) {
            throw new ServiceException("该单据已终止");
        }
    }

    /**
     * @description: 校验撤销申请
     * @param: status 状态
     * @return: void
     * @author: gssong
     * @date: 2022/7/28
     */
    public static void checkCancel(String status) {
        if (status.equals(FINISH.getStatus())) {
            throw new ServiceException("该单据已完成申请");
        } else if (status.equals(INVALID.getStatus())) {
            throw new ServiceException("该单据已作废");
        } else if (status.equals(DELETE.getStatus())) {
            throw new ServiceException("该单据已删除");
        } else if (status.equals(CANCEL.getStatus())) {
            throw new ServiceException("该单据已撤回");
        } else if (status.equals(BACK.getStatus())) {
            throw new ServiceException("该单据已退回");
        } else if (status.equals(TERMINATION.getStatus())) {
            throw new ServiceException("该单据已终止");
        }
    }
}

