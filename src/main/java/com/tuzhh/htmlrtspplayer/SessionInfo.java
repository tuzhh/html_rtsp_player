package com.tuzhh.htmlrtspplayer;

import java.util.HashMap;

public class SessionInfo {
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public LocalRtspService getLocalRtspService() {
        return localRtspService;
    }

    public void setLocalRtspService(LocalRtspService localRtspService) {
        this.localRtspService = localRtspService;
    }

    public RemoteRtspService getRemoteRtspService() {
        return remoteRtspService;
    }

    public void setRemoteRtspService(RemoteRtspService remoteRtspService) {
        this.remoteRtspService = remoteRtspService;
    }
    public void close() {
        if(localRtspService != null) localRtspService.close();
        if(remoteRtspService != null) remoteRtspService.close();
        hsmpChannel2SessionInfo.remove(channel);
    }
    public String getChannel() {
        return channel;
    }

    public SessionInfo() {
        channel = Integer.toString(channelSeq ++);
        synchronized (hsmpChannel2SessionInfo) {
            hsmpChannel2SessionInfo.put(channel, this);
        }
    }

    private LocalRtspService localRtspService = null;
    private RemoteRtspService remoteRtspService = null;
    private String redirectUrl = null;
    private String url = null;
    private String host = null;
    private int port = 0;

    private String channel = null;

    private final static HashMap<String,SessionInfo> hsmpSessionInfo = new HashMap<String,SessionInfo>();
    private final static HashMap<String,SessionInfo> hsmpChannel2SessionInfo = new HashMap<String,SessionInfo>();
    private static int channelSeq = 0;

    public static SessionInfo getByChannel(String channel) {
        synchronized (hsmpChannel2SessionInfo) {
            return hsmpChannel2SessionInfo.get(channel);
        }
    }
    public static void add(String sessionId,SessionInfo sessionInfo) {
        hsmpSessionInfo.put(sessionId,sessionInfo);
    }
    public static void remove(String sessionId) {
        hsmpSessionInfo.remove(sessionId);
    }
    public static SessionInfo get(String sessionId) {
        return hsmpSessionInfo.get(sessionId);
    }
}
