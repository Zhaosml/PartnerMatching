package com.zsmx.usercenter.config;

import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
@Configuration
public class CorsFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request= (HttpServletRequest) servletRequest;

        // 允许指定的来源进行访问，或者使用通配符 * 允许所有来源
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");

        // 允许客户端发送凭据（例如，包含身份验证信息的请求）
        response.setHeader("Access-Control-Allow-Credentials", "true");

        // 允许的请求方法
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        // 允许的自定义请求头
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");

        // 预检请求的缓存时间（单位：秒）
        response.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // 针对预检请求（OPTIONS 请求），直接返回成功状态码
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            // 非预检请求，继续处理其他过滤器
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
    }
}
