package server;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * The Spring Boot application GameServer.start(port) boots. Registers the one and only
 * WebSocket endpoint at "/" - the single path GameClient/net.WebSocketClient ever connects to.
 */
@SpringBootApplication
@EnableWebSocket
class GameWebSocketBootstrap implements WebSocketConfigurer {

    @Bean
    GameWebSocketHandler gameWebSocketHandler() {
        return new GameWebSocketHandler(GameServerContextHolder.get());
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler(), "/");
    }
}
