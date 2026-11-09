package com.onward.orders.service.impl;

import com.onward.orders.config.AppInfoProperties;
import com.onward.orders.dto.response.ServerInfoResponse;
import com.onward.orders.service.InfoService;
import com.onward.orders.service.ServerIdentityService;
import org.springframework.stereotype.Service;

@Service
public class InfoServiceImpl implements InfoService {
    private final AppInfoProperties appInfo;
    private final ServerIdentityService serverIdentity;

    public InfoServiceImpl(AppInfoProperties appInfo, ServerIdentityService serverIdentity) {
        this.appInfo = appInfo;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public ServerInfoResponse getServerInfo() {
        return new ServerInfoResponse(
                appInfo.studentName(),
                appInfo.labName(),
                appInfo.applicationName(),
                serverIdentity.currentServer());
    }
}
