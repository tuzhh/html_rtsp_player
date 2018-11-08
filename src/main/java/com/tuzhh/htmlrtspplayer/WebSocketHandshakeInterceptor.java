package com.tuzhh.htmlrtspplayer;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpSession;
import java.util.Map;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            ServletServerHttpResponse servletResponse = (ServletServerHttpResponse) response;
            String protocol = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
            if(!StringUtils.isEmpty(protocol))
                response.getHeaders().set("Sec-WebSocket-Protocol",protocol);

            HttpSession session = servletRequest.getServletRequest().getSession(false);
            if (session != null) {
                //session.setAttribute(Constants.Content);
                //session.setAttribute(Constants.Encod);
                //String userName = (String) session.getAttribute(Constants.SESSION_USERNAME);
                //attributes.put(Constants.WEBSOCKET_USERNAME,userName);
                }
            }
            return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
