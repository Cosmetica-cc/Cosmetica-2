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

package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.gui.Component;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

/**
 * State which only updates dependent components with a debouncing period.
 * @param <T> type stored in this state.
 */
public class DebounceState<T> extends State<T> {
    /**
     * Create a new debounced state.
     * @param initialValue the initial value.
     * @param debounceTime the time, in milliseconds, to debounce.
     */
    public DebounceState(T initialValue, long debounceTime) {
        super(initialValue);
        this.instant = new State<>(initialValue);
        this.debounceTime = debounceTime;
        this.time = System.currentTimeMillis() - debounceTime;

        if (debounceTime < 100) {
            throw new IllegalArgumentException("Debounce time must be at least 100ms");
        }
    }

    private final State<T> instant;

    private final long debounceTime;
    private long time;
    private volatile long lastUpdateId;

    /**
     * Acquire and get the instant state.
     * @param component the component which is acquiring the instant state.
     * @return the value.
     */
    public T acquireInstant(Component component) {
        return this.instant.acquire(component);
    }

    /**
     * Peek at the instant value of this state.
     * @return the instant value of this state.
     */
    @Override
    public T peek() {
        return this.instant.peek();
    }

    /**
     * Set this state, with debouncing.
     * @param value the value to set.
     */
    @Override
    public void set(T value) {
        // reject duplicates
        if (this.instant.peek().equals(value)) {
            return;
        }
        this.instant.set(value);

        // debounce queries to every 600ms
        final long theTime = System.currentTimeMillis();
        this.lastUpdateId = theTime;

        if (theTime - this.time > this.debounceTime) {
            super.set(this.instant.peek());
            this.time = theTime;
        } else {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(this.debounceTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Setting debounced state", e);
                }

                Minecraft.getInstance().execute(() -> {
                    // check still same latest value
                    if (this.lastUpdateId == theTime) {
                        super.set(this.instant.peek());
                        this.time = theTime;
                    }
                });
            });
        }
    }

    /**
     * Set this state now, bypassing debouncing.
     * @param value the new value to set.
     */
    public void setNow(T value) {
        this.instant.set(value);
        super.set(value);
    }
}
