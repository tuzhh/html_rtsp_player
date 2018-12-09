package com.tuzhh.htmlrtspplayer;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.HashMap;
import java.util.List;

@Component
public class HandleRtpWebSocker implements IHandleWebSocket {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Rtp Sesson start:" + session.getId());
        System.out.println(session.getAcceptedProtocol());
    }
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)  {
        SessionInfo sessionInfo = SessionInfo.get(session.getId());
        if(sessionInfo == null) {
            String str_msg = message.getPayload();
            System.out.println("Recv from RTP session:[" + session.getId() + "]\r\n" + str_msg);

            List<String> lines = Utils.msg2lines(str_msg);
            HashMap<String,String> hsmpVal = Utils.list2key(lines, ":", 1);

            String channel = hsmpVal.get("channel".toLowerCase());
            String seq = hsmpVal.get("seq".toLowerCase());

            try {
                if(!StringUtils.isEmpty(channel))
                {
                    String rtspChannel = channel.trim().substring(channel.indexOf(" ")).trim();
                    sessionInfo = SessionInfo.getByChannel(rtspChannel);
                    if(sessionInfo == null) {
                        session.sendMessage(new TextMessage("UNKNOWN\r\n\r\n"));
                        return;
                    } else {
                        SessionInfo.add(session.getId(),sessionInfo);
                        sessionInfo.getLocalRtspService().attachRtpChannel(session);

                        String s = WebSocketConfig.PROXY_PROTOCOL + "/" + WebSocketConfig.PROXY_VERSION + " 200 OK" + "\r\n"
                                + "seq: " + seq + "\r\n"
                                + "\r\n";
                        session.sendMessage(new TextMessage(s));
                        System.out.println("[Send to RTP Channel]\r\n" + s);
                        return;
                    }
                }
                session.sendMessage(new TextMessage("ERROR\r\n\r\n"));
            } catch (Exception e) {
            }
            return;
        } else {
            try {
                //sessionInfo.getTcpTask().send(null);
            } catch (Exception e) {
                e.printStackTrace();
                sessionInfo.close();
            }
        }
    }
    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {

    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionInfo sessionInfo = SessionInfo.get(session.getId());
        if(sessionInfo != null) {
            SessionInfo.remove(session.getId());
            System.out.println("RTP Sesson closed:" + session.getId());
        }
    }
}
