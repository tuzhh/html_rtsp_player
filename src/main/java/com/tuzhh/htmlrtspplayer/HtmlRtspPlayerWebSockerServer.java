package com.tuzhh.htmlrtspplayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.HashMap;
import java.util.List;

@Component
public class HtmlRtspPlayerWebSockerServer extends AbstractWebSocketHandler {

    @Autowired
    HandleWebSocker handleWebSocker;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        handleWebSocker.afterConnectionEstablished(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)  {
        handleWebSocker.handleTextMessage(session,message);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)  {
        handleWebSocker.handleBinaryMessage(session,message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        handleWebSocker.afterConnectionClosed(session,status);
        super.afterConnectionClosed(session, status);
    }
}
