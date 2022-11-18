package com.ruoyi.workflow.common;


/**
 * @description: 分页参数
 * @author: gssong
 * @date: 2022-02-26
 */
public class PageEntity {

    /**
     * 当前页码
     */
    private Integer pageNum = 0;

    /**
     * 页容量
     */
    private Integer pageSize = 10;

    public Integer getPageNum() {
        return (pageNum - 1) * pageSize;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
