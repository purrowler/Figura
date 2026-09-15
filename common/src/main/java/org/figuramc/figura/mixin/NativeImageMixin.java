package org.figuramc.figura.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.figuramc.figura.ducks.NativeImageExtension;
import org.lwjgl.stb.STBImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public abstract class NativeImageMixin implements NativeImageExtension {
    @Shadow protected abstract boolean writeToChannel(WritableByteChannel channel) throws IOException;

    @Shadow protected abstract int getPixelABGR(int i, int j);

    @Shadow protected abstract void setPixelABGR(int i, int j, int k);

    @Unique
    public byte[] figura$asByteArray() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (WritableByteChannel channel = Channels.newChannel(out)) {
            if (!this.writeToChannel(channel))
                throw new IOException("Could not encode image: " + STBImage.stbi_failure_reason());
        }
        return out.toByteArray();
    }

    @Override
    public int figura$getPixelABGR(int i, int j) {
        return getPixelABGR(i, j);
    }

    @Override
    public void figura$setPixelABGR(int i, int j, int color) {
        setPixelABGR(i,j,color);
    }
}
