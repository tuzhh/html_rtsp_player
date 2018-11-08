package com.tuzhh.htmlrtspplayer;

import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class LocalRtspService {
    private static final String NULL_STRING = "";
    private static final byte[] NULL_BYTE = {};

    private BlockingQueue<String> queueRtspEvent = new LinkedBlockingDeque<String>();
    private BlockingQueue<byte[]> queueRtpEvent = new LinkedBlockingDeque<byte[]>();

    private SessionInfo sessionInfo = null;
    private RtspSendThread rtspSendThread = null;
    private RtpSendThread rtpSendThread = null;
    private WebSocketSession rtspSession = null;
    private WebSocketSession rtpSession = null;

    private HashMap<String,String> hsmpCseq2Seq = new HashMap<String,String>();

    public LocalRtspService(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public void start() {
    }
    public void attachRtspChannel(WebSocketSession rtspSession) {
        this.rtspSession = rtspSession;
        if(rtspSendThread == null && rtspSession!= null) {
            rtspSendThread = new RtspSendThread();
            rtspSendThread.start();
        }
    }
    public void attachRtpChannel(WebSocketSession rtpSession) {
        this.rtpSession = rtpSession;
        if(rtpSendThread == null && rtpSession != null) {
            rtpSendThread = new RtpSendThread();
            rtpSendThread.start();
        }
    }
    public void close() {
        try {
            queueRtspEvent.put(NULL_STRING);
            queueRtpEvent.put(NULL_BYTE);
        } catch(Exception e) {

        }    }
    private class RtspSendThread extends Thread {
        public void run() {
            try {
                String msg;
                while((msg = queueRtspEvent.take()) != null) {
                    if(msg == NULL_STRING)
                        break;
                    String head = "";
                    List<String> lines = Utils.msg2lines(msg);
                    HashMap<String,String> hsmpVal = Utils.list2key(lines, ":", 1);
                    String cseq = hsmpVal.get("cseq".toLowerCase());

                    if(!StringUtils.isEmpty(cseq)) {
                        String seq = null;
                        synchronized (hsmpCseq2Seq) {
                            seq = hsmpCseq2Seq.get(cseq);
                            hsmpCseq2Seq.remove(cseq);
                        }
                        if (seq != null) {
                            int contentLength = msg.getBytes().length;
                            head = WebSocketConfig.PROXY_PROTOCOL + "/" + WebSocketConfig.PROXY_VERSION + " 200 OK" + "\r\n"
                                    + "seq: " + seq + "\r\n"
                                    + "contentLength: " + contentLength + "\r\n"
                                    + "\r\n";
                        }
                        rtspSession.sendMessage(new TextMessage(head + msg));
                        System.out.println("[Send to RTSP Channel]--" + rtspSession.getId() + "==============================\r\n" + head + msg);
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            System.out.println("RtspSendThread is down!");
        }
    }
    public class RtpSendThread extends Thread {
        public void run() {
            try {
                byte[] data;
                while((data = queueRtpEvent.take()) != null) {
                    if(data == NULL_BYTE)
                        break;
                    rtpSession.sendMessage(new BinaryMessage(data));
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            System.out.println("RtpSendThread is down!");
        }
    }
    private String send_msg = "";
    public void recvMsg(String msg) {
        send_msg += msg;
        int div;
        while((div = send_msg.indexOf("\r\n\r\n")) > 0) {
            div += 4;
            String header = send_msg.substring(0,div).trim();
            String body = send_msg.substring(div);
            if(!header.startsWith(WebSocketConfig.PROXY_PROTOCOL + "/" + WebSocketConfig.PROXY_VERSION)) {
                send_msg = body;
                break;
            }
            List<String> lines = Utils.msg2lines(header);
            HashMap<String,String> hsmpVal = Utils.list2key(lines, ":", 1);
            String seq = hsmpVal.get("seq".toLowerCase());
            String contentLength = hsmpVal.get("contentLength".toLowerCase());
            if(!StringUtils.isEmpty(contentLength)) {
                int expectLen = Integer.parseInt(contentLength);
                byte[] bodyBytes = body.getBytes();
                if(expectLen < bodyBytes.length)
                    return;
                String rtspMsg = new String(bodyBytes,0,expectLen);
                if(expectLen == bodyBytes.length) {
                    send_msg = "";
                } else {
                    send_msg = new String(bodyBytes,expectLen,bodyBytes.length - expectLen);
                }
                lines = Utils.msg2lines(rtspMsg);
                hsmpVal = Utils.list2key(lines, ":", 1);
                String cseq = hsmpVal.get("cseq".toLowerCase());
                if(!StringUtils.isEmpty(seq) && !StringUtils.isEmpty(cseq)) {
                    synchronized (hsmpCseq2Seq) {
                        hsmpCseq2Seq.put(cseq, seq);
                    }
                }
                sessionInfo.getRemoteRtspService().sendMsg(rtspMsg);
            }
        }
    }
    public void sendMsg(String msg) {
        if(!msg.startsWith("RTSP")) {
            System.out.println("=========================================[ERROR]" + msg.length() + "================================");
        }
        try {
            queueRtspEvent.put(msg);
        } catch(Exception e) {
        }
    }
    public void sendData(byte[] data) {
        try {
            queueRtpEvent.put(data);
        } catch(Exception e) {

        }
    }
    public void sendData(byte[] data, int offset, int len) {
        if(len == 0) return;
        byte[] newData = new byte[len];
        System.arraycopy(data,offset,newData,0,len);
        sendData(newData);
    }
}
