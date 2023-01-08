package com.ruoyi.report.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 *
 * @author gssong
 * @date 2023-01-07
 */
@Data
public class ReportDbVo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private Long id;

    /**
     * 报表编码
     */
    private String dbCode;

    /**
     * 报表名称
     */
    private String name;

    /**
     * 类型
     */
    private String type;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;


}
