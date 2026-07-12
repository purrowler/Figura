package org.figuramc.figura.ducks;

import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundSource;
import org.figuramc.figura.lua.api.sound.LuaSound;

import java.util.Collection;
import java.util.UUID;

public interface SoundEngineAccessor {

    void figura$addSound(LuaSound sound);
    void figura$stopSound(UUID owner, String name);
    void figura$stopAllSounds();
    void figura$releaseAlBuffers(Collection<SoundBuffer> buffers);
    ChannelAccess.ChannelHandle figura$createHandle(UUID owner, String name, Library.Pool pool);
    float figura$getVolume(SoundSource category);
    SoundBufferLibrary figura$getSoundBuffers();
    boolean figura$isPlaying(UUID owner);
    boolean figura$isEngineActive();
}
