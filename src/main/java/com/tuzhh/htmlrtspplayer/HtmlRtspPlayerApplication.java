package com.tuzhh.htmlrtspplayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@SpringBootApplication
public class HtmlRtspPlayerApplication implements WebSocketConfigurer {

    @Bean
    public WebSockerServerRtspHandler handlerRtsp() {
        return new WebSockerServerRtspHandler();
    }
    @Bean
    public WebSockerServerRtpHandler handlerRtp() {
        return new WebSockerServerRtpHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handlerRtsp(), "/player/rtsp").setAllowedOrigins("*").addInterceptors(new WebSocketHandshakeInterceptor());
        registry.addHandler(handlerRtp(), "/player/rtp").setAllowedOrigins("*").addInterceptors(new WebSocketHandshakeInterceptor());
    }

    public static void main(String[] args) {
        SpringApplication.run(HtmlRtspPlayerApplication.class, args);
    }
}
