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

import cc.cosmetica.core.api.AsyncApi;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.impl.CosmeticaSession;
import cc.cosmetica.cosmetica.Authentication;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.kupe.api.Text;
import gg.cloaks.javaclient.ApiException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletionException;

/**
 * Async API thing
 */
@Mixin(value = AsyncApi.class, remap = false)
public class AsyncApiMixin {
    @Inject(at = @At("HEAD"), method = "lambda$requestAsync$1")
    private static void a(Throwable t, CallbackInfoReturnable<Object> cir) {
        Throwable t1 = t;
        if (t1 instanceof CompletionException) {
            t1 = t.getCause();
        }

        if (t1 instanceof ApiException && (
                ((ApiException) t1).getCode() == 502 ||
                ((ApiException) t1).getCode() == 503 ||
                ((ApiException) t1).getCode() == 504
        )) {
            if (Authentication.everAuthenticated.get()) {
                CosmeticaSession.deauthenticate(CosmeticaAPI.AuthChangeReason.ERROR_401);

                Cosmetica.showToast(
                        Text.translatable("toast.cosmetica.disconnected"),
                        Text.translatable("toast.cosmetica.disconnected.message")
                );

                Authentication.showedUnauthenticatedToast.set(true);
            }
        }
    }
}
