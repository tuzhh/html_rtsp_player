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
    public HtmlRtspPlayerWebSockerServer htmlRtspPlayerWebSockerServer() {
        return new HtmlRtspPlayerWebSockerServer();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(htmlRtspPlayerWebSockerServer(), "/player").setAllowedOrigins("*").addInterceptors(new WebSocketHandshakeInterceptor());
    }

    public static void main(String[] args) {
        SpringApplication.run(HtmlRtspPlayerApplication.class, args);
    }
}
