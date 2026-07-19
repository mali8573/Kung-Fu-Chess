import client.GameClient;
import engine.GameSnapshot;
import engine.PieceSnapshot;
import net.SnapshotCodec;
import org.junit.jupiter.api.Test;
import server.GameServer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class GameServerTest {

    /** A fresh, throwaway accounts.db per test - keeps test accounts out of the real one and
     *  out of each other's way. */
    private static GameServer newServer() throws IOException {
        File dbFile = File.createTempFile("kfc-accounts-test", ".db");
        dbFile.deleteOnExit();
        return new GameServer(dbFile.getAbsolutePath());
    }

    /** Polls incoming messages until one satisfies the predicate, or fails after timeoutMs. */
    private String awaitMessage(BlockingQueue<String> queue, java.util.function.Predicate<String> matches,
                                 long timeoutMs, String description) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String message = queue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            if (message != null && matches.test(message)) return message;
        }
        fail("Timed out waiting for: " + description);
        return null; // unreachable
    }

    /** Logs in, then joins matchmaking, waiting for confirmation of each step before moving on -
     *  so in a two-player test, calling this for "white" before "black" guarantees queue order. */
    private void loginAndJoinQueue(GameClient client, BlockingQueue<String> queue,
                                    String username, String password) throws Exception {
        client.sendLogin(username, password);
        awaitMessage(queue, m -> m.startsWith("LOGIN_OK"), 2000, username + " logs in");
        client.sendPlay();
        awaitMessage(queue, m -> m.equals("SEARCHING"), 2000, username + " joins the queue");
    }

    @Test
    public void firstToQueueIsWhiteSecondIsBlack() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> queue2 = new LinkedBlockingQueue<>();
        GameClient client1 = GameClient.connect("localhost", port, queue1::add);
        GameClient client2 = GameClient.connect("localhost", port, queue2::add);

        loginAndJoinQueue(client1, queue1, "Alice", "pw1");
        loginAndJoinQueue(client2, queue2, "Bob", "pw2");

        awaitMessage(queue1, m -> m.equals("MATCH_FOUND"), 2000, "client1 gets matched");
        awaitMessage(queue1, m -> m.equals("COLOR white"), 2000, "client1 assigned white");
        awaitMessage(queue2, m -> m.equals("MATCH_FOUND"), 2000, "client2 gets matched");
        awaitMessage(queue2, m -> m.equals("COLOR black"), 2000, "client2 assigned black");

        assertEquals("white", client1.getAssignedColor());
        assertEquals("black", client2.getAssignedColor());

        client1.close();
        client2.close();
    }

    @Test
    public void moveFromOneClientIsBroadcastToBoth() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> queue2 = new LinkedBlockingQueue<>();
        GameClient white = GameClient.connect("localhost", port, queue1::add);
        GameClient black = GameClient.connect("localhost", port, queue2::add);

        loginAndJoinQueue(white, queue1, "Alice", "pw");
        loginAndJoinQueue(black, queue2, "Bob", "pw");
        awaitMessage(queue1, m -> m.equals("COLOR white"), 2000, "white assigned");
        awaitMessage(queue2, m -> m.equals("COLOR black"), 2000, "black assigned");

        // White pawn e2 -> e3 (one square forward).
        white.sendMove("e2", "e3");

        String finalState = awaitMessage(queue1, m -> m.startsWith("STATE") && "wP".equals(pieceAt(m, "e3")),
                3000, "pawn to land on e3");

        assertNull(pieceAt(finalState, "e2"), "e2 should be empty after the pawn left");
        // Both clients see the fact land, even if not necessarily the exact same tick
        // (elapsed/rest-cooldown fields keep advancing every broadcast, so byte-identical
        // strings aren't guaranteed - the meaningful fact, where the pawn is, is what matters).
        String blackSideState = awaitMessage(queue2, m -> m.startsWith("STATE") && "wP".equals(pieceAt(m, "e3")),
                3000, "black client also sees the landed pawn");
        assertEquals("wP", pieceAt(blackSideState, "e3"));

        white.close();
        black.close();
    }

    @Test
    public void matchedPlayersSeeEachOthersNameAndStartingElo() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> queue2 = new LinkedBlockingQueue<>();
        GameClient white = GameClient.connect("localhost", port, queue1::add);
        GameClient black = GameClient.connect("localhost", port, queue2::add);

        loginAndJoinQueue(white, queue1, "Alice", "secret");
        loginAndJoinQueue(black, queue2, "Bob", "secret2");

        awaitMessage(queue1, m -> m.equals("COLOR white"), 2000, "white assigned");
        awaitMessage(queue2, m -> m.equals("COLOR black"), 2000, "black assigned");
        awaitMessage(queue1, m -> m.equals("NAMES Alice:1200|Bob:1200"), 2000, "white sees both names+elo");
        awaitMessage(queue2, m -> m.equals("NAMES Alice:1200|Bob:1200"), 2000, "black sees both names+elo");

        white.close();
        black.close();
    }

    @Test
    public void wrongPasswordForAnExistingAccountIsRejected() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
        GameClient first = GameClient.connect("localhost", port, queue1::add);
        first.sendLogin("Carol", "correct_password");
        awaitMessage(queue1, m -> m.equals("LOGIN_OK 1200"), 2000, "first login registers the account");
        first.close();

        BlockingQueue<String> queue2 = new LinkedBlockingQueue<>();
        GameClient second = GameClient.connect("localhost", port, queue2::add);
        second.sendLogin("Carol", "wrong_password");
        awaitMessage(queue2, m -> m.equals("LOGIN_FAILED wrong_password"), 2000,
                "second login with the wrong password is rejected");
        assertNull(second.getAssignedColor(), "a rejected login must not receive a color");

        second.close();
    }

    @Test
    public void sameUsernameCannotLogInTwiceWhileFirstIsStillConnected() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
        GameClient first = GameClient.connect("localhost", port, queue1::add);
        first.sendLogin("Dana", "same_password");
        awaitMessage(queue1, m -> m.equals("LOGIN_OK 1200"), 2000, "first login is seated");

        // Same username AND the correct password this time - matching credentials alone must
        // not be enough to log in a second time while the first connection is still around.
        BlockingQueue<String> queue2 = new LinkedBlockingQueue<>();
        GameClient second = GameClient.connect("localhost", port, queue2::add);
        second.sendLogin("Dana", "same_password");
        awaitMessage(queue2, m -> m.equals("LOGIN_FAILED already_logged_in"), 2000,
                "a second login as the same already-connected username is rejected");
        assertNull(second.getAssignedColor(), "the rejected duplicate login must not receive a color");

        first.close();
        second.close();
    }

    @Test
    public void playerAloneInQueueGetsNoMatchAfterTimeout() throws Exception {
        GameServer gameServer = newServer();
        gameServer.setMatchmakingTimeoutMsForTesting(300); // don't make this test wait 60 real seconds
        int port = gameServer.start(0);

        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        GameClient client = GameClient.connect("localhost", port, queue::add);
        loginAndJoinQueue(client, queue, "Lonely", "pw");

        awaitMessage(queue, m -> m.equals("NO_MATCH"), 2000,
                "a lone queued player eventually gets told nobody was found");

        client.close();
    }

    @Test
    public void disconnectedPlayerIsAutoForfeitedAfterReconnectWindow() throws Exception {
        GameServer gameServer = newServer();
        gameServer.setReconnectWindowMsForTesting(300); // don't make this test wait 20 real seconds
        int port = gameServer.start(0);

        BlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> queue2 = new LinkedBlockingQueue<>();
        GameClient white = GameClient.connect("localhost", port, queue1::add);
        GameClient black = GameClient.connect("localhost", port, queue2::add);
        loginAndJoinQueue(white, queue1, "Alice", "pw");
        loginAndJoinQueue(black, queue2, "Bob", "pw");
        awaitMessage(queue1, m -> m.equals("COLOR white"), 2000, "white assigned");
        awaitMessage(queue2, m -> m.equals("COLOR black"), 2000, "black assigned");

        black.close(); // black disconnects mid-match

        awaitMessage(queue1, m -> m.startsWith("OPPONENT_DISCONNECTED"), 2000,
                "white is told the opponent disconnected");

        String finalState = awaitMessage(queue1, m -> m.startsWith("STATE") && isGameOverWithWinner(m, "white"),
                3000, "white is auto-declared the winner once the reconnect window expires");
        assertTrue(isGameOverWithWinner(finalState, "white"));

        white.close();
    }

    @Test
    public void disconnectedPlayerCanReconnectToTheSameMatchInTime() throws Exception {
        GameServer gameServer = newServer();
        gameServer.setReconnectWindowMsForTesting(5000); // plenty of time for this test to reconnect
        int port = gameServer.start(0);

        BlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> queue2 = new LinkedBlockingQueue<>();
        GameClient white = GameClient.connect("localhost", port, queue1::add);
        GameClient black = GameClient.connect("localhost", port, queue2::add);
        loginAndJoinQueue(white, queue1, "Alice", "pw");
        loginAndJoinQueue(black, queue2, "Bob", "pw");
        awaitMessage(queue1, m -> m.equals("COLOR white"), 2000, "white assigned");
        awaitMessage(queue2, m -> m.equals("COLOR black"), 2000, "black assigned");

        black.close();
        awaitMessage(queue1, m -> m.startsWith("OPPONENT_DISCONNECTED"), 2000,
                "white is told the opponent disconnected");

        BlockingQueue<String> queue2b = new LinkedBlockingQueue<>();
        GameClient blackAgain = GameClient.connect("localhost", port, queue2b::add);
        blackAgain.sendLogin("Bob", "pw");
        awaitMessage(queue2b, m -> m.equals("COLOR black"), 2000, "black rejoins the same match as the same color");
        awaitMessage(queue1, m -> m.equals("OPPONENT_RECONNECTED"), 2000, "white is told the opponent came back");

        white.close();
        blackAgain.close();
    }

    @Test
    public void roomCanBeCreatedAndJoinedByCode() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> hostQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> joinerQueue = new LinkedBlockingQueue<>();
        GameClient host = GameClient.connect("localhost", port, hostQueue::add);
        GameClient joiner = GameClient.connect("localhost", port, joinerQueue::add);

        host.sendLogin("Host", "pw");
        awaitMessage(hostQueue, m -> m.startsWith("LOGIN_OK"), 2000, "host logs in");
        host.sendCreateRoom();
        String created = awaitMessage(hostQueue, m -> m.startsWith("ROOM_CREATED "), 2000, "room code is handed out");
        String code = created.substring("ROOM_CREATED ".length()).trim();

        joiner.sendLogin("Joiner", "pw");
        awaitMessage(joinerQueue, m -> m.startsWith("LOGIN_OK"), 2000, "joiner logs in");
        joiner.sendJoinRoom(code);

        awaitMessage(hostQueue, m -> m.equals("MATCH_FOUND"), 2000, "host gets matched");
        awaitMessage(hostQueue, m -> m.equals("COLOR white"), 2000, "host (the room's creator) is white");
        awaitMessage(joinerQueue, m -> m.equals("MATCH_FOUND"), 2000, "joiner gets matched");
        awaitMessage(joinerQueue, m -> m.equals("COLOR black"), 2000, "joiner is black");

        host.close();
        joiner.close();
    }

    @Test
    public void joiningWithAnUnknownRoomCodeFails() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        GameClient client = GameClient.connect("localhost", port, queue::add);
        client.sendLogin("Wanderer", "pw");
        awaitMessage(queue, m -> m.startsWith("LOGIN_OK"), 2000, "logs in");

        client.sendJoinRoom("ZZZZ");
        awaitMessage(queue, m -> m.equals("ROOM_NOT_FOUND"), 2000, "an unknown room code is rejected");

        client.close();
    }

    @Test
    public void spectatorSeesTheMatchButCannotMovePieces() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> hostQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> joinerQueue = new LinkedBlockingQueue<>();
        GameClient host = GameClient.connect("localhost", port, hostQueue::add);
        GameClient joiner = GameClient.connect("localhost", port, joinerQueue::add);

        host.sendLogin("Host2", "pw");
        awaitMessage(hostQueue, m -> m.startsWith("LOGIN_OK"), 2000, "host logs in");
        host.sendCreateRoom();
        String code = awaitMessage(hostQueue, m -> m.startsWith("ROOM_CREATED "), 2000, "room created")
                .substring("ROOM_CREATED ".length()).trim();

        joiner.sendLogin("Joiner2", "pw");
        awaitMessage(joinerQueue, m -> m.startsWith("LOGIN_OK"), 2000, "joiner logs in");
        joiner.sendJoinRoom(code);
        awaitMessage(hostQueue, m -> m.equals("COLOR white"), 2000, "host assigned");
        awaitMessage(joinerQueue, m -> m.equals("COLOR black"), 2000, "joiner assigned");

        BlockingQueue<String> watcherQueue = new LinkedBlockingQueue<>();
        GameClient watcher = GameClient.connect("localhost", port, watcherQueue::add);
        watcher.sendLogin("Watcher", "pw");
        awaitMessage(watcherQueue, m -> m.startsWith("LOGIN_OK"), 2000, "watcher logs in");
        // The room already has both its players seated - joining it now, by the very same
        // JOIN_ROOM command, must fall back to spectating instead of rejecting the request.
        watcher.sendJoinRoom(code);

        awaitMessage(watcherQueue, m -> m.equals("SPECTATING"), 2000, "watcher is accepted as a spectator");
        awaitMessage(watcherQueue, m -> m.startsWith("NAMES Host2:"), 2000, "watcher sees both names");
        awaitMessage(watcherQueue, m -> m.startsWith("STATE"), 2000, "watcher sees the board");
        assertNull(watcher.getAssignedColor(), "a spectator is never assigned a color");

        // A spectator trying to move a piece must have zero effect on the game.
        watcher.sendMove("e2", "e3");
        Thread.sleep(300);
        String stateAfter = awaitMessage(watcherQueue, m -> m.startsWith("STATE"), 2000, "board keeps broadcasting");
        assertEquals("wP", pieceAt(stateAfter, "e2"), "the spectator's move must not have moved the pawn");
        assertNull(pieceAt(stateAfter, "e3"));

        host.close();
        joiner.close();
        watcher.close();
    }

    @Test
    public void joiningYourOwnRoomFails() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        GameClient host = GameClient.connect("localhost", port, queue::add);
        host.sendLogin("SoloHost", "pw");
        awaitMessage(queue, m -> m.startsWith("LOGIN_OK"), 2000, "logs in");
        host.sendCreateRoom();
        String code = awaitMessage(queue, m -> m.startsWith("ROOM_CREATED "), 2000, "room created")
                .substring("ROOM_CREATED ".length()).trim();

        host.sendJoinRoom(code);
        awaitMessage(queue, m -> m.equals("ROOM_NOT_FOUND"), 2000,
                "joining the room you yourself just created is rejected, not matched against yourself");

        host.close();
    }

    @Test
    public void doubleClickingPlayDoesNotQueueTheSameConnectionTwice() throws Exception {
        GameServer gameServer = newServer();
        int port = gameServer.start(0);

        BlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> queue2 = new LinkedBlockingQueue<>();
        GameClient first = GameClient.connect("localhost", port, queue1::add);
        GameClient second = GameClient.connect("localhost", port, queue2::add);

        first.sendLogin("Eager", "pw");
        awaitMessage(queue1, m -> m.startsWith("LOGIN_OK"), 2000, "first logs in");
        first.sendPlay();
        awaitMessage(queue1, m -> m.equals("SEARCHING"), 2000, "first joins the queue");
        first.sendPlay(); // the double click - must not add a second queue entry for the same connection

        second.sendLogin("Steady", "pw");
        awaitMessage(queue2, m -> m.startsWith("LOGIN_OK"), 2000, "second logs in");
        second.sendPlay();
        awaitMessage(queue2, m -> m.equals("SEARCHING"), 2000, "second joins the queue");

        awaitMessage(queue1, m -> m.equals("COLOR white"), 2000, "first is paired exactly once, as white");
        awaitMessage(queue2, m -> m.equals("COLOR black"), 2000, "second is paired with first, as black");

        first.close();
        second.close();
    }

    /** Decodes a "STATE\n..." broadcast and checks it declares the game over with the given
     *  winner color. */
    private static boolean isGameOverWithWinner(String stateMessage, String expectedWinner) {
        GameSnapshot snapshot = SnapshotCodec.decode(stateMessage.substring("STATE\n".length()));
        return snapshot.gameOver && expectedWinner.equals(snapshot.winner);
    }

    /** Decodes a "STATE\n..." broadcast and returns the piece code sitting at the given
     *  algebraic square, or null if nothing is there. */
    private static String pieceAt(String stateMessage, String square) {
        GameSnapshot snapshot = SnapshotCodec.decode(stateMessage.substring("STATE\n".length()));
        int col = square.charAt(0) - 'a';
        int row = 8 - (square.charAt(1) - '0');
        for (PieceSnapshot piece : snapshot.pieces) {
            if (Math.round(piece.row) == row && Math.round(piece.col) == col) return piece.pieceCode;
        }
        return null;
    }
}
