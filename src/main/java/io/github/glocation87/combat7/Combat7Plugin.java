package io.github.glocation87.combat7;

import org.bukkit.plugin.java.JavaPlugin;

public class Combat7Plugin extends JavaPlugin {
    private CombatConfig settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = CombatConfig.from(getConfig());
    }

    public CombatConfig settings() {
        return settings;
    }
}
