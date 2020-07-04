package com.atguigu.gmall1213.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mqx
 * @date 2020/6/22 16:25
 */
@Controller
public class PassportController {

    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        // http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "login";
    }
}
