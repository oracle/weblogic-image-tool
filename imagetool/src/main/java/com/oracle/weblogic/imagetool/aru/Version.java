// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.Arrays;

public class Version implements Comparable<Version> {
    private final int[] sequence;
    private final String stringValue;

    /**
     * Representation of the ARU version number used for Oracle products.
     * Version must be one or more integers separated by a period, ".".
     * @param value String to be parsed as the ARU version.
     */
    public Version(String value) {
        stringValue = value;

        if (value != null && !value.isEmpty()) {
            // split version into a sequence tokens using the period separator
            sequence = Arrays.stream(value.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
        } else {
            sequence = new int[1];
        }
    }

    /**
     * Return the sequence of version tokens padded to the minimum length with 0's.
     * The sequence will NOT be truncated.
     * @param minLength minimum number of version tokens in the array.
     * @return sequence of version tokens
     */
    public int[] getSequence(int minLength) {
        if (sequence.length < minLength) {
            return Arrays.copyOf(sequence, minLength);
        }
        return sequence;
    }

    /**
     * Compare this version number against the provided version, returning -1, 0, or 1 if
     * this version is less than, equal to, or greater than the provided version, respectively.
     * @param provided the object to be compared.
     * @return -1, 0, or 1 if this version is less than, equal to, or greater than the provided version
     */
    @Override
    public int compareTo(Version provided) {
        int match = 0;
        int sequenceLength = Math.max(sequence.length, provided.sequence.length);
        int[] mySequence = getSequence(sequenceLength);
        int[] providedSequence = provided.getSequence(sequenceLength);

        for (int i = 0; i < sequenceLength; i++) {
            if (mySequence[i] > providedSequence[i]) {
                match = 1;
            } else if (mySequence[i] < providedSequence[i]) {
                match = -1;
            }
            if (match != 0) {
                break;
            }
        }

        return match;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Version version = (Version) o;
        return this.compareTo(version) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sequence);
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
