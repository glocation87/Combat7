package io.github.glocation87.combat7.parry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ParryTracker {
    // The client resends right click every 4 ticks while it's held, so a shorter gap means the button never came up
    private static final int HELD_REPEAT_TICKS = 5;

    private final Map<UUID, Integer> lastUse = new HashMap<>();
    private final Map<UUID, Integer> windowEnds = new HashMap<>();
    private final Map<UUID, Integer> readyAt = new HashMap<>();
    private final Map<UUID, Integer> staggerEnds = new HashMap<>();

    public boolean tryStart(UUID id, int now, int windowTicks, int cooldownTicks) {
        Integer previous = lastUse.put(id, now);
        if (previous != null && now - previous <= HELD_REPEAT_TICKS) {
            return false;
        }
        Integer ready = readyAt.get(id);
        if (ready != null && now < ready) {
            return false;
        }
        windowEnds.put(id, now + windowTicks);
        readyAt.put(id, now + cooldownTicks);
        return true;
    }

    public boolean isParrying(UUID id, int now) {
        Integer end = windowEnds.get(id);
        return end != null && now <= end;
    }

    // A clean parry refunds the cooldown so you can chain them
    public void succeeded(UUID id, int now) {
        windowEnds.remove(id);
        readyAt.put(id, now);
    }

    public void stagger(UUID id, int until) {
        staggerEnds.put(id, until);
    }

    public boolean isStaggered(UUID id, int now) {
        Integer end = staggerEnds.get(id);
        if (end == null) {
            return false;
        }
        if (now > end) {
            // mobs never quit, so expired entries have to clean themselves up
            staggerEnds.remove(id);
            return false;
        }
        return true;
    }

    public void forget(UUID id) {
        lastUse.remove(id);
        windowEnds.remove(id);
        readyAt.remove(id);
        staggerEnds.remove(id);
    }
}
