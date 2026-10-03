package io.github.glocation87.combat7.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Counts hits in a row on the same target, the chain drops after a pause or when you get hit
public final class ComboTracker {

    private record Combo(UUID target, int lastTick, int count) {
    }

    private final Map<UUID, Combo> combos = new HashMap<>();

    public int hit(UUID attacker, UUID target, int now, int windowTicks) {
        Combo current = combos.get(attacker);
        boolean continues = current != null && current.target().equals(target) && now - current.lastTick() <= windowTicks;
        int count = continues ? current.count() + 1 : 1;
        combos.put(attacker, new Combo(target, now, count));
        return count;
    }

    // Returns how long the combo was, 0 if there wasn't one
    public int broken(UUID id) {
        Combo combo = combos.remove(id);
        return combo == null ? 0 : combo.count();
    }

    public void forget(UUID id) {
        combos.remove(id);
    }
}
