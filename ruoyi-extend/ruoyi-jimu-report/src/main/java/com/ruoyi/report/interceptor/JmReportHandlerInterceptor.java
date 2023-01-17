package com.ruoyi.report.interceptor;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * report 拦截器
 *
 * @author gssong
 */
public class JmReportHandlerInterceptor implements HandlerInterceptor {

    /**
     * 添加日志
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String url;

    public JmReportHandlerInterceptor(String url) {
        this.url = url;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String requestUri = request.getRequestURI();
        if (requestUri.contains("jmreport/view")) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (CollectionUtil.isNotEmpty(parameterMap) && parameterMap.containsKey("token")) {
                String token = parameterMap.get("token")[0];
                String result = null;
                try {
                    result = HttpUtil.get(url + token);
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage(response, e.getMessage());
                }
                if (result == null || result.contains(HttpStatus.HTTP_INTERNAL_ERROR + StrUtil.EMPTY)) {
                    errorMessage(response, "无权限访问,请联系管理员！");
                }
                if (result != null && result.contains("Cannot GET")) {
                    logger.error(result);
                    errorMessage(response, "请求失败,请联系管理员！");
                }
                return true;
            }
            errorMessage(response, "无权限访问,请联系管理员！");
        }
        return true;
    }

    private void errorMessage(HttpServletResponse response, String message) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        String msg = JSONObject.toJSONString(message, SerializerFeature.DisableCircularReferenceDetect);
        out.write(msg.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }
}
