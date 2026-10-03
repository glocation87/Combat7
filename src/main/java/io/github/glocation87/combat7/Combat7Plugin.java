package io.github.glocation87.combat7;

import io.github.glocation87.combat7.clash.ClashTracker;
import io.github.glocation87.combat7.parry.ParryTracker;
import io.github.glocation87.combat7.shield.ShieldService;
import org.bukkit.plugin.java.JavaPlugin;

public class Combat7Plugin extends JavaPlugin {
    private CombatConfig settings;
    private ShieldService shields;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = CombatConfig.from(getConfig());

        shields = new ShieldService(this);
        CombatListener listener = new CombatListener(this::settings, new ParryTracker(), new ClashTracker(), shields);

        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getScheduler().runTaskTimer(this, shields::tick, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (shields != null) {
            shields.clear();
        }
    }

    public CombatConfig settings() {
        return settings;
    }
}
