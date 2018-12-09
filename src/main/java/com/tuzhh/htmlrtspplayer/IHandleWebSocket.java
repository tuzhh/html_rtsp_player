package com.tuzhh.htmlrtspplayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public interface IHandleWebSocket {

    void afterConnectionEstablished(WebSocketSession session) throws Exception;

    void handleTextMessage(WebSocketSession session, TextMessage message);

    void handleBinaryMessage(WebSocketSession session, BinaryMessage message);

    void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception;
}
