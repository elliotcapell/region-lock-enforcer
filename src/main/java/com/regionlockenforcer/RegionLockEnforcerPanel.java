package com.regionlockenforcer;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel for managing regions.
 */
@Singleton
public class RegionLockEnforcerPanel extends PluginPanel
{
    private final RegionConfigComponent borderComponent;

    @Inject
    public RegionLockEnforcerPanel(RegionLockEnforcerPlugin plugin, ConfigManager configManager, TeleportRegistry teleportRegistry)
    {
        super();
        // Remove all borders from the panel
        setBorder(null);
        // Set background to match RuneLite's dark theme
        setBackground(net.runelite.client.ui.ColorScheme.DARK_GRAY_COLOR);
        // Ensure no default borders are applied
        setOpaque(true);
        this.borderComponent = new RegionConfigComponent(plugin, configManager, teleportRegistry);
        add(borderComponent);
    }

    public RegionConfigComponent getBorderComponent()
    {
        return borderComponent;
    }
}
