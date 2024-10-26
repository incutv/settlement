package com.example.settlement.see.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class SseController {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @GetMapping("/sse")
    public SseEmitter handleSse() {
        SseEmitter sseEmitter = new SseEmitter();

        executorService.submit(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    // 데이터 전송
                    sseEmitter.send("Event " + i);
                    Thread.sleep(1000); // 1초 대기
                }
                sseEmitter.complete(); // 이벤트 전송 완료
            } catch (IOException | InterruptedException e) {
                sseEmitter.completeWithError(e); // 오류 발생 시 완료
            }
        });

        return sseEmitter;
    }
}

