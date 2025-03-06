package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.util.IMusicHandler;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.FadeableSoundInstance;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;

import java.util.HashSet;
import java.util.Set;

public class FireBossMusicHandler implements IMusicHandler {

    enum Instrument {
        DRUMS_A(new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_DRUMS_A.get(), SoundSource.RECORDS, false)),
        DRUMS_B(new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_DRUMS_B.get(), SoundSource.RECORDS, false)),
        //        DRUMS_C(),
        BASS_A(new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_BASS_A.get(), SoundSource.RECORDS, false)),
        //        BASS_B(),
        //        BASS_C(),
        CHOIR(new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_CHOIR.get(), SoundSource.RECORDS, false)),
        ORCHESTRA(new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_ORCHESTRA.get(), SoundSource.RECORDS, false)),
        //        ORCHESTRA_STABS(),
        MELODY_A(new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_MELODY.get(), SoundSource.RECORDS, false)),
        //        MELODY_B(),
        BELL_A(new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_BELLS_A.get(), SoundSource.RECORDS, false)),
//        BELL_B(),
//        REVERSE(),
        ;

        final FadeableSoundInstance sound;

        Instrument(FadeableSoundInstance sound) {
            this.sound = sound;
        }
    }

    Instrument[][] MUSIC = {
            {Instrument.DRUMS_B, Instrument.CHOIR, Instrument.BELL_A},
            {Instrument.DRUMS_B, Instrument.CHOIR, Instrument.MELODY_A, Instrument.BELL_A},
            {Instrument.DRUMS_B, Instrument.CHOIR, Instrument.BELL_A},
            {Instrument.DRUMS_B, Instrument.CHOIR, Instrument.BELL_A},
            {Instrument.DRUMS_A, Instrument.BASS_A},
            {Instrument.DRUMS_A, Instrument.BASS_A},
            {Instrument.DRUMS_A, Instrument.CHOIR, Instrument.ORCHESTRA, Instrument.BASS_A},
            {Instrument.DRUMS_B, Instrument.CHOIR, Instrument.ORCHESTRA},
            {Instrument.DRUMS_B, Instrument.CHOIR, Instrument.ORCHESTRA},
            {Instrument.DRUMS_B, Instrument.CHOIR, Instrument.BELL_A},
            {Instrument.DRUMS_B, Instrument.CHOIR, Instrument.MELODY_A, Instrument.BELL_A},
            {Instrument.DRUMS_B, Instrument.CHOIR},
            {Instrument.DRUMS_B, Instrument.CHOIR},
            {Instrument.DRUMS_A, Instrument.BASS_A},
            {Instrument.DRUMS_A, Instrument.BASS_A},
            {Instrument.DRUMS_A, Instrument.CHOIR, Instrument.ORCHESTRA, Instrument.BASS_A}
    };

    Set<FadeableSoundInstance> layers = new HashSet<>();
    static int musicIndex;
    static final Long SECTION_MILIS = 8000L;
    private long lastMilisPlayed;
    private long milisStarted;

    final SoundManager soundManager;

    public FireBossMusicHandler() {
        this.soundManager = Minecraft.getInstance().getSoundManager();
    }

    private void addLayer(FadeableSoundInstance soundInstance) {
        layers.stream().filter((sound) -> sound.isStopped() || !soundManager.isActive(sound)).toList().forEach(layers::remove);
        soundManager.play(soundInstance);
        layers.add(soundInstance);
    }

    public void stopLayers() {
        layers.forEach(FadeableSoundInstance::triggerStop);
    }

    @Override
    public void hardStop() {
        layers.forEach(soundManager::stop);
    }

    @Override
    public void triggerResume() {
        layers.forEach((sound) -> {
            sound.triggerStart();
            if (!soundManager.isActive(sound)) {
                soundManager.play(sound);
            }
        });
    }

    @Override
    public void init() {
        musicIndex = 0;
        playCurrentSheet();
        lastMilisPlayed = System.currentTimeMillis();
        milisStarted = System.currentTimeMillis();
    }

    @Override
    public void stop() {
        stopLayers();
    }

    @Override
    public void tick() {
        if (lastMilisPlayed <= System.currentTimeMillis() - SECTION_MILIS) {
            lastMilisPlayed = System.currentTimeMillis();
            musicIndex = (musicIndex + 1) % MUSIC.length;
            playCurrentSheet();
        }
    }

    private void playCurrentSheet() {
        IronsSpellbooks.LOGGER.debug("FIRE BOSS MUSIC {}/{}\t{}", musicIndex, MUSIC.length, System.currentTimeMillis() - milisStarted);
        Instrument[] instruments = MUSIC[musicIndex];
        for (Instrument instrument : instruments) {
            IronsSpellbooks.LOGGER.debug("\tplaying {}", instrument.toString());
            instrument.sound.unstop(); // sound instances are static and may be in an undefined state due to other bosses // fixme: i think the real solution is don't use static sound references...
            addLayer(instrument.sound);
        }
    }

    @Override
    public boolean isDone() {
        for (FadeableSoundInstance soundInstance : layers) {
            if (!soundInstance.isStopped() && soundManager.isActive(soundInstance)) {
                return false;
            }
        }
        return true;
    }
}
