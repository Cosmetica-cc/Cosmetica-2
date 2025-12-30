//package cc.cosmetica.cosmetica.mixin;
//
//import cc.cosmetica.core.api.texture.CosmeticaTexture;
//import cc.cosmetica.cosmetica.Cosmetica;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.ModifyArg;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//
//@Mixin(value = CosmeticaTexture.class, remap = false)
//public abstract class DebugMixin {
//
//    /**
//     * @reason a
//     * @author a
//     */
//    @ModifyArg(
//            method = "lambda$load$1",
//            index = 0,
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lcc/cosmetica/core/api/texture/CosmeticaTexture;readToPNG(Ljava/io/InputStream;Ljava/lang/String;I)Lcc/cosmetica/core/api/texture/CosmeticaTexture$AnimatedInputStream;")
//    )
//    private InputStream modifyStream(InputStream original) throws IOException {
//        return original;
////        ByteArrayOutputStream os = new ByteArrayOutputStream();
////        // Write the BufferedImage as PNG to the output stream
////        ImageIO.write(Cosmetica.cosmetica$debugimage, "webp", os);
////        // Convert the output stream to a byte array
////        byte[] imageBytes = os.toByteArray();
////        // Return a ByteArrayInputStream
////        return new ByteArrayInputStream(imageBytes);
//    }
//
//    /**
//     * @reason a
//     * @author a
//     */
//    @ModifyArg(
//            method = "loadCacheFile",
//            index = 0,
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lcc/cosmetica/core/api/texture/CosmeticaTexture;readToPNG(Ljava/io/InputStream;Ljava/lang/String;I)Lcc/cosmetica/core/api/texture/CosmeticaTexture$AnimatedInputStream;")
//    )
//    private InputStream modifyStream2(InputStream original) throws IOException {
//        return original;
////        ByteArrayOutputStream os = new ByteArrayOutputStream();
////        // Write the BufferedImage as PNG to the output stream
////        ImageIO.write(Cosmetica.cosmetica$debugimage, "webp", os);
////        // Convert the output stream to a byte array
////        byte[] imageBytes = os.toByteArray();
////        // Return a ByteArrayInputStream
////        return new ByteArrayInputStream(imageBytes);
//    }
//}
