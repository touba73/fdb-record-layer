/*
 * IndexDeferredMaintenancePolicy.java
 *
 * This source file is part of the FoundationDB open source project
 *
 * Copyright 2015-2023 Apple Inc. and the FoundationDB project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apple.foundationdb.record.provider.foundationdb;

import com.apple.foundationdb.record.metadata.Index;

import java.util.HashSet;
import java.util.Set;

/**
 * Some store's indexes may need merging on some occasions. This helper module should allow the caller
 * to set and probe the merge policy and merge requests.
 */
public class IndexDeferredMaintenanceControl {
    private Set<Index> mergeRequiredIndexes = null;
    private boolean autoMergeDuringCommit = true;
    private long mergesLimit = 0;
    private long mergesFound;
    private long mergesTried;
    private long timeQuotaMillis;
    private long sizeQuotaBytes;

    /**
     * Return a set of indexes that need a deferred index merge operation. This function may be used by the
     * caller, to check which index maintainer requested a deferred merge.
     * @return set of indexes to be merged, null if no merge was requested.
     */
    public synchronized Set<Index> getMergeRequiredIndexes() {
        return mergeRequiredIndexes;
    }

    /**
     * Indicate to the caller if a deferred merge operation is required. This function is used by the index maintainer.
     * @param mergeRequiredIndex an index that should be merged
     */
    public synchronized void setMergeRequiredIndexes(final Index mergeRequiredIndex) {
        if (mergeRequiredIndexes == null) {
            this.mergeRequiredIndexes = new HashSet<>();
        }
        this.mergeRequiredIndexes.add(mergeRequiredIndex);
    }

    /**
     * If thrue, indicates to the index maintenance to automatically merge indexes during commit.
     * @return true if should merge
     */
    public boolean shouldAutoMergeDuringCommit() {
        return autoMergeDuringCommit;
    }

    /**
     * Indicate to the index maintenance to automatically merge indexes during commit (if applicable).
     * If the user sets it to false, they are responsible to call, possibly in the background, the {@link OnlineIndexer#mergeIndex()}
     * function with the set of indexes returned by {@link #getMergeRequiredIndexes()} as target indexes.
     * @param autoMergeDuringCommit if true (default) and applicable, automatically merge during commit
     */
    public void setAutoMergeDuringCommit(final boolean autoMergeDuringCommit) {
        this.autoMergeDuringCommit = autoMergeDuringCommit;
    }

    /**
     * Limit the number of merges that may be attempted in a single transaction.
     * @return the max merges allowed
     */
    public long getMergesLimit() {
        return mergesLimit;
    }

    /**
     * Set by the caller - see {@link #getMergesLimit()}.
     * @param mergesLimit the max merges allowed
     */
    public void setMergesLimit(final long mergesLimit) {
        this.mergesLimit = mergesLimit;
    }

    /**
     * Report the number of merges found.
     * @return number of merges found
     */
    public long getMergesFound() {
        return mergesFound;
    }

    /**
     * Set by the merger - see {@link #getMergesFound()}.
     * @param mergesFound number of merges found
     */
    public void setMergesFound(final long mergesFound) {
        this.mergesFound = mergesFound;
    }

    /**
     * Report the number of merges attempted in a single transaction.
     * @return number of merges tried
     */
    public long getMergesTried() {
        return mergesTried;
    }

    /**
     * Set by the merger - see {@link #getMergesTried()}.
     * @param mergesTried number of merges tried
     */
    public void setMergesTried(final long mergesTried) {
        this.mergesTried = mergesTried;
    }

    /**
     * If bigger than 0, define a time quota for agility context (i.e. auto-commit).
     * @return time quota in milliseconds
     */
    public long getTimeQuotaMillis() {
        return timeQuotaMillis;
    }

    /**
     * Set by the caller - if applicable, request to auto-commit after this time quota.
     * @param timeQuotaMillis time quota in milliseconds
     */
    public void setTimeQuotaMillis(final long timeQuotaMillis) {
        this.timeQuotaMillis = timeQuotaMillis;
    }

    /**
     * If bigger than 0, define a size quota for agility context (i.e. auto-commit).
     * @return size quota in bytes
     */
    public long getSizeQuotaBytes() {
        return sizeQuotaBytes;
    }

    /**
     * Set by the caller - if applicable, request to auto-commit after this size quota.
     * @param sizeQuotaBytes size quota in bytes
     */
    public void setSizeQuotaBytes(final long sizeQuotaBytes) {
        this.sizeQuotaBytes = sizeQuotaBytes;
    }
}
