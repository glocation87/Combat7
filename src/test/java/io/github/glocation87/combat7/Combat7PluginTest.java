package io.github.glocation87.combat7;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class Combat7PluginTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void enablesAndDisablesCleanly() {
        Combat7Plugin plugin = MockBukkit.load(Combat7Plugin.class);
        assertTrue(plugin.isEnabled());

        server.getPluginManager().disablePlugin(plugin);
        assertFalse(plugin.isEnabled());
    }
}
