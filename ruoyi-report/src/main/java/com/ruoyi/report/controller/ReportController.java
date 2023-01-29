package com.ruoyi.report.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 报表检查token
 *
 * @author gssong
 * @date 2023-01-07
 */

@RestController
@RequestMapping("/report/check")
@RequiredArgsConstructor
public class ReportController extends BaseController {


    /**
     * 校验token
     * @param: token
     * @return: com.ruoyi.common.core.domain.R<java.lang.Void>
     * @author: gssong
     * @date: 2023/1/7 19:28
     */
    @GetMapping("/token/{token}")
    @SaIgnore
    public R<String> token(@PathVariable String token) {
        String result = StpUtil.stpLogic.getLoginIdNotHandle(token);
        if (result == null) {
            return R.fail();
        }
        return R.ok("操作成功",result);
    }
}
