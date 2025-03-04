package io.redspace.ironsspellbooks.api.util;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;

@EventBusSubscriber
public class BossMusicManager {
    private static final Map<ResourceKey<Level>, BossMusicManager> MUSIC_MANAGERS = new HashMap<>();
    private final LinkedHashMap<UUID, IBossMusicHandler> musicHandlers = new LinkedHashMap<>();

    public static void createEvent(Entity entity, IBossMusicHandler event) {
        createEvent(entity.level.dimension(), entity.getUUID(), event);
    }

    public static void createEvent(ResourceKey<Level> dimension, UUID id, IBossMusicHandler event) {
        var manager = getManagerFor(dimension);
        manager.musicHandlers.put(id, event);
    }

    public static void stopEvent(Entity entity) {
        // while we only create events per-dimension, if something in any dimension calls for a specific uuid to be cancelled, we cancel it
        for (BossMusicManager manager : MUSIC_MANAGERS.values()) {
            if (manager.musicHandlers.containsKey(entity.getUUID())) {
                manager.musicHandlers.remove(entity.getUUID()).stop(entity);
            }
        }
    }

    private static BossMusicManager getManagerFor(ResourceKey<Level> dimension) {
        return MUSIC_MANAGERS.computeIfAbsent(dimension, (dim) -> new BossMusicManager());
    }

    public static void clear() {
        for (BossMusicManager m : MUSIC_MANAGERS.values()) {
            for (IBossMusicHandler<?> h : m.musicHandlers.values()) {
                h.hardStop();
            }
        }
        MUSIC_MANAGERS.clear();
    }

    @SubscribeEvent
    public static void fog(/*ViewportEvent.ComputeFogColor*/ event) {
        if (Minecraft.getInstance().player != null) {
            var manager = getManagerFor(Minecraft.getInstance().player.level.dimension());
            if (!manager.musicHandlers.isEmpty()) {
                IBossMusicHandler<?> fogEvent = manager.musicHandlers.lastEntry().getValue();
                fogEvent.tick();
            }
        }
    }
}
