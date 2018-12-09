package com.tuzhh.htmlrtspplayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;

@Component
public class HandleWebSocker {

    @Autowired
    HandleRtpWebSocker handleRtpWebSocker;

    @Autowired
    HandleRtspWebSocker handleRtspWebSocker;

    IHandleWebSocket getHandleWebSocket(WebSocketSession session) {
        String secWebSocketProtocol = session.getHandshakeHeaders().getFirst("Sec-WebSocket-Protocol");
        if(StringUtils.isEmpty(secWebSocketProtocol)) secWebSocketProtocol = session.getHandshakeHeaders().getFirst("sec-websocket-protocol");
        if("control".equalsIgnoreCase(secWebSocketProtocol)) return handleRtspWebSocker;
        if("data".equalsIgnoreCase(secWebSocketProtocol)) return handleRtpWebSocker;
        return null;
    }

    void afterConnectionEstablished(WebSocketSession session) throws Exception {
        IHandleWebSocket handleWebSocket = getHandleWebSocket(session);
        if(handleWebSocket != null) {
            handleWebSocket.afterConnectionEstablished(session);
        }
    }

    void handleTextMessage(WebSocketSession session, TextMessage message) {
        IHandleWebSocket handleWebSocket = getHandleWebSocket(session);
        if(handleWebSocket != null) {
            handleWebSocket.handleTextMessage(session,message);
        }
    }

    void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        IHandleWebSocket handleWebSocket = getHandleWebSocket(session);
        if(handleWebSocket != null) {
            handleWebSocket.handleBinaryMessage(session,message);
        }
    }

    void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        IHandleWebSocket handleWebSocket = getHandleWebSocket(session);
        if(handleWebSocket != null) {
            handleWebSocket.afterConnectionClosed(session,status);
        }
    }
}
