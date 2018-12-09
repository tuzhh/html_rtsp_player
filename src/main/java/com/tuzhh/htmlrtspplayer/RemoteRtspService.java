package com.tuzhh.htmlrtspplayer;

import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class RemoteRtspService {
    private final static byte[] RES_HEAD = "RTSP/1.0 200 OK\r\n".getBytes();
    private final static String[] OP_NAME = {
            "OPTIONS",
            "DESCRIBE",
            "SETUP",
            "PLAY",
            "PAUSE",
            "ANNOUNCE",
            "RECORD",
            "GET_PARAMETER",
            "TEARDOWN",
    };

    private SessionInfo sessionInfo = null;
    private Socket socket;

    private HashMap<String,String> hsmpRtspMsg = new HashMap<String,String>();
    private BlockingQueue<EventItem> queueEvent = new LinkedBlockingDeque<EventItem>();

    public RemoteRtspService(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }
    public void start() {
        (new ExecThread()).start();
    }
    public void close() {
        try {
            EventItem eventItem = new EventItem();
            eventItem.eventType = EventItem.EVT_STOP_ALL;
            queueEvent.put(eventItem);
        } catch(Exception ee) {
        }
    }
    public void sendMsg(String msg) {
        try {
            EventItem eventItem = new EventItem();
            eventItem.eventType = EventItem.EVT_SEND_DATA;
            eventItem.msg = msg;
            queueEvent.put(eventItem);
        }catch(Exception e) {
        }
    }
    public void recvMsg(String msg) {
        sessionInfo.getLocalRtspService().sendMsg(msg);
    }
    public void recvData(byte[] data) {
        sessionInfo.getLocalRtspService().sendData(data,0,data.length);
    }
    private boolean connect(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            this.socket = socket;
            sessionInfo.setPort(port);
            sessionInfo.setHost(host);
            (new RecvThread(socket)).start();
            return true;
        } catch(Exception e) {
        }
        return false;
    }
    private boolean isConnected() {
        return (socket != null);
    }
    private static class EventItem {
        public final static int EVT_RECV_MSG = 0;
        public final static int EVT_SEND_DATA = 1;
        public final static int EVT_STOP_ALL = 2;
        public final static int EVT_STOP = 9;
        int eventType = EVT_RECV_MSG;
        String msg = null;
        byte[] data = null;
        String opName = null;
        Socket socket = null;
    }

    private class RecvThread extends Thread {
        private static final int DATA_UNK = -1;
        private static final int DATA_RTSP = 0;
        private static final int DATA_RTP = 1;
        private Socket socket;
        private boolean isTerminated = false;
        public RecvThread(Socket socket) {
            this.socket = socket;
        }
        public void run() {
            byte[] data = new byte[8192];
            int len;
            InputStream inputStream = null;
            byte[] buff = new byte[16384];
            int buffLen = 0;
            int dataType = DATA_UNK;
            int rtpDateLen = 0;
            int expectMsgDataLen = 0;
            try {
                inputStream = socket.getInputStream();
                while ((len = inputStream.read(data)) > 0) {
                    int offset = 0;
                    while(offset < len) {
                        switch (dataType) {
                            case DATA_UNK: {
                                boolean isFound = false;
                                for (int i = offset; i < len; i++) {
                                    if (data[i] != '$' && data[i] != 'R')
                                        continue;
                                    offset = i;
                                    isFound = true;
                                    break;
                                }
                                if (isFound) {
                                    buffLen = 0;
                                    expectMsgDataLen = 0;
                                    if(data[offset] == '$')
                                    {
                                        dataType = DATA_RTP;
                                    } else {
                                        dataType = DATA_RTSP;
                                    }
                                }
                                break;
                            }
                            case DATA_RTP: {
                                if(buffLen < 4) {
                                    int minLen = Math.min(len - offset, 4 - buffLen);
                                    System.arraycopy(data,offset,buff,buffLen,minLen);
                                    buffLen += minLen;
                                    offset += minLen;
                                }
                                if(buffLen < 4)
                                    break;
                                rtpDateLen = (buff[3] & 0x0FF) + (buff[2] & 0xFF) * 0x100;
                                if(rtpDateLen < 0) {
                                    System.out.println("Error data");
                                }
                                if(rtpDateLen > (buff.length - 4)) {
                                    //Adjust buff size
                                    byte[] newBuff = new byte[buff.length + 16384];
                                    System.arraycopy(buff,0,newBuff,0,buffLen);
                                    buff = newBuff;
                                }
                                int minLen = Math.min(rtpDateLen - (buffLen - 4),len - offset);
                                if(minLen > 0) {
                                    System.arraycopy(data,offset,buff,buffLen,minLen);
                                    offset += minLen;
                                    buffLen += minLen;
                                } else {
                                    System.out.println("Error data");
                                }
                                if(rtpDateLen == (buffLen - 4)) {
                                    byte[] d = new byte[buffLen];
                                    System.arraycopy(buff,0,d,0,buffLen);
                                    recvData(d);
                                    buffLen = 0;
                                    dataType = DATA_UNK;
                                }
                                break;
                            }
                            case DATA_RTSP: {
                                if(buffLen < 4) {
                                    int minLen = Math.min(len - offset, 4 - buffLen);
                                    System.arraycopy(data,offset,buff,buffLen,minLen);
                                    buffLen += minLen;
                                    offset += minLen;
                                    expectMsgDataLen = 0;
                                }
                                if(buffLen < 4)
                                    break;
                                if((buff.length - buffLen) < 8192) {
                                    //Adjust buff size
                                    byte[] newBuff = new byte[((int)((buff.length + 16384)/16384)) * 16384];
                                    System.arraycopy(buff,0,newBuff,0,buffLen);
                                    buff = newBuff;
                                }
                                for(int i=offset;i<len;i++) {
                                    if(buff.length == buffLen)
                                        break;
                                    buff[buffLen] = data[i];
                                    buffLen ++;
                                    offset ++;
                                    if(expectMsgDataLen == 0) {
                                        if (data[i] == '\n') {
                                            if (buff[ buffLen - 1] == '\n'
                                                    && buff[buffLen - 2] == '\r'
                                                    && buff[buffLen - 3] == '\n'
                                                    && buff[buffLen - 4] == '\r') {
                                                String msg = new String(buff, 0, buffLen);
                                                int div_first = msg.indexOf("\r\n\r\n");
                                                if (div_first < 0) {
                                                    continue;
                                                }
                                                List<String> lines = Utils.msg2lines(msg.substring(0, div_first));
                                                HashMap<String, String> hsmpVal = Utils.list2key(lines, ":", 1);
                                                String content_length = hsmpVal.get("Content-Length".toLowerCase());
                                                int contentLength = 0;
                                                if (!StringUtils.isEmpty(content_length)) {
                                                    contentLength = Integer.parseInt(content_length.trim());
                                                }
                                                expectMsgDataLen = buffLen + contentLength;
                                            }
                                        }
                                    }
                                    if(expectMsgDataLen > 0) {
                                        if(expectMsgDataLen == buffLen) {
                                            String msg = new String(buff, 0, buffLen);

                                            EventItem newEventItem = new EventItem();
                                            newEventItem.msg = msg;
                                            newEventItem.eventType = EventItem.EVT_RECV_MSG;
                                            queueEvent.put(newEventItem);
                                            expectMsgDataLen = 0;
                                            buffLen = 0;
                                            dataType = DATA_UNK;
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            } catch(Exception e) {
            }
            try {
                if(inputStream != null)
                    inputStream.close();
            } catch(Exception e) {
            }
            inputStream = null;
            System.out.println("RemoteRecvThread Is Down!");
            try {
                EventItem eventItem = new EventItem();
                eventItem.eventType = EventItem.EVT_STOP;
                eventItem.socket = socket;
                queueEvent.put(eventItem);
            } catch(Exception e) {
            }
        }
    }
    private class ExecThread extends Thread {
        public void run() {
            EventItem eventItem;
            String sendmsg = "";
            try {
                while((eventItem = queueEvent.take()) != null) {

                    switch(eventItem.eventType) {
                        case EventItem.EVT_RECV_MSG:
                        {
                            //1.如果是重定向，则重连socket
                            //2.将url恢复成原url
                            String rtspMsg = eventItem.msg;
                            System.out.println("[Recv From Remote]==================================================\r\n" + rtspMsg);
                            int div_first = rtspMsg.indexOf("\r\n\r\n");
                            if (div_first < 0) {
                                break;
                            }

                            List<String> lines = Utils.msg2lines(rtspMsg.substring(0,div_first));
                            HashMap<String, String> hsmpVal = Utils.list2key(lines, ":", 1);
                            String cseq = hsmpVal.get("CSeq".toLowerCase());
                            String rtspReq = null;
                            if(!StringUtils.isEmpty(cseq)) {
                                rtspReq = hsmpRtspMsg.get(cseq);
                                hsmpRtspMsg.remove(cseq);
                            }

                            if(lines.get(0).toLowerCase().matches(".+ moved([ ]+)temporarily([ ].*)*")) {
                                String redirectUrl = hsmpVal.get("Location".toLowerCase());
                                if(!StringUtils.isEmpty(redirectUrl)) {
                                    redirectUrl = redirectUrl.trim();

                                    HashMap<String,String> hsmpUrl = Utils.parseUrl(redirectUrl);
                                    if(StringUtils.isEmpty(hsmpVal.get("host"))) hsmpVal.put("host","127.0.0.1");

                                    try {
                                        EventItem newEventItem = new EventItem();
                                        newEventItem.eventType = EventItem.EVT_STOP;
                                        newEventItem.socket = socket;
                                        queueEvent.put(newEventItem);
                                    } catch(Exception e) {
                                    }
                                    System.out.println("Redirect to new url:" + redirectUrl + "*******************************************");
                                    if(connect(hsmpUrl.get("host"),Integer.parseInt(hsmpUrl.get("port")))) {
                                        sessionInfo.setRedirectUrl(redirectUrl);
                                        if(rtspReq != null) {
                                            EventItem newEventItem = new EventItem();
                                            newEventItem.eventType = EventItem.EVT_SEND_DATA;
                                            newEventItem.msg = rtspReq;
                                            queueEvent.put(newEventItem);
                                        }
                                    } else {
                                        EventItem newEventItem = new EventItem();
                                        newEventItem.eventType = EventItem.EVT_STOP_ALL;
                                        queueEvent.put(newEventItem);
                                    }
                                    break;
                                }
                            } else {
                                String newUrl = sessionInfo.getRedirectUrl();
                                if(newUrl != null) {
                                    String oldUrl = sessionInfo.getUrl();
                                    if(newUrl.endsWith("/")) newUrl = newUrl.substring(0,newUrl.length() - 1);
                                    if(oldUrl.endsWith("/")) oldUrl = oldUrl.substring(0,oldUrl.length() - 1);
                                    int idx;
                                    while((idx = rtspMsg.indexOf(newUrl)) >= 0) {
                                        rtspMsg = rtspMsg.substring(0,idx) + oldUrl + rtspMsg.substring(idx + newUrl.length());
                                    }
                                }
                                recvMsg(rtspMsg);
                            }
                        }
                        break;
                        case EventItem.EVT_SEND_DATA:
                        {
                            String msg = sendmsg + eventItem.msg;
                            if(sendmsg.length() < 2) {
                                msg = StringUtils.trimLeadingWhitespace(msg);
                            }
                            sendmsg = msg;
                            while(true) {
                                msg = sendmsg;
                                int div_first = msg.indexOf("\r\n\r\n");
                                if (div_first <= 0) {
                                    break;
                                }
                                List<String> lines = Utils.msg2lines(msg);
                                HashMap<String, String> hsmpVal = Utils.list2key(lines, ":", 1);
                                String content_length = hsmpVal.get("Content-Length".toLowerCase());
                                int contentLength = 0;
                                if (!StringUtils.isEmpty(content_length)) {
                                    contentLength = Integer.parseInt(content_length.trim());
                                }
                                int tailLength = msg.length() - div_first - 4;
                                if (tailLength < contentLength) {
                                    break;
                                }
                                int msgEndIdx = div_first + 4 + contentLength;
                                sendmsg = msg.substring(msgEndIdx);
                                String rtspMsg = msg.substring(0, msgEndIdx);

                                String cseq = hsmpVal.get("CSeq".toLowerCase());
                                if(!StringUtils.isEmpty(cseq)) hsmpRtspMsg.put(cseq,rtspMsg);

                                String opName = null;
                                String firstKey = rtspMsg.trim().split(" ")[0].toUpperCase();
                                for (String s : OP_NAME) {
                                    if (firstKey.equalsIgnoreCase(s)) {
                                        opName = s;
                                        break;
                                    }
                                }
                                if (opName != null) {
                                    if(sessionInfo.getUrl() == null) {
                                        String s = lines.get(0).trim();
                                        int idx = s.indexOf("rtsp://");
                                        if (idx > 0) {
                                            String url = s.substring(idx).split(" ")[0];

                                            HashMap<String, String> hsmpUrl = Utils.parseUrl(url);
                                            if (hsmpUrl != null) {
                                                if (StringUtils.isEmpty(hsmpUrl.get("host")))
                                                    hsmpUrl.put("host", "127.0.0.1");
                                                sessionInfo.setHost(hsmpUrl.get("host"));
                                                sessionInfo.setPort(Integer.parseInt(hsmpUrl.get("port")));
                                                sessionInfo.setUrl(url);
                                            }
                                        }
                                    }
                                    if (!isConnected()) {
                                        if (!StringUtils.isEmpty(sessionInfo.getHost()) && sessionInfo.getPort() > 0) {
                                            connect(sessionInfo.getHost(), sessionInfo.getPort());
                                        }
                                    }
                                    if(!isConnected()) {
                                        if (opName.equalsIgnoreCase("OPTIONS")) {
                                            String respMsg = "RTSP/1.0 200 OK\r\n" +
                                                    "CSeq: " + cseq + "\r\n" +
                                                    "Server: Wowza Streaming Engine 4.7.5.01 build21752\r\n" +
                                                    "Cache-Control: no-cache\r\n" +
                                                    "Public: DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS, ANNOUNCE, RECORD, GET_PARAMETER\r\n" +
                                                    "Supported: play.basic, con.persistent\r\n\r\n";
                                            recvMsg(respMsg);
                                        } else {
                                            String respMsg = "RTSP/1.0 401 UNKNOWN\r\n" +
                                                    "CSeq: " + cseq + "\r\n" +
                                                    "Server: Wowza Streaming Engine 4.7.5.01 build21752\r\n" +
                                                    "Supported: play.basic, con.persistent\r\n\r\n";
                                            recvMsg(respMsg);
                                        }
                                        continue;
                                    }
                                    if(sessionInfo.getUrl() != null) {
                                        String redirectUrl = sessionInfo.getRedirectUrl();
                                        if(!StringUtils.isEmpty(redirectUrl)) {
                                            if(redirectUrl.endsWith("/")) redirectUrl = redirectUrl.substring(0,redirectUrl.length() - 1);
                                            String url = sessionInfo.getUrl();
                                            if(url.endsWith("/")) url = url.substring(0,url.length() - 1);
                                            int idx;
                                            while((idx = rtspMsg.indexOf(url)) > 0) {
                                                rtspMsg = rtspMsg.substring(0,idx) + redirectUrl + rtspMsg.substring(idx + url.length());
                                            }
                                        }
                                        /*
                                        if("SETUP".equalsIgnoreCase(opName)) {
                                            int div = lines.get(0).toLowerCase().indexOf("trackID=".toLowerCase());
                                            if(div > 0) {
                                                String trackID = lines.get(0).substring(div + "trackID=".length()).split(" ")[0];
                                            }
                                        }
                                        */
                                    }
                                    try {
                                        System.out.println("[Send To Remote]===========================================================\r\n" + rtspMsg);
                                        socket.getOutputStream().write(rtspMsg.getBytes());
                                    } catch (Exception e) {
                                        EventItem newEventItem = new EventItem();
                                        newEventItem.eventType = EventItem.EVT_STOP_ALL;
                                        queueEvent.put(newEventItem);
                                        break;
                                    }
                                } else {
                                    String respMsg = "RTSP/1.0 401 UNKNOWN\r\n" +
                                            "CSeq: " + cseq + "\r\n" +
                                            "Server: Wowza Streaming Engine 4.7.5.01 build21752\r\n" +
                                            "Supported: play.basic, con.persistent\r\n\r\n";
                                    recvMsg(respMsg);
                                }
                            }
                        }
                        break;
                        case EventItem.EVT_STOP:
                        {
                            try {
                                eventItem.socket.close();
                            } catch(Exception e) {
                            }
                            if(eventItem.socket == socket) {
                                EventItem newEventItem = new EventItem();
                                newEventItem.eventType = EventItem.EVT_STOP_ALL;
                                queueEvent.put(newEventItem);
                                return;
                            } else {
                            }
                        }
                        break;
                        case EventItem.EVT_STOP_ALL:
                        {
                            if (socket != null) {
                                try {
                                    socket.close();
                                } catch (Exception e) {
                                }
                                socket = null;
                            }
                            sessionInfo.close();
                            return;
                        }
                    }
                }
            } catch(Exception e) {
            } finally {
                System.out.println("RemoteExecThread Is Down!");
            }
        }
    }
}
