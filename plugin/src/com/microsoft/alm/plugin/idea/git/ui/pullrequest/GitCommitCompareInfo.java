// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.changes.Change;
import git4idea.GitCommit;
import git4idea.repo.GitRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A local replacement for {@code git4idea.util.GitCommitCompareInfo} which was removed from the platform.
 * Holds, per repository, the commits that differ between two branches and the total diff between them.
 */
public class GitCommitCompareInfo {

    public enum InfoType {
        BOTH,
        HEAD_TO_BRANCH,
        BRANCH_TO_HEAD
    }

    private final InfoType infoType;
    private final Map<GitRepository, Pair<List<GitCommit>, List<GitCommit>>> commitsInfo =
            new HashMap<GitRepository, Pair<List<GitCommit>, List<GitCommit>>>();
    private final Map<GitRepository, Collection<Change>> diffInfo = new HashMap<GitRepository, Collection<Change>>();

    public GitCommitCompareInfo() {
        this(InfoType.BOTH);
    }

    public GitCommitCompareInfo(final InfoType infoType) {
        this.infoType = infoType;
    }

    public InfoType getInfoType() {
        return infoType;
    }

    /**
     * @param commits a pair of (commits from HEAD to branch, commits from branch to HEAD)
     */
    public void put(final GitRepository repository, final Pair<List<GitCommit>, List<GitCommit>> commits) {
        commitsInfo.put(repository, commits);
    }

    public void put(final GitRepository repository, final Collection<Change> totalDiff) {
        diffInfo.put(repository, totalDiff);
    }

    public List<GitCommit> getHeadToBranchCommits(final GitRepository repository) {
        final Pair<List<GitCommit>, List<GitCommit>> commits = commitsInfo.get(repository);
        return commits != null ? commits.getFirst() : null;
    }

    public List<GitCommit> getBranchToHeadCommits(final GitRepository repository) {
        final Pair<List<GitCommit>, List<GitCommit>> commits = commitsInfo.get(repository);
        return commits != null ? commits.getSecond() : null;
    }

    public List<Change> getTotalDiff() {
        final List<Change> changes = new ArrayList<Change>();
        for (final Collection<Change> changeCollection : diffInfo.values()) {
            changes.addAll(changeCollection);
        }
        return changes;
    }

    public boolean isEmpty() {
        return commitsInfo.isEmpty();
    }
}
