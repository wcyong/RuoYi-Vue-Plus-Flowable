package com.ruoyi.workflow.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workflow.domain.bo.DefinitionBo;
import com.ruoyi.workflow.domain.vo.ActProcessNodeVo;
import com.ruoyi.workflow.domain.vo.ProcessDefinitionVo;
import com.ruoyi.workflow.service.IProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.util.*;

/**
 * 流程定义
 *
 * @author gssong
 * @date 2021/10/07 11:12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/definition")
public class ProcessDefinitionController extends BaseController {

    private final IProcessDefinitionService iProcessDefinitionService;

    /**
     * 查询流程定义列表
     * @param: defReq
     * @return: com.ruoyi.common.core.domain.R<java.util.List < com.ruoyi.workflow.domain.vo.ProcessDefinitionVo>>
     * @author: gssong
     * @date: 2021/10/7
     */
    @GetMapping("/list")
    public TableDataInfo<ProcessDefinitionVo> getByPage(DefinitionBo defReq) {
        return iProcessDefinitionService.getByPage(defReq);
    }

    /**
     * 查询历史流程定义列表
     * @param: definitionBo
     * @return: com.ruoyi.common.core.domain.R<java.util.List < com.ruoyi.workflow.domain.vo.ProcessDefinitionVo>>
     * @author: gssong
     * @date: 2021/10/7
     */
    @GetMapping("/getHistByPage")
    public R<List<ProcessDefinitionVo>> getHistByPage(DefinitionBo definitionBo) {
        List<ProcessDefinitionVo> definitionVoList = iProcessDefinitionService.getHistByPage(definitionBo);
        return R.ok(definitionVoList);
    }


    /**
     * 删除流程定义
     * @param: deploymentId 流程部署id
     * @param: definitionId 流程定义id
     * @return: com.ruoyi.common.core.domain.R<java.lang.Void>
     * @author: gssong
     * @date: 2021/10/7
     */
    @Log(title = "流程定义管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deploymentId}/{definitionId}")
    public R<Void> deleteDeployment(@NotBlank(message = "流程部署id不能为空") @PathVariable String deploymentId,
                                    @NotBlank(message = "流程定义id不能为空") @PathVariable String definitionId) {
        return toAjax(iProcessDefinitionService.deleteDeployment(deploymentId, definitionId));
    }


    /**
     * 通过zip或xml部署流程定义
     * @param: file
     * @return: com.ruoyi.common.core.domain.R<java.lang.Void>
     * @author: gssong
     * @date: 2022/4/12 13:32
     */
    @Log(title = "流程定义管理", businessType = BusinessType.INSERT)
    @PostMapping("/deployByFile")
    public R<Void> deployByFile(@RequestParam("file") MultipartFile file) {
        return toAjax(iProcessDefinitionService.deployByFile(file));
    }


    /**
     * 导出流程定义文件（xml,png)
     * @param: type 类型 xml 或 png
     * @param: definitionId 流程定义id
     * @param: response
     * @return: void
     * @author: gssong
     * @date: 2021/10/7
     */
    @SaIgnore
    @GetMapping("/export/{type}/{definitionId}")
    public void exportFile(@NotBlank(message = "文件类型不能为空") @PathVariable String type,
                           @NotBlank(message = "流程定义id不能为空") @PathVariable String definitionId,
                           HttpServletResponse response) {
        iProcessDefinitionService.exportFile(type, definitionId, response);
    }

    /**
     * 查看xml文件
     * @param: definitionId
     * @return: com.ruoyi.common.core.domain.R<java.lang.Object>
     * @author: gssong
     * @date: 2022/5/3 19:25
     */
    @GetMapping("/getXml/{definitionId}")
    public R<Map<String,Object>> getXml(@NotBlank(message = "流程定义id不能为空") @PathVariable String definitionId) {
        Map<String, Object> map = new HashMap<>();
        String xmlStr = iProcessDefinitionService.getXml(definitionId);
        List<String> xml = new ArrayList<>(Arrays.asList(xmlStr.split("\n")));
        map.put("xml",xml);
        map.put("xmlStr",xmlStr);
        return R.ok(map);
    }

    /**
     * 激活或者挂起流程定义
     * @param: data 参数
     * @return: com.ruoyi.common.core.domain.R<java.lang.Boolean>
     * @author: gssong
     * @date: 2021/10/10
     */
    @Log(title = "流程定义管理", businessType = BusinessType.UPDATE)
    @PutMapping("/updateProcDefState")
    public R<Boolean> updateProcDefState(@RequestBody Map<String, Object> data) {
        return R.ok(iProcessDefinitionService.updateProcDefState(data));
    }

    /**
     * 查询流程环节
     * @param: processDefinitionId
     * @return: com.ruoyi.common.core.domain.R<java.util.List < com.ruoyi.workflow.domain.vo.ActProcessNodeVo>>
     * @author: gssong
     * @date: 2021/11/19
     */
    @GetMapping("/setting/{processDefinitionId}")
    public R<List<ActProcessNodeVo>> setting(@NotBlank(message = "流程定义id不能为空") @PathVariable String processDefinitionId) {
        return R.ok(iProcessDefinitionService.setting(processDefinitionId));
    }

    /**
     * 迁移流程定义
     * @param: currentProcessDefinitionId
     * @param: fromProcessDefinitionId
     * @return: com.ruoyi.common.core.domain.R<java.lang.Boolean>
     * @author: gssong
     * @date: 2022/11/1 12:49
     */
    @Log(title = "流程定义管理", businessType = BusinessType.UPDATE)
    @PutMapping("/migrationProcessDefinition/{currentProcessDefinitionId}/{fromProcessDefinitionId}")
    public R<Boolean> migrationProcessDefinition(@PathVariable String currentProcessDefinitionId,@PathVariable String fromProcessDefinitionId) {
        return R.ok(iProcessDefinitionService.migrationProcessDefinition(currentProcessDefinitionId,fromProcessDefinitionId));
    }


}
