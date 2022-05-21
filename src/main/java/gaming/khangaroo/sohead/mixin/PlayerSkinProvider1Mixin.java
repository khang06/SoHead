package gaming.khangaroo.sohead.mixin;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;

import gaming.khangaroo.sohead.SoHeadMod;

// I can't mixin authlib, so this is the next best thing
@Mixin(targets = "net.minecraft.client.texture.PlayerSkinProvider$1")
public class PlayerSkinProvider1Mixin {
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    private static final String[] ALLOWED_DOMAINS = {
        ".minecraft.net",
        ".mojang.com",
        ".discordapp.com",
        ".discordapp.net",
        ".imgur.com"
    };

    private static boolean isDomainOnList(String domain, String[] list) {
        for (final String entry : list) {
            if (domain.endsWith(entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedTextureDomain(final String url) {
        URI uri;

        try {
            uri = new URI(url);
        } catch (final URISyntaxException ignored) {
            throw new IllegalArgumentException("Invalid URL '" + url + "'");
        }

        final String domain = uri.getHost();
        return isDomainOnList(domain, ALLOWED_DOMAINS);
    }

    @Redirect(method = "load(Ljava/lang/String;)Ljava/util/Map;", at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/minecraft/MinecraftSessionService;getTextures(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;"))
    private Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTexturesInjected(
            MinecraftSessionService sessionService, GameProfile profile, boolean requireSecure) {
        final Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);

        if (textureProperty == null) {
            return new HashMap<>();
        }

        final MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.getDecoder().decode(textureProperty.getValue()),
                    StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (final JsonParseException e) {
            SoHeadMod.LOGGER.error("Could not decode textures payload", e);
            return new HashMap<>();
        }

        if (result == null || result.getTextures() == null) {
            return new HashMap<>();
        }

        for (final Map.Entry<MinecraftProfileTexture.Type, MinecraftProfileTexture> entry : result.getTextures()
                .entrySet()) {
            final String url = entry.getValue().getUrl();
            if (!isAllowedTextureDomain(url)) {
                SoHeadMod.LOGGER.error("Textures payload contains blocked domain: {}", url);
                return new HashMap<>();
            }
        }

        return result.getTextures();
    }
}
