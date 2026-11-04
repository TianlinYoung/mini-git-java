import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class BranchTest {
    private Repository repo;

    @BeforeEach
    public void setUp() {
        Repository.Commit.resetIds();
        repo = new Repository("demo");
    }

    @Test
    public void testEmptyBranchesAndDefensiveList() {
        assertEquals("main", repo.getCurrentBranch());
        repo.createBranch("work");
        List<String> names = repo.listBranches();
        assertEquals(Arrays.asList("main", "work"), names);
        names.clear();
        assertEquals(2, repo.listBranches().size());
        repo.checkoutBranch("work");
        assertNull(repo.getRepoHead());
        assertEquals(0, repo.getRepoSize());
        assertEquals("", repo.getHistory(5));
        assertFalse(repo.drop("missing"));
        repo.mergeBranch("main");
        assertNull(repo.getRepoHead());
        assertEquals("work", repo.getCurrentBranch());
    }

    @Test
    public void testDivergentHistoriesAndCheckout() {
        String base = repo.commit("base");
        repo.createBranch("work");
        String main = repo.commit("main change");
        String mainHistory = repo.getHistory(5);
        repo.checkoutBranch("work");
        assertEquals(base, repo.getRepoHead());
        assertFalse(repo.contains(main));
        String work = repo.commit("work change");
        String workHistory = repo.getHistory(5);
        repo.checkoutBranch("main");
        assertEquals(mainHistory, repo.getHistory(5));
        assertEquals(main, repo.getRepoHead());
        assertFalse(repo.contains(work));
        repo.checkoutBranch("work");
        assertEquals(workHistory, repo.getHistory(5));
        assertEquals(2, repo.getRepoSize());
        assertTrue(repo.toString().contains("work change"));
        repo.checkoutBranch("work");
        assertEquals(workHistory, repo.getHistory(5));
    }

    @Test
    public void testDropIsolation() {
        String base = repo.commit("base");
        String middle = repo.commit("middle");
        repo.commit("head");
        String original = repo.getHistory(5);
        repo.createBranch("work");
        assertTrue(repo.drop(middle));
        assertEquals(2, repo.getRepoSize());
        repo.checkoutBranch("work");
        assertEquals(original, repo.getHistory(5));
        assertTrue(repo.contains(middle));
        assertTrue(repo.drop(base));
        repo.checkoutBranch("main");
        assertTrue(repo.contains(base));
        assertFalse(repo.contains(middle));
        assertEquals(2, repo.getRepoSize());
    }

    @Test
    public void testMergeDeduplicatesAndPreservesSource() throws InterruptedException {
        String base = repo.commit("base");
        repo.createBranch("work");
        Thread.sleep(5);
        String main = repo.commit("main change");
        repo.checkoutBranch("work");
        Thread.sleep(5);
        String work = repo.commit("work change");
        String sourceHistory = repo.getHistory(5);
        repo.checkoutBranch("main");
        repo.mergeBranch("work");
        assertEquals(3, repo.getRepoSize());
        assertEquals(work, repo.getRepoHead());
        assertTrue(repo.contains(base));
        assertTrue(repo.contains(main));
        assertTrue(repo.contains(work));
        String[] lines = repo.getHistory(5).split("\n");
        assertTrue(lines[0].endsWith(": work change"));
        assertTrue(lines[1].endsWith(": main change"));
        assertTrue(lines[2].endsWith(": base"));
        String merged = repo.getHistory(5);
        repo.mergeBranch("work");
        assertEquals(merged, repo.getHistory(5));
        assertEquals(3, repo.getRepoSize());
        repo.mergeBranch("main");
        assertEquals(merged, repo.getHistory(5));
        repo.checkoutBranch("work");
        assertEquals(sourceHistory, repo.getHistory(5));
        assertFalse(repo.contains(main));
        repo.checkoutBranch("main");
        assertEquals(merged, repo.getHistory(5));
    }

    @Test
    public void testEmptySourceAndEmptyReceiver() {
        repo.createBranch("empty");
        String first = repo.commit("first");
        repo.mergeBranch("empty");
        assertEquals(first, repo.getRepoHead());
        assertEquals(1, repo.getRepoSize());
        repo.checkoutBranch("empty");
        repo.mergeBranch("main");
        assertEquals(first, repo.getRepoHead());
        assertEquals(1, repo.getRepoSize());
        repo.drop(first);
        repo.checkoutBranch("main");
        assertEquals(first, repo.getRepoHead());
        assertEquals(1, repo.getRepoSize());
    }

    @Test
    public void testInvalidNamesDoNotChangeHistory() {
        repo.commit("base");
        String history = repo.getHistory(5);
        String[] badNames = {null, "", "   ", "main"};
        for (String name : badNames) {
            assertThrows(IllegalArgumentException.class, () -> repo.createBranch(name));
        }
        String[] missing = {null, "", "missing"};
        for (String name : missing) {
            assertThrows(IllegalArgumentException.class, () -> repo.checkoutBranch(name));
            assertThrows(IllegalArgumentException.class, () -> repo.mergeBranch(name));
        }
        assertEquals(history, repo.getHistory(5));
        assertEquals("main", repo.getCurrentBranch());
        assertEquals(Arrays.asList("main"), repo.listBranches());
    }

    @Test
    public void testSynchronizeWithSavedBranches() throws InterruptedException {
        repo.commit("receiver");
        repo.createBranch("saved");
        Repository donor = new Repository("donor");
        Thread.sleep(5);
        String other = donor.commit("donor");
        donor.createBranch("saved");
        repo.synchronize(donor);
        assertEquals(2, repo.getRepoSize());
        assertEquals(other, repo.getRepoHead());
        assertEquals(0, donor.getRepoSize());
        String merged = repo.getHistory(5);
        repo.checkoutBranch("saved");
        assertEquals(1, repo.getRepoSize());
        repo.checkoutBranch("main");
        assertEquals(merged, repo.getHistory(5));
        donor.checkoutBranch("saved");
        assertEquals(other, donor.getRepoHead());
        assertEquals(1, donor.getRepoSize());
        // Restoring the donor must not corrupt the receiver's active history.
        assertEquals(merged, repo.getHistory(5));
    }
}
