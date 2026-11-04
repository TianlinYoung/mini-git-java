// Tianlin Yang
// 02/11/2026
// CSE 123
// P1: Mini-Git
// TA: Aidan Jefferson Suen
// This class represents a repository that stores and manages a collection
// of commits. It provides functionality to add, remove, and access elements
// while, at the same time, maintains the rules and behavior defined by the program.
import java.util.*;
import java.text.SimpleDateFormat;

public class Repository {
    private String name;
    private HistoryNode head;
    private int size;
    private Map<String, List<Commit>> branches;
    private String currentBranch;

    // Behavior:
    //     Constructs a new empty Repository with the given name.
    // Exceptions:
    //     Throws IllegalArgumentException if name is null or empty.
    // Parameters:
    //     name - the name of the repository.
    public Repository(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.head = null;
        this.size = 0;
        this.branches = new TreeMap<>();
        this.currentBranch = "main";
        this.branches.put("main", new ArrayList<>());
    }


    // Behavior:
    //     Returns the id of the current head commit.
    //     Returns null if the repository has no commits.
    // Returns:
    //     The id of the head commit, or null if empty.
    public String getRepoHead() {
        if (this.head == null) {
            return null;
        }
        return this.head.commit.id;
    }


    // Behavior:
    //     Returns the number of commits currently stored in the repository.
    // Returns:
    //     The total number of commits in this repository.
    public int getRepoSize() {
        return this.size;
    }
    

    // Behavior:
    //     Returns a string representation of the repository.
    //     If there are no commits, returns "<name> - No commits".
    //     Otherwise, returns "<name> - Current head: <head commit string>".
    // Returns:
    //     A formatted string representation of this repository.
    public String toString() {
        if (head == null) {
            return name + " - No commits";
        }
        return this.name + " - Current head: " + this.head.commit.toString();
    }


