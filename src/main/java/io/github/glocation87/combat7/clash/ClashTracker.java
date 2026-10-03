package io.github.glocation87.combat7.clash;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

public final class ClashTracker {

    private record Hit(UUID victim, int tick, double damage) {
    }

    private final Map<UUID, Hit> lastHitBy = new HashMap<>();

    public void record(UUID attacker, UUID victim, int tick, double damage) {
        lastHitBy.put(attacker, new Hit(victim, tick, damage));
    }

    // If the victim hit the attacker within the window, that hit is used up and its damage returned so it can be undone
    public OptionalDouble takeCounter(UUID attacker, UUID victim, int now, int windowTicks) {
        Hit earlier = lastHitBy.get(victim);
        if (earlier == null || !earlier.victim().equals(attacker) || now - earlier.tick() > windowTicks) {
            return OptionalDouble.empty();
        }
        lastHitBy.remove(victim);
        return OptionalDouble.of(earlier.damage());
    }

    public void forget(UUID id) {
        lastHitBy.remove(id);
    }
}
