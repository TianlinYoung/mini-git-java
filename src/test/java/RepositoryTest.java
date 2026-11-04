// Tianlin Yang
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class RepositoryTest {
    private Repository repo1;
    private Repository repo2;

    // Occurs before each of the individual test cases
    // (creates new repos and resets commit ids)
    @BeforeEach
    public void setUp() {
        repo1 = new Repository("repo1");
        repo2 = new Repository("repo2");
        Repository.Commit.resetIds();
    }

    @Test
    public void testSynchronizeEmpty() throws InterruptedException {
        String[] messages = {"A", "B"};
        commitAll(repo1, messages);

        repo1.synchronize(repo2);

        assertEquals(2, repo1.getRepoSize());
        assertEquals(0, repo2.getRepoSize());
        assertEquals("1", repo1.getRepoHead());
        assertNull(repo2.getRepoHead());

        testHistory(repo1, 2, messages);
    }


    @Test
    public void testSynchronizeFront() throws InterruptedException {
        String[] repo1Messages = {"A", "B"};
        String[] repo2Messages = {"C", "D"};

        commitAll(repo1, repo1Messages);
        commitAll(repo2, repo2Messages);

        repo1.synchronize(repo2);

        assertEquals(4, repo1.getRepoSize());
        assertEquals(0, repo2.getRepoSize());
        assertEquals("3", repo1.getRepoHead());
        assertNull(repo2.getRepoHead());

        String[] allMessages = {"A", "B", "C", "D"};
        testHistory(repo1, 4, allMessages);
    }


    @Test
    public void testSynchronizeEnd() throws InterruptedException {
        String[] repo1Messages = {"New1", "New2"};
        String[] repo2Messages = {"Old1", "Old2"};

        commitAll(repo2, repo2Messages);
        commitAll(repo1, repo1Messages);

        repo1.synchronize(repo2);

        assertEquals(4, repo1.getRepoSize());
        assertEquals(0, repo2.getRepoSize());
        assertEquals("3", repo1.getRepoHead());
        assertNull(repo2.getRepoHead());

        String[] allMessages = {"Old1", "Old2", "New1", "New2"};
        testHistory(repo1, 4, allMessages);
    }


    @Test
    public void testSynchronizeMiddle() throws InterruptedException {
        repo1.commit("A");
        Thread.sleep(2);

        repo2.commit("B");
        Thread.sleep(2);

        repo1.commit("C");
        Thread.sleep(2);

        repo1.synchronize(repo2);

        assertEquals(3, repo1.getRepoSize());
        assertEquals(0, repo2.getRepoSize());
        assertEquals("2", repo1.getRepoHead());
        assertNull(repo2.getRepoHead());

        String[] allMessages = {"A", "B", "C"}; // oldest -> newest
        testHistory(repo1, 3, allMessages);
    }


    @Test
    public void testCommitUpdatesHeadAndSize() {
        String first = repo1.commit("First");

        assertEquals(first, repo1.getRepoHead());
        assertEquals(1, repo1.getRepoSize());
        assertTrue(repo1.contains(first));

        String second = repo1.commit("Second");

        assertNotEquals(first, second);
        assertEquals(second, repo1.getRepoHead());
        assertEquals(2, repo1.getRepoSize());
        assertTrue(repo1.contains(first));
        assertTrue(repo1.contains(second));
        assertFalse(repo1.contains("missing"));
        checkHistory(repo1, 2, new String[]{second, first}, new String[]{"Second", "First"});
        assertEquals("repo1 - Current head: " + repo1.getHistory(1), repo1.toString());
    }

    @Test
    public void testHistoryLimitAndOversizedRequest() {
        String first = repo1.commit("First");
        String second = repo1.commit("Second");
        String third = repo1.commit("Third");

        checkHistory(repo1, 1, new String[]{third}, new String[]{"Third"});
        checkHistory(repo1, 2, new String[]{third, second}, new String[]{"Third", "Second"});
        checkHistory(repo1, 10, new String[]{third, second, first},
                     new String[]{"Third", "Second", "First"});
        assertEquals(third, repo1.getRepoHead());
        assertEquals(3, repo1.getRepoSize());
    }

    @Test
    public void testDropMiddle() {
        String first = repo1.commit("First");
        String second = repo1.commit("Second");
        String third = repo1.commit("Third");

        assertTrue(repo1.drop(second));

        assertEquals(2, repo1.getRepoSize());
        assertEquals(third, repo1.getRepoHead());
        assertFalse(repo1.contains(second));
        assertTrue(repo1.contains(first));
        assertTrue(repo1.contains(third));
        checkHistory(repo1, 3, new String[]{third, first}, new String[]{"Third", "First"});
    }

    @Test
    public void testDropTail() {
        String first = repo1.commit("First");
        String second = repo1.commit("Second");
        String third = repo1.commit("Third");

        assertTrue(repo1.drop(first));

        assertEquals(2, repo1.getRepoSize());
        assertEquals(third, repo1.getRepoHead());
        assertFalse(repo1.contains(first));
        checkHistory(repo1, 3, new String[]{third, second}, new String[]{"Third", "Second"});
    }

    @Test
    public void testDropHeadAndCommitAgain() {
        String first = repo1.commit("First");
        String second = repo1.commit("Second");

        assertTrue(repo1.drop(second));

        assertEquals(first, repo1.getRepoHead());
        assertEquals(1, repo1.getRepoSize());
        assertFalse(repo1.contains(second));
        checkHistory(repo1, 2, new String[]{first}, new String[]{"First"});

        String third = repo1.commit("Third");

        assertEquals(third, repo1.getRepoHead());
        assertEquals(2, repo1.getRepoSize());
        checkHistory(repo1, 2, new String[]{third, first}, new String[]{"Third", "First"});
    }

    @Test
    public void testDropOnlyCommit() {
        String first = repo1.commit("First");

        assertTrue(repo1.drop(first));

        assertEquals(0, repo1.getRepoSize());
        assertNull(repo1.getRepoHead());
        assertFalse(repo1.contains(first));
        assertEquals("", repo1.getHistory(1));
        assertEquals("repo1 - No commits", repo1.toString());
        assertFalse(repo1.drop(first));

        String second = repo1.commit("Second");

        assertEquals(second, repo1.getRepoHead());
        assertEquals(1, repo1.getRepoSize());
        checkHistory(repo1, 2, new String[]{second}, new String[]{"Second"});
    }

    @Test
    public void testDropUnknownIdLeavesHistoryUnchanged() {
        String first = repo1.commit("First");
        String second = repo1.commit("Second");
        String history = repo1.getHistory(2);

        assertFalse(repo1.drop("missing"));

        assertEquals(second, repo1.getRepoHead());
        assertEquals(2, repo1.getRepoSize());
        assertEquals(history, repo1.getHistory(2));
        assertTrue(repo1.contains(first));
        assertTrue(repo1.contains(second));
    }

    @Test
    public void testEmptyRepository() {
        assertNull(repo1.getRepoHead());
        assertEquals(0, repo1.getRepoSize());
        assertEquals("repo1 - No commits", repo1.toString());
        assertEquals("", repo1.getHistory(1));
        assertEquals("", repo1.getHistory(10));
        assertFalse(repo1.contains("missing"));
        assertFalse(repo1.drop("missing"));
        assertNull(repo1.getRepoHead());
        assertEquals(0, repo1.getRepoSize());
    }

    @Test
    public void testInvalidInputsLeaveStateUnchanged() {
        String first = repo1.commit("First");
        String other = repo2.commit("Other");
        String history = repo1.getHistory(1);
        String otherHistory = repo2.getHistory(1);

        assertThrows(IllegalArgumentException.class, () -> new Repository(null));
        assertThrows(IllegalArgumentException.class, () -> new Repository(""));
        assertThrows(IllegalArgumentException.class, () -> repo1.commit(null));
        assertThrows(IllegalArgumentException.class, () -> repo1.contains(null));
        assertThrows(IllegalArgumentException.class, () -> repo1.drop(null));
        assertThrows(IllegalArgumentException.class, () -> repo1.synchronize(null));
        assertThrows(IllegalArgumentException.class, () -> repo1.getHistory(0));
        assertThrows(IllegalArgumentException.class, () -> repo1.getHistory(-1));

        assertEquals(first, repo1.getRepoHead());
        assertEquals(1, repo1.getRepoSize());
        assertEquals(history, repo1.getHistory(1));
        assertEquals(other, repo2.getRepoHead());
        assertEquals(1, repo2.getRepoSize());
        assertEquals(otherHistory, repo2.getHistory(1));
    }

    @Test
    public void testSynchronizeIntoEmptyRepository() throws InterruptedException {
        commitAll(repo2, new String[]{"A", "B"});
        String head = repo2.getRepoHead();
        String history = repo2.getHistory(2);

        repo1.synchronize(repo2);

        assertEquals(2, repo1.getRepoSize());
        assertEquals(head, repo1.getRepoHead());
        assertEquals(history, repo1.getHistory(2));
        assertTrue(repo1.contains("0"));
        assertTrue(repo1.contains("1"));
        assertEquals(0, repo2.getRepoSize());
        assertNull(repo2.getRepoHead());
        assertEquals("", repo2.getHistory(1));
        assertFalse(repo2.contains("0"));
        assertFalse(repo2.contains("1"));

        String next = repo1.commit("C");
        String other = repo2.commit("New start");

        assertEquals(next, repo1.getRepoHead());
        assertEquals(3, repo1.getRepoSize());
        checkHistory(repo1, 3, new String[]{next, "1", "0"}, new String[]{"C", "B", "A"});
        assertEquals(other, repo2.getRepoHead());
        assertEquals(1, repo2.getRepoSize());
        checkHistory(repo2, 2, new String[]{other}, new String[]{"New start"});
    }

    @Test
    public void testSynchronizeBothEmpty() {
        repo1.synchronize(repo2);

        assertEquals(0, repo1.getRepoSize());
        assertNull(repo1.getRepoHead());
        assertEquals("", repo1.getHistory(1));
        assertEquals(0, repo2.getRepoSize());
        assertNull(repo2.getRepoHead());
        assertEquals("", repo2.getHistory(1));
    }

    // Checks IDs and messages listed from newest to oldest, including oversized requests.
    private void checkHistory(Repository repo, int n, String[] ids, String[] messages) {
        String[] history = repo.getHistory(n).split("\n");
        assertEquals(ids.length, history.length);
        assertEquals(ids.length, messages.length);
        for (int i = 0; i < history.length; i++) {
            assertTrue(history[i].startsWith(ids[i] + " at "), history[i]);
            assertTrue(history[i].endsWith(": " + messages[i]), history[i]);
        }
    }

    // Adds messages from oldest to newest, with time between commits for merge tests.
    private void commitAll(Repository repo, String[] messages) throws InterruptedException {
        // Commit all of the provided messages
        for (String message : messages) {
            int size = repo.getRepoSize();
            repo.commit(message);

            // Make sure exactly one commit was added to the repo
            assertEquals(size + 1, repo.getRepoSize(),
                         String.format("Size not correctly updated after committing message [%s]",
                                       message));

            // The merge scenarios need different millisecond timestamps.
            Thread.sleep(2);
        }
    }

    // Checks the original merge scenarios, where IDs follow the creation order.
    private void testHistory(Repository repo, int n, String[] allCommits) {
        int totalCommits = repo.getRepoSize();
        assertTrue(n <= totalCommits,
                   String.format("Provided n [%d] too big. Only [%d] commits",
                                 n, totalCommits));

        String[] nCommits = repo.getHistory(n).split("\n");

        assertEquals(n, nCommits.length);
        assertTrue(nCommits.length <= allCommits.length,
                   String.format("Not enough expected commits to check against. " +
                                 "Expected at least [%d]. Actual [%d]",
                                 n, allCommits.length));

        for (int i = 0; i < n; i++) {
            String commit = nCommits[i];

            // Old commit messages/ids are on the left and the more recent commit messages/ids are
            // on the right so need to traverse from right to left
            int backwardsIndex = totalCommits - 1 - i;
            String commitMessage = allCommits[backwardsIndex];

            assertTrue(commit.endsWith(": " + commitMessage),
                       String.format("Commit [%s] doesn't end with expected message [%s]",
                                     commit, commitMessage));
            assertTrue(commit.startsWith(backwardsIndex + " at "),
                       String.format("Commit [%s] doesn't start with expected id [%d]",
                                     commit, backwardsIndex));
        }
    }
}
