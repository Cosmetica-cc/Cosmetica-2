package cc.cosmetica.cosmetica.mixin;

import cc.cosmetica.core.render.texture.CosmeticaTexture;
import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//@Mixin(CosmeticaTexture.class)
//public class CosmeticaTextureDebugMixin {
//    @Shadow @Final private String url;
//
//    @Shadow private int frame;
//
//    @Shadow private int currentFrames;
//
//    @Shadow private NativeImage image;
//
//    @Shadow private int frameHeight;
//
//    @Inject(at = @At("HEAD"), method = "upload")
//    private void onUpload(NativeImage image, boolean close, CallbackInfo ci) {
//        System.out.println("(Cosmetica) uploading " + this.url + " (f: " + this.frame + " cf:" + this.currentFrames + " h:" + this.frameHeight + " im:" + this.image);
//    }
//}
