/*
 * Copyright 2024, 2025 Cosmetica
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

package cc.cosmetica.cosmetica.util;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Utility class for mapping by segments, rather than exact key-value.
 * Implemented as a red-black tree.
 */
public final class Division<T> {
    public Division() {
        this.redBlackTree = new TreeMap<>();
    }

    private final NavigableMap<Double, T> redBlackTree;
    private T min;
    private double minKey = Double.MAX_VALUE;

    /**
     * Add a new section with the given parameters.
     * @param minBound the minimum bound for the section (by key).
     * @param value the value stored in the section.
     * @return this division instance.
     */
    public Division<T> addSection(double minBound, T value) {
        this.redBlackTree.put(minBound, value);

        if (minBound < this.minKey) {
            this.minKey = minBound;
            this.min = value;
        }

        return this;
    }

    /**
     * Get the value associated with the given key in this division.
     * @param key the key to look up in the division.
     * @return the value associated with that key. That is, the value of the highest section below this, or the lowest
     * section if below all section's min bounds.
     */
    public T get(double key) {
        if (key <= this.minKey) {
            return this.min;
        }

        return this.redBlackTree.floorEntry(key).getValue();
    }
}