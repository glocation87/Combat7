package io.github.glocation87.combat7.dash;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

// Vanilla draws an invisible entity translucent for players on the same team when that team can see friendly
// invisibles, that's the only way to get a see-through player model without a resource pack.
// Players already on a team are left alone so other plugins' teams keep working
final class GhostTeam {
    private static final String NAME = "combat7_ghosts";

    private final Set<Scoreboard> boards = new HashSet<>();

    void show(Entity ghost, Collection<? extends Player> viewers) {
        for (Player viewer : viewers) {
            Scoreboard board = viewer.getScoreboard();
            Team team = teamOn(board);
            if (board.getEntryTeam(viewer.getName()) == null) {
                team.addEntity(viewer);
            }
            team.addEntity(ghost);
        }
    }

    // Entries get saved with the scoreboard, so ghosts have to be taken off again
    void hide(Entity ghost) {
        for (Scoreboard board : boards) {
            Team team = board.getTeam(NAME);
            if (team != null) {
                team.removeEntity(ghost);
            }
        }
    }

    void clear() {
        for (Scoreboard board : boards) {
            Team team = board.getTeam(NAME);
            if (team != null) {
                team.unregister();
            }
        }
        boards.clear();
    }

    private Team teamOn(Scoreboard board) {
        Team team = board.getTeam(NAME);
        if (team == null) {
            team = board.registerNewTeam(NAME);
            team.setCanSeeFriendlyInvisibles(true);
        }
        boards.add(board);
        return team;
    }
}
