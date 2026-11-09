package com.onward.orders.controller;

import com.onward.orders.dto.response.ServerInfoResponse;
import com.onward.orders.service.InfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InfoController {
    private final InfoService infoService;

    public InfoController(InfoService infoService) {
        this.infoService = infoService;
    }

    @GetMapping("/info")
    public ServerInfoResponse info() {
        return infoService.getServerInfo();
    }
}