    // Behavior:
    //     Checks whether a commit with the given targetId exists in the repository.
    // Exceptions:
    //     Throws IllegalArgumentException if targetId is null.
    // Parameters:
    //     targetId - the id of the commit to search for.
    // Returns:
    //     true if a matching commit exists, false otherwise.
    public boolean contains(String targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException();
        }
        HistoryNode curr = head;
        while(curr != null) {
            if (curr.commit.id.equals(targetId)) {
                return true;
            }
            curr = curr.past;
        }
        return false;
    }


    // Behavior:
    //     Returns a string containing the most recent n commits, ordered 
    //     from most recent to least recent, separated by newline characters (\n).
    //     If the repository has fewer than n commits, returns all commits.
    //     Returns an empty string if there are no commits.
    // Exceptions:
    //     Throws IllegalArgumentException if n is less than or equal to 0.
    // Parameters:
    //     n - the number of recent commits to retrieve.
    // Returns:
    //     A formatted string of up to n commit representations.
    public String getHistory(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }

        if (head == null) {
            return "";
        }

        int count = 1;
        String result = head.commit.toString();
        HistoryNode curr = head.past;
        while (curr != null && count < n) {
            result += "\n" + curr.commit.toString();
            curr = curr.past;
            count ++;
        }
        return result;
    }


    // Behavior:
    //     Creates a new commit with the given message and adds it
    //     as the new head of the repository.
    // Exceptions:
    //     Throws IllegalArgumentException if message is null.
    // Parameters:
    //     message - the commit message describing the changes.
    // Returns:
    //     The id of the newly created commit.
    public String commit(String message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }

        this.head = new HistoryNode(new Commit(message), head);
        this.size++;
        return this.head.commit.id;
    }


    // Behavior:
    //     Removes the commit with the given targetId from the repository,
    //     maintaining the rest of the commit history.
    // Exceptions:
    //     Throws IllegalArgumentException if targetId is null.
    // Parameters:
    //     targetId - the id of the commit to remove.
    // Returns:
    //     true if the commit was successfully removed,
    //     false if no commit with the given id exists.
    public boolean drop(String targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException();
        }

        if (head == null) {
            return false;
        }

        if (head.commit.id.equals(targetId)) {
            head = head.past;
            size --;
            return true;
        }

        HistoryNode curr = head;
        while (curr.past != null) {
            if (curr.past.commit.id.equals(targetId)) {
                curr.past = curr.past.past;
                size --;
                return true;
            }
            curr = curr.past;
        }
        return false;
    }


    // Behavior:
    //     Merges all commits from the other repository into this repository,
    //     maintaining chronological order by timeStamps (Most recent first).
    //     After merging, the other repository was made empty.
    // Exceptions:
    //     Throws IllegalArgumentException if other is null.
    // Parameters:
    //     other - the repository whose commits will be merged into this repository.
    public void synchronize(Repository other) {
        if (other == null) {
            throw new IllegalArgumentException();
        }

        if (other.head == null) {
            return;
        }

        if (this.head == null) {
            this.head = other.head;
            this.size = other.size;

            other.head = null;
            other.size = 0;
            return;
        }

        HistoryNode thisCurr = this.head;
        HistoryNode otherCurr = other.head;

        HistoryNode finalHead = null;
        HistoryNode finalCurr = null;
        
        if (thisCurr.commit.timeStamp > otherCurr.commit.timeStamp) {
            finalHead = thisCurr;
            thisCurr = thisCurr.past;
        } else {
            finalHead = otherCurr;
            otherCurr = otherCurr.past;
        }
        finalCurr = finalHead;

        while (thisCurr != null && otherCurr != null) {
            if (thisCurr.commit.timeStamp > otherCurr.commit.timeStamp) {
                finalCurr.past = thisCurr;
                thisCurr = thisCurr.past;
            } else {
                finalCurr.past = otherCurr;
                otherCurr = otherCurr.past;
            }
            finalCurr = finalCurr.past;
        }

        if (thisCurr != null) {
            finalCurr.past = thisCurr;
        } else {
            finalCurr.past = otherCurr;
        }

        this.head = finalHead;
        this.size = this.size + other.size;

        other.head = null;
        other.size = 0;
    }


    // Returns the name of the checked-out branch.
    public String getCurrentBranch() {
        return currentBranch;
    }

    // Returns branch names in alphabetical order. Changing the returned list
    // does not change the repository.
    public List<String> listBranches() {
        return new ArrayList<>(branches.keySet());
    }

    // Creates a branch at the current history without checking it out.
    // Throws IllegalArgumentException for null, blank, or duplicate names.
    public void createBranch(String branchName) {
        if (branchName == null || branchName.trim().isEmpty()
                || branches.containsKey(branchName)) {
            throw new IllegalArgumentException();
        }
        branches.put(branchName, snapshot());
    }

    // Saves the current history and checks out an existing branch.
    // Throws IllegalArgumentException if the branch does not exist.
    public void checkoutBranch(String branchName) {
        requireBranch(branchName);
        branches.put(currentBranch, snapshot());
        currentBranch = branchName;
        restore(branches.get(branchName));
    }

    // Merges an existing branch into the current branch, newest timestamp first.
    // Shared commits appear only once; equal timestamps prefer the source branch.
    // The source branch is preserved. Merging the current branch is a no-op.
    // Throws IllegalArgumentException if the source branch does not exist.
    public void mergeBranch(String branchName) {
        requireBranch(branchName);
        if (branchName.equals(currentBranch)) {
            return;
        }
        List<Commit> current = snapshot();
        List<Commit> source = branches.get(branchName);
        List<Commit> merged = new ArrayList<>();
        Set<Commit> seen = new HashSet<>();
        int i = 0;
        int j = 0;
        while (i < current.size() || j < source.size()) {
            Commit next;
            if (j == source.size() || (i < current.size()
                    && current.get(i).timeStamp > source.get(j).timeStamp)) {
                next = current.get(i);
                i++;
            } else {
                next = source.get(j);
                j++;
            }
            if (seen.add(next)) {
                merged.add(next);
            }
        }
        restore(merged);
    }

    // Branches save ordered commits. Each active history uses separate list
    // nodes so changing one history never rewires another repository or branch.
    // Checkout and merge preserve commit IDs and timestamps.
    private List<Commit> snapshot() {
        List<Commit> history = new ArrayList<>();
        HistoryNode curr = head;
        while (curr != null) {
            history.add(curr.commit);
            curr = curr.past;
        }
        return history;
    }

    private void restore(List<Commit> history) {
        head = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            head = new HistoryNode(history.get(i), head);
        }
        size = history.size();
    }

    private void requireBranch(String branchName) {
        if (branchName == null || !branches.containsKey(branchName)) {
            throw new IllegalArgumentException();
        }
    }


    private static class HistoryNode {
        public final Commit commit;
        public HistoryNode past;

        public HistoryNode(Commit commit, HistoryNode past) {
            this.commit = commit;
            this.past = past;
        }
    }

    /**
     * DO NOT MODIFY
     * A class that represents a single commit in the repository.
     * Commits are characterized by an identifier, a commit message,
     * and the time that the commit was made. A commit also stores
     * a reference to the immediately previous commit if it exists.
     *
     * Staff Note: You may notice that the comments in this 
     * class openly mention the fields of the class. This is fine 
     * because the fields of the Commit class are public. In general, 
     * be careful about revealing implementation details!
     */
    public static class Commit {

        private static int currentCommitID;

        /**
         * The time, in milliseconds, at which this commit was created.
         */
        public final long timeStamp;

        /**
         * A unique identifier for this commit.
         */
        public final String id;

        /**
         * A message describing the changes made in this commit.
         */
        public final String message;

        /**
         * A reference to the previous commit, if it exists. Otherwise, null.
         */
        public Commit past;

        /**
         * Constructs a commit object. The unique identifier and timestamp
         * are automatically generated.
         * @param message A message describing the changes made in this commit. Should be non-null.
         * @param past A reference to the commit made immediately before this
         *             commit.
         */
        public Commit(String message, Commit past) {
            this.id = "" + currentCommitID++;
            this.message = message;
            this.timeStamp = System.currentTimeMillis();
            this.past = past;
        }

        /**
         * Constructs a commit object with no previous commit. The unique
         * identifier and timestamp are automatically generated.
         * @param message A message describing the changes made in this commit. Should be non-null.
         */
        public Commit(String message) {
            this(message, null);
        }

        /**
         * Returns a string representation of this commit. The string
         * representation consists of this commit's unique identifier,
         * timestamp, and message, in the following form:
         *      "[identifier] at [timestamp]: [message]"
         * @return The string representation of this collection.
         */
        @Override
        public String toString() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
            Date date = new Date(timeStamp);

            return id + " at " + formatter.format(date) + ": " + message;
        }

        /**
        * Resets the IDs of the commit nodes such that they reset to 0.
        * Primarily for testing purposes.
        */
        public static void resetIds() {
            Commit.currentCommitID = 0;
        }
    }
}
