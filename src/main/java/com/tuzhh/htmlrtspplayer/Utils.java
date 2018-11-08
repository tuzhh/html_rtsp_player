package com.tuzhh.htmlrtspplayer;

import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Utils {
    public static List<String> msg2lines(String s) {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(s.getBytes(Charset.forName("utf8"))), Charset.forName("utf8")));
        String line;
        List<String> listLine = new ArrayList<String>();
        try {
            while ((line = br.readLine()) != null) {
                listLine.add(line);
            }
            br.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return listLine;

    }
    public static HashMap<String,String> list2key(List<String> lines, String div) {
        return list2key(lines,div, 0);
    }
    public static HashMap<String,String> list2key(List<String> lines, String div, int parseLineIdx) {
        HashMap<String,String> hsmpVal = new HashMap<String,String>();
        for(int i=parseLineIdx;i<lines.size();i++) {
            String line = lines.get(i).trim();
            if(line.length() == 0)
                continue;
            int divIdx = line.indexOf(div);
            if(divIdx <= 0)
                continue;
            String k = line.toLowerCase().substring(0,divIdx).trim();
            String v = line.substring(divIdx + div.length()).trim();
            hsmpVal.put(k,v);
        }
        return hsmpVal;
    }

    public static HashMap<String,String> parseUrl(String url) {
        HashMap<String,String> hsmpVal = new HashMap<String,String>();

        String protocol = url.split("://")[0];
        if(!StringUtils.isEmpty(protocol)) {
            String hostfull = url.split("://")[1].split("/")[0];
            hostfull = (hostfull.indexOf('@') > 0) ? hostfull.split("@")[1] : hostfull;
            String host = (hostfull.indexOf(':') >= 0) ? hostfull.split(":")[0] : hostfull;
            int port = (hostfull.indexOf(':') >= 0) ? Integer.parseInt(hostfull.split(":")[1]) : 554;
            String path = url.split("://")[1].split("^([^/]+)")[1];
            hsmpVal.put("url",url);
            hsmpVal.put("protocol",protocol);
            hsmpVal.put("host",host);
            hsmpVal.put("port",Integer.toString(port));
            hsmpVal.put("path",path);
            return hsmpVal;
        } else {
            return null;
        }
    }
}
