package com.ruoyi.report.mapper;

import com.ruoyi.report.domain.ReportRegister;
import com.ruoyi.report.domain.vo.ReportDbVo;
import com.ruoyi.report.domain.vo.ReportRegisterVo;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报表注册Mapper接口
 *
 * @author gssong
 * @date 2023-01-07
 */
public interface ReportRegisterMapper extends BaseMapperPlus<ReportRegisterMapper, ReportRegister, ReportRegisterVo> {

    /**+
     * 分页查询
     * @param dbCode 报表编码
     * @param name 报表名称
     * @param pageNum 当前页
     * @param pageSize 页容量
     * @return
     */
    List<ReportDbVo> selectReportDbPage(@Param("dbCode") String dbCode, @Param("name") String name, @Param("pageNum") Integer pageNum, @Param("pageSize") Integer pageSize);

    /**+
     * 总数
     * @param dbCode 报表编码
     * @param name 报表名称
     * @param pageNum 当前页
     * @param pageSize 页容量
     * @return
     */
    Integer selectReportDbCount(@Param("dbCode") String dbCode, @Param("name") String name, @Param("pageNum") Integer pageNum, @Param("pageSize") Integer pageSize);

}
