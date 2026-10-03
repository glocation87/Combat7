package io.github.glocation87.combat7;

import io.github.glocation87.combat7.parry.ParryTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class Combat7Plugin extends JavaPlugin {
    private CombatConfig settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = CombatConfig.from(getConfig());

        CombatListener listener = new CombatListener(this::settings, new ParryTracker());
        getServer().getPluginManager().registerEvents(listener, this);
    }

    public CombatConfig settings() {
        return settings;
    }
}
