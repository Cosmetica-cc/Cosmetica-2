package cc.cosmetica.cosmetica.mixin;

import cc.cosmetica.cosmetica.util.Thumbnail;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.cloaks.javaclient.model.Cosmetic;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Cosmetic.class)
public class JCACosmeticMixin implements Thumbnail {
    private static final String JSON_PROPERTY_THUMBNAIL = "thumbnail";
    private String thumbnail;

    @Override
    @JsonProperty(JSON_PROPERTY_THUMBNAIL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setThumbnail(String thumb) {
        thumbnail = thumb;
    }

    @Override
    @JsonProperty(JSON_PROPERTY_THUMBNAIL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getThumbnail() {
        return thumbnail;
    }
}
