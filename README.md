# Kung Fu Chess

A real-time chess variant: there are no turns - each side can move any of its own resting
pieces at any moment, and moves resolve when the piece physically arrives at its destination
(not instantly on click), so two pieces can be in flight at once. Includes a local two-player
(hot-seat) Swing client and a networked multiplayer mode (accounts, matchmaking, private rooms,
spectating) backed by a Spring Boot WebSocket server.

## Requirements

- JDK 8 (the scripts below point `JAVA_HOME` at
  `C:\Program Files\Android\jdk\jdk-8.0.302.8-hotspot\jdk8u302-b08` - edit that line in each
  `.bat` file if your JDK 8 lives somewhere else).
- All third-party dependencies are already vendored as jars in `lib/`, so no internet access
  or dependency download is needed to build or run.
- Maven (optional) is only needed if you want to run `mvn test` / `mvn exec:java` instead of
  the `.bat` scripts, or to generate a Jacoco coverage report (see below).

## Running the game

### Local two-player (same machine, no server)

`App.java` is the local hot-seat entry point - both players share one board and one window,
taking turns clicking their own pieces. There's no dedicated script for it; run it from your
IDE, or from a terminal after building once with `run-tests.bat` or `run-server.bat`:

```
java -cp "target\classes;lib\*" App
```

### Networked two-player (or more, as spectators)

1. `run-server.bat` - builds the project and starts `server.GameServer` on port 8765. Leave
   this window open for as long as anyone is playing.
2. `run-network-app.bat` - connects to `localhost:8765`. Run this once per player (twice for a
   normal game), each in its own window. Log in with any username/password (a new username is
   registered on the spot); then either click **Play** to get matched with whoever else is
   waiting, or use **Room** to create/join a private room by code.

To connect to a server running on a different machine, run
`java -cp "target\classes;lib\*" NetworkApp <host> <port>` directly instead of
`run-network-app.bat` (which is hardcoded to `localhost:8765`).

### Tests

`run-tests.bat` compiles everything (including `src/test/java`) and runs the full JUnit 5
suite via the console launcher. If you have Maven installed, `mvn test` runs the same tests
and additionally produces a Jacoco coverage report at `target/site/jacoco/index.html`.

## Project layout

- `src/model`, `src/rules`, `src/engine` - the board/piece model, per-piece move legality, and
  the real-time move-resolution engine (all UI- and network-agnostic).
- `src/view` - Swing rendering, sprites, sound.
- `src/controller` - translates mouse clicks into engine commands for the local client.
- `src/server`, `src/net`, `src/client` - the WebSocket server (matchmaking, rooms, accounts)
  and the client-side connection used by `NetworkApp`.
- `src/bus` - a small pub/sub event bus (move logged, score changed, game started/ended) that
  `GameEngine` publishes to and both the local and networked UIs react to.
- `App.java` / `NetworkApp.java` - the two GUI entry points (local hot-seat vs. networked).
- `src/test/java` - the JUnit 5 test suite.
