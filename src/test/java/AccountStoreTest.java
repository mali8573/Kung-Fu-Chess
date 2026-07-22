import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import server.AccountStore;

import java.io.File;
import java.io.IOException;

public class AccountStoreTest {

    /** A fresh, throwaway accounts.db per test - keeps test accounts out of the real one and
     *  out of each other's way (same pattern GameServerTest uses for the same reason). */
    private static AccountStore newStore() throws IOException {
        File dbFile = File.createTempFile("kfc-accounts-unit-test", ".db");
        dbFile.deleteOnExit();
        return new AccountStore(dbFile.getAbsolutePath());
    }

    @Test
    public void newUsernameRegistersWithStartingElo() throws IOException {
        AccountStore store = newStore();
        AccountStore.LoginResult result = store.login("Alice", "secret");

        assertTrue(result.success);
        assertNull(result.reason);
        assertEquals(1200, result.elo);
    }

    @Test
    public void existingUsernameWithCorrectPasswordLogsInAgain() throws IOException {
        AccountStore store = newStore();
        store.login("Bob", "correct_password");

        AccountStore.LoginResult result = store.login("Bob", "correct_password");

        assertTrue(result.success);
        assertEquals(1200, result.elo);
    }

    @Test
    public void existingUsernameWithWrongPasswordIsRejected() throws IOException {
        AccountStore store = newStore();
        store.login("Carol", "correct_password");

        AccountStore.LoginResult result = store.login("Carol", "wrong_password");

        assertFalse(result.success);
        assertEquals("wrong_password", result.reason);
    }

    @Test
    public void winningAGameRaisesEloAndLosingLowersIt() throws IOException {
        AccountStore store = newStore();
        store.login("Winner", "pw");
        store.login("Loser", "pw");

        AccountStore.EloUpdate update = store.applyGameResult("Winner", "Loser");

        assertTrue(update.winnerElo > 1200, "the winner's ELO should have gone up from 1200");
        assertTrue(update.loserElo < 1200, "the loser's ELO should have gone down from 1200");
    }

    @Test
    public void eloChangeIsSymmetricBetweenEquallyRatedPlayers() throws IOException {
        AccountStore store = newStore();
        store.login("Even1", "pw");
        store.login("Even2", "pw");

        AccountStore.EloUpdate update = store.applyGameResult("Even1", "Even2");

        // Starting from the same rating, a K=32 win/loss is +16/-16.
        assertEquals(1216, update.winnerElo);
        assertEquals(1184, update.loserElo);
    }

    @Test
    public void eloRatingPersistsAcrossMultipleGames() throws IOException {
        AccountStore store = newStore();
        store.login("Regular", "pw");
        store.login("Opponent1", "pw");
        store.login("Opponent2", "pw");

        store.applyGameResult("Regular", "Opponent1");
        AccountStore.LoginResult afterOneWin = store.login("Regular", "pw");
        assertEquals(1216, afterOneWin.elo);

        store.applyGameResult("Regular", "Opponent2");
        AccountStore.LoginResult afterTwoWins = store.login("Regular", "pw");
        assertTrue(afterTwoWins.elo > afterOneWin.elo, "a second win should raise the rating further");
    }
}
