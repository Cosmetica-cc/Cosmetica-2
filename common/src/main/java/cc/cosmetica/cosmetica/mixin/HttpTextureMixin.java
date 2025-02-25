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

package cc.cosmetica.cosmetica.mixin;

import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.HttpTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;

/**
 * Enables HttpTexture to download WEBP files to PNGs.
 */
@Mixin(HttpTexture.class)
public abstract class HttpTextureMixin {
    @Shadow @Final private String urlString;

    @Shadow @Final @Nullable private File file;

    @Shadow @Nullable protected abstract NativeImage load(InputStream inputStream);

    @Shadow protected abstract void loadCallback(NativeImage arg);

    @Inject(at = @At(value = "INVOKE", ordinal = 0, target = "Lorg/apache/logging/log4j/Logger;debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"),
            method = "method_22801", cancellable = true)
    private void redirectToWebp(CallbackInfo ci) {
        // Assume all .webp files are webp, and ignore others.
        if (!this.urlString.endsWith(".webp"))
            return;

        // We are now only processing webps.
        if (this.file == null) {
            Logging.getInstance().warnOnce("null-file-webp", "Tried to download and convert WEBP at {} but no cache file!", this.urlString);
            return;
        }

        Cosmetica.downloadWebpToPng(this.urlString, this.file, this::load, this::loadCallback);
        ci.cancel();
    }
}
