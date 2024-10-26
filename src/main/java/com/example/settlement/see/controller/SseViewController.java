package com.example.settlement.see.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/sse")
public class SseViewController {

    @GetMapping("/view")
    public ModelAndView handleSse() {
        ModelAndView mav = new ModelAndView("sse"); // 가져온 데이터를 "paymentList"로 뷰에 전달

        return mav;
    }
}

