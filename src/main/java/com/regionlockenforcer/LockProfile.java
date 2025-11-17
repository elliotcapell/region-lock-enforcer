package com.regionlockenforcer;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

@Data
public class LockProfile
{
    private boolean enabled = true;
    private List<WorldPoint> border = new ArrayList<>();
    private List<MenuBlockRule> menuRules = new ArrayList<>();
    private List<String> hideQuestRegex = new ArrayList<>();
    private List<String> hidePrayerRegex = new ArrayList<>();
    private List<String> hideSpellRegex = new ArrayList<>();
}
