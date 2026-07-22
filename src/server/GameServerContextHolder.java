package server;

/**
 * Hands the GameServer instance being started to GameWebSocketBootstrap's @Bean method.
 * GameServer isn't component-scanned (it's a plain object, constructed directly by callers
 * and tests with per-instance constructor args like the accounts DB path), so it can't be
 * @Autowired the normal way - this is how it reaches the @Configuration class that needs it
 * to build the WebSocket handler. Set immediately before SpringApplicationBuilder.run() and
 * cleared right after; safe because servlet-context startup runs synchronously on that thread.
 */
final class GameServerContextHolder {
    private static final ThreadLocal<GameServer> CURRENT = new ThreadLocal<>();

    private GameServerContextHolder() { }

    static void set(GameServer gameServer) {
        CURRENT.set(gameServer);
    }

    static GameServer get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}
