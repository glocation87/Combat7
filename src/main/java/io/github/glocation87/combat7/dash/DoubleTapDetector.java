package io.github.glocation87.combat7.dash;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DoubleTapDetector {
    private final Map<UUID, Set<DashDirection>> held = new HashMap<>();
    private final Map<UUID, Map<DashDirection, Integer>> lastPress = new HashMap<>();

    public Optional<DashDirection> update(UUID id, Set<DashDirection> pressed, int now, int windowTicks) {
        Set<DashDirection> before = held.getOrDefault(id, Set.of());
        Map<DashDirection, Integer> presses = lastPress.computeIfAbsent(id, key -> new EnumMap<>(DashDirection.class));

        Set<DashDirection> copy = EnumSet.noneOf(DashDirection.class);
        copy.addAll(pressed);
        held.put(id, copy);

        DashDirection tapped = null;
        for (DashDirection direction : pressed) {
            if (before.contains(direction)) {
                continue;
            }
            Integer previous = presses.put(direction, now);
            if (tapped == null && previous != null && now - previous <= windowTicks) {
                tapped = direction;
                // otherwise a third tap would pair with the second and dash again
                presses.remove(direction);
            }
        }
        return Optional.ofNullable(tapped);
    }

    public void forget(UUID id) {
        held.remove(id);
        lastPress.remove(id);
    }
}
