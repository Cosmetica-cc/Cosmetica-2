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

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;

/**
 * Hack to read from SelfCosmeticsManager in core, which isn't exposed directly.
 */
public final class SelfCosmeticsReader {
    private SelfCosmeticsReader() {
    }

    public static Cosmetics getCosmetics() {
        return SELF_COSMETICS.getCosmetics(null);
    }

    // hack to access own cosmetics field
    private static final CosmeticManager SELF_COSMETICS = new SelfCosmeticManager();
}
