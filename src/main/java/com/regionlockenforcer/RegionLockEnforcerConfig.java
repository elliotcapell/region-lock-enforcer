package com.regionlockenforcer;

import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup(RegionLockEnforcerConfig.GROUP)
public interface RegionLockEnforcerConfig extends Config
{
    String GROUP = "regionlockenforcer";

    @ConfigItem(
            keyName = "disableTeleportFiltering",
            name = "Disable Teleport Filtering",
            description = "Disable all teleport filtering regardless of region config",
            position = 1
    )
    default boolean disableTeleportFiltering() { return false; }

    @ConfigItem(
            keyName = "disableBorder",
            name = "Disable Border",
            description = "Disable border drawing and tile click restrictions",
            position = 2
    )
    default boolean disableBorder() { return false; }

    @ConfigItem(
            keyName = "displayBorderOnWorldMap",
            name = "Display Border on World Map",
            description = "Show the border on the world map overlay",
            position = 3
    )
    default boolean displayBorderOnWorldMap() { return true; }

    @ConfigItem(
            keyName = "toggleEditor",
            name = "Toggle Editor Hotkey",
            description = "Enter/exit edit mode; shift-click to mark tiles",
            position = 4
    )
    default Keybind toggleEditor() { return new Keybind(KeyEvent.VK_F12, 0); }

    enum PropStyle
    {
        SEA_ROCK,
        ROCK_WALL,
        IRON_FENCE,
        LOG_FENCE
    }

    @ConfigItem(
            keyName = "worldMapGridSize",
            name = "World Map Grid Size",
            description = "Tile width/height for the world map selection grid",
            position = 5
    )
    default int worldMapGridSize() { return 4; }
}
