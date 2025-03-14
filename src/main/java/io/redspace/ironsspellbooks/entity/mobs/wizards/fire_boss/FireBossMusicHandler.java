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
import java.util.function.Function;

public class FireBossMusicHandler implements IMusicHandler {
    enum Instrument {
        MELODY_A(m -> m.melodyA),
        MELODY_B(m -> m.melodyB),
        BELLS_A(m -> m.bellsA),
        BELLS_B(m -> m.bellsB),
        DRUMS(m -> m.drums),
        BACKTRACK(m -> m.backtrack),
        ;

        final Function<FireBossMusicHandler, FadeableSoundInstance> sound;

        Instrument(Function<FireBossMusicHandler, FadeableSoundInstance> sound) {
            this.sound = sound;
        }
    }


    Instrument[][] MUSIC = {
            {Instrument.BELLS_A, /*Instrument.DRUMS,*/ /*Instrument.BACKTRACK,*/ Instrument.MELODY_A},
            {/*Instrument.DRUMS,*/ Instrument.BACKTRACK},
            {/*Instrument.BELLS_A,*/ Instrument.DRUMS, Instrument.BACKTRACK, Instrument.MELODY_A},
            {Instrument.DRUMS, Instrument.BACKTRACK},
            {Instrument.BELLS_B, Instrument.DRUMS, Instrument.BACKTRACK, Instrument.MELODY_A},
            {Instrument.DRUMS, Instrument.BACKTRACK},
            {Instrument.BELLS_B, Instrument.DRUMS, Instrument.BACKTRACK, Instrument.MELODY_B},
//            {Instrument.DRUMS, Instrument.BACKTRACK},
    };

    Set<FadeableSoundInstance> layers = new HashSet<>();
    static int musicIndex;
    static final int SECTION_LENGTH_TICKS = 20 * 8;
    int timer, runningTicks;
    boolean starting;
    final SoundManager soundManager;

    FadeableSoundInstance melodyA, melodyB, bellsA, bellsB, backtrack, drums;

    public FireBossMusicHandler() {
        this.soundManager = Minecraft.getInstance().getSoundManager();
        melodyA = new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_MELODY_A.get(), SoundSource.RECORDS, false);
        melodyB = new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_MELODY_B.get(), SoundSource.RECORDS, false);
        bellsA = new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_BELLS_A.get(), SoundSource.RECORDS, false);
        bellsB = new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_BELLS_B.get(), SoundSource.RECORDS, false);
        drums = new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_DRUMS.get(), SoundSource.RECORDS, false);
        backtrack = new FadeableSoundInstance(SoundRegistry.MUSIC_FIRE_BOSS_BACKTRACK.get(), SoundSource.RECORDS, false);
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
        musicIndex = -1;
//        playCurrentSheet();
        timer = 0;//SECTION_LENGTH_TICKS - 20;
        addLayer(melodyB);
        starting = true;
    }

    @Override
    public void stop() {
        stopLayers();
    }

    @Override
    public void tick() {
        runningTicks++;
        if (++timer >= SECTION_LENGTH_TICKS - 1) {
            timer = 0;
            musicIndex = (musicIndex + 1) % MUSIC.length;
            playCurrentSheet();
        }
    }

    private void playCurrentSheet() {
        starting = false;
        IronsSpellbooks.LOGGER.debug("FIRE BOSS MUSIC {}/{}\t{}", (musicIndex + 1), MUSIC.length, runningTicks / 20.0);
        Instrument[] instruments = MUSIC[musicIndex];
        for (Instrument instrument : instruments) {
            IronsSpellbooks.LOGGER.debug("\tplaying {}", instrument.toString());
            var sound = instrument.sound.apply(this);
            sound.unstop();
            addLayer(sound);
        }
    }

    @Override
    public boolean isDone() {
        if (starting) {
            return false; // starting
        }
        for (FadeableSoundInstance soundInstance : layers) {
            if (!soundInstance.isStopped() && soundManager.isActive(soundInstance)) {
                return false;
            }
        }
        return true;
    }
}
