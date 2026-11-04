# Mini-Git Java

A small, in-memory Java model of a Git-like commit history, written by Tianlin Yang.
It stores commit messages and timestamps in a linked list and supports adding,
finding, deleting, and merging commits. It does not track files or invoke Git.

## Features

- Create commits with unique IDs and update the repository head.
- Read recent history from newest to oldest and find commits by ID.
- Drop commits while preserving the rest of the history.
- Merge two repositories by timestamp, moving the donor's commits into the receiver.

## Build

Requirements: JDK 17 or newer (including `jshell`) and Maven 3.6.3 or newer.

Maven manages the JUnit 5 test dependency and provides one build command for local
development and CI. Source files use Maven's standard layout:

- `src/main/java/Repository.java`: repository implementation.
- `src/test/java/RepositoryTest.java`: JUnit 5 tests.

From the repository root:

```sh
mvn package
```

This compiles the code, runs the tests, and creates
`target/mini-git-java-1.0-SNAPSHOT.jar`. The project is a library with no `main`
method; the JAR is not an executable application.

## Run a demo

From the repository root, use JShell to load the source and try the API:

```sh
jshell --feedback concise <<'EOF'
/open src/main/java/Repository.java
Repository repo = new Repository("demo");
String first = repo.commit("Initial commit");
repo.commit("Add README");
System.out.println(repo.getHistory(2));
System.out.println("Dropped initial commit: " + repo.drop(first));
System.out.println("Commits remaining: " + repo.getRepoSize());
/exit
EOF
```

The history lists `Add README` before `Initial commit`, with generated IDs and
timestamps. The last two printed results are `Dropped initial commit: true` and
`Commits remaining: 1`.

## Run tests

```sh
mvn test
```

The JUnit 5 suite covers commit IDs, size and head updates, history traversal,
deletion, timestamp-ordered merges, empty repositories, and invalid inputs. It
retains the original four merge scenarios. GitHub Actions runs the same command
on pushes and pull requests with Java 17.

## Design notes

Each repository stores a head reference and a size. Each history node stores a `Commit` and a link to the next older node.
A commit retains its ID, message, and timestamp. Branch snapshots store ordered
commit references; checkout rebuilds history nodes without creating new commits. Adding a
commit replaces the head in O(1) time. Searching or dropping a commit walks the
list, taking O(n) time in the worst case.

`synchronize(other)` merges two histories by comparing their timestamps, newest
first. It reconnects the existing nodes without allocating new `Commit` nodes.
For histories of lengths n and m, this takes O(n + m) time and O(1) extra space.
Afterward, the donor is empty and the receiver owns the combined history. If two
timestamps are equal, the donor's commit is selected first.

Use two distinct repositories for synchronization. Commit IDs are generated in
memory for the current process; this model has no disk persistence.

## Background

This project began as coursework at the University of Washington.

## Branches

A new repository starts on `main`. `createBranch(name)` saves the current history
without switching. `listBranches()` returns an independent, alphabetically sorted
list. `getCurrentBranch()` returns the active name. `checkoutBranch(name)` saves
the current line and restores the requested one. Commit, drop, history, size, and
repository synchronization operate on the checked-out line.

Run this from the repository root:

```sh
jshell --feedback concise <<'EOF'
/open src/main/java/Repository.java
Repository repo = new Repository("branches");
String base = repo.commit("Base");
repo.createBranch("feature");
repo.checkoutBranch("feature");
String feature = repo.commit("Feature change");
repo.checkoutBranch("main");
System.out.println(repo.contains(feature));
repo.mergeBranch("feature");
System.out.println(repo.contains(feature));
System.out.println(repo.getRepoSize());
System.out.println(repo.listBranches());
System.out.println(repo.getCurrentBranch());
/exit
EOF
```

The printed results are `false`, `true`, `2`, `[feature, main]`, and `main`.
`mergeBranch(name)` merges by timestamp, newest first, preserving the source.
Shared commits appear once. Equal timestamps prefer the source branch; merging
itself is a no-op. Empty branches are supported. Null, blank, or duplicate names
are invalid for creation; checkout and merge require an existing branch. Invalid
operations throw `IllegalArgumentException` without changing history.

Snapshots and rebuilt list nodes isolate branch edits, including dropping shared
commits and switching branches after repository synchronization. Branch creation
and checkout take O(n) time/space; branch merge takes O(n + m) time/space using two
indices and a set of already-seen commits. This is a timestamp union of histories,
not Git's file merge or conflict resolution. There are no remote/push semantics.

The original 15 tests still cover repository operations. Branch tests cover
creation/listing, switching and head updates, invalid names, shared-commit merge,
empty branches, repeated/self merges, drop isolation, and synchronization with
saved branches. Tests use short waits only when asserting timestamp order.
