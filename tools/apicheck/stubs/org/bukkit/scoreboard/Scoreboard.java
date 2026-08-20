package org.bukkit.scoreboard;

import java.util.Set;

public interface Scoreboard {
    Team registerNewTeam(String name);
    Team getTeam(String name);
    Set<Team> getTeams();
}
