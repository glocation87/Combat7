package io.github.glocation87.combat7;

import com.mojang.brigadier.Command;
import io.github.glocation87.combat7.clash.ClashTracker;
import io.github.glocation87.combat7.dash.AfterImages;
import io.github.glocation87.combat7.dash.DashService;
import io.github.glocation87.combat7.dash.DoubleTapDetector;
import io.github.glocation87.combat7.parry.ParryTracker;
import io.github.glocation87.combat7.shield.ShieldService;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public class Combat7Plugin extends JavaPlugin {
    private CombatConfig settings;
    private AfterImages afterImages;
    private ShieldService shields;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = CombatConfig.from(getConfig());

        afterImages = new AfterImages(this);
        shields = new ShieldService(this);
        DashService dashes = new DashService(this::settings, afterImages);
        CombatListener listener = new CombatListener(this::settings, new ParryTracker(), new ClashTracker(),
            new DoubleTapDetector(), dashes, shields);

        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getScheduler().runTaskTimer(this, shields::tick, 1L, 1L);
        registerCommands();
    }

    @Override
    public void onDisable() {
        if (shields != null) {
            shields.clear();
        }
        if (afterImages != null) {
            afterImages.clear();
        }
    }

    public CombatConfig settings() {
        return settings;
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            event.registrar().register(Commands.literal("combat7")
                .requires(source -> source.getSender().hasPermission("combat7.admin"))
                .then(Commands.literal("reload").executes(context -> {
                    reloadConfig();
                    settings = CombatConfig.from(getConfig());
                    context.getSource().getSender().sendRichMessage("<green>Combat7 config reloaded");
                    return Command.SINGLE_SUCCESS;
                }))
                .build(), "Combat7 admin commands"));
    }
}
