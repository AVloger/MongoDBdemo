package com.mongodemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author : avloger
 * @date : 2021/10/29 20:08
 */
@Controller
public class test {
    @GetMapping("/test")
    public String test(HttpServletRequest request) {
        request.setAttribute("path", "index");
        return "admin/index.html";
    }
}
