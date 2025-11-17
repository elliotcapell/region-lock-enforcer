package com.regionlockenforcer;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.EnumComposition;
import net.runelite.api.ItemComposition;
import net.runelite.api.ParamID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
        name = "Region Lock Enforcer",
        description = "Editor marks tiles (darken + west edge) and blocks clicks on them."
)
@Singleton
public class RegionLockEnforcerPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private RegionLockEnforcerConfig config;
    @Inject private ConfigManager configManager;
    @Inject private OverlayManager overlayManager;
    @Inject private RegionLockOverlay overlay;
    @Inject private RegionLockWorldMapOverlay worldMapOverlay;
    @Inject private MouseManager mouseManager;
    @Inject private KeyManager keyManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private RegionLockEnforcerPanel panel;
    @Inject private TeleportRegistry teleportRegistry;
    @Inject private Gson gson;

    private NavigationButton navButton;

    // Region management
    @Getter private final List<Region> regions = new ArrayList<>();
    @Getter private Region currentRegion = null;

    // Current lock profile (contains menu rules, quest filters, etc.)
    @Getter private LockProfile currentProfile = new LockProfile();

    final EditorInput editor = new EditorInput();
    
    // Charter ship interface caching
    private int lastCharterShipWidgetId = -1; // Cache the widget ID once found
    private int charterShipSearchCooldown = 0; // Throttle widget searches
    
    // World map mouse listener reference for cleanup
    private java.awt.event.MouseListener worldMapMouseListener;

    @Override protected void startUp()
    {
        overlay.setPlugin(this);
        worldMapOverlay.setPlugin(this);
        overlayManager.add(overlay);
        overlayManager.add(worldMapOverlay);
        mouseManager.registerMouseListener(editor);
        // Register world map mouse listener on the client canvas
        clientThread.invokeLater(() ->
        {
            java.awt.Canvas canvas = client.getCanvas();
            if (canvas != null)
            {
                worldMapMouseListener = new java.awt.event.MouseAdapter()
                {
                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e)
                    {
                        if (worldMapOverlay.handleWorldMapClick(e))
                        {
                            e.consume(); // Consume the event if handled
                        }
                    }
                };
                canvas.addMouseListener(worldMapMouseListener);
            }
        });
        keyManager.registerKeyListener(editor.toggleEditHotkey);
        loadRegions();
        
        // Create and add side panel navigation button
        BufferedImage icon = loadPanelIcon();
        if (icon == null)
        {
            icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
        navButton = NavigationButton.builder()
                .tooltip("Region Lock Enforcer")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        
        // Refresh the region, border, and teleports lists in the panel
        if (panel.getBorderComponent() != null)
        {
            panel.getBorderComponent().refreshRegionList();
            panel.getBorderComponent().refreshBorderList();
            panel.getBorderComponent().refreshTeleportsList();
        }
        
        // Redraw spellbook to re-apply filtering when plugin is enabled
        redrawSpellbook();
        
        // Region Lock Enforcer started
    }

    @Override protected void shutDown()
    {
        saveRegions();
        overlayManager.remove(overlay);
        overlayManager.remove(worldMapOverlay);
        // Unregister world map mouse listener
        if (worldMapMouseListener != null)
        {
            clientThread.invokeLater(() ->
            {
                java.awt.Canvas canvas = client.getCanvas();
                if (canvas != null)
                {
                    canvas.removeMouseListener(worldMapMouseListener);
                }
            });
        }
        mouseManager.unregisterMouseListener(editor);
        keyManager.unregisterKeyListener(editor.toggleEditHotkey);
        clientToolbar.removeNavigation(navButton);
        
        // Redraw spellbook to restore all spells when plugin is disabled
        redrawSpellbook();
        
        // Region Lock Enforcer stopped
    }

    private BufferedImage loadPanelIcon()
    {
        try
        {
            return ImageUtil.loadImageResource(RegionLockEnforcerPlugin.class, "/panel_icon.png");
        }
        catch (Exception ex)
        {
            log.warn("Failed to load panel icon resource", ex);
            return null;
        }
    }

    @Provides
    RegionLockEnforcerConfig provideConfig(ConfigManager mgr) { return mgr.getConfig(RegionLockEnforcerConfig.class); }

    /**
     * Get the teleport whitelist from the current region, or null if not available.
     * @return The whitelist Set, or null if filtering is disabled, no region is selected, or whitelist is null
     */
    private Set<String> getTeleportWhitelist()
    {
        if (config.disableTeleportFiltering() || currentRegion == null)
        {
            return null;
        }
        return currentRegion.getTeleportWhitelist();
    }

    /**
     * Remove HTML color tags from a string.
     */
    private String cleanTarget(String target)
    {
        return target != null ? target.replaceAll("<col=[^>]*>", "").replaceAll("</col>", "") : null;
    }

    /**
     * Check if a category is a jewellery box category.
     */
    private boolean isJewelleryBoxCategory(String category)
    {
        return category.equals("Ring of dueling") ||
               category.equals("Games necklace") ||
               category.equals("Combat bracelet") ||
               category.equals("Skills necklace") ||
               category.equals("Amulet of glory") ||
               category.equals("Ring of wealth");
    }

    /**
     * Find a teleport definition that matches the given option and target.
     * @return The matching teleport, or null if no match found
     */
    private TeleportDefinition findMatchingTeleport(String option, String targetClean)
    {
        if (option == null) return null;
        
        for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
        {
            if (option.equalsIgnoreCase(teleport.getName()) && targetMatchesSimple(targetClean, teleport))
            {
                return teleport;
            }
        }
        return null;
    }

    /**
     * Check if a menu entry is a parent teleport entry (e.g., "Teleport" or "Rub" -> "Item name").
     */
    private boolean isParentTeleportEntry(String option, String targetClean)
    {
        if (option == null || targetClean == null) return false;
        if (!option.equalsIgnoreCase("Teleport") && !option.equalsIgnoreCase("Rub")) return false;
        
        for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
        {
            if (teleport.getMenuTarget() != null)
            {
                String targetLower = targetClean.toLowerCase();
                String menuTargetLower = teleport.getMenuTarget().toLowerCase();
                if (targetLower.contains(menuTargetLower))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if any teleport for the given target item is whitelisted.
     */
    private boolean hasAnyWhitelistedTeleport(String targetClean, Set<String> whitelist)
    {
        if (targetClean == null) return false;
        
        String targetLower = targetClean.toLowerCase();
        for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
        {
            if (teleport.getMenuTarget() != null)
            {
                String menuTargetLower = teleport.getMenuTarget().toLowerCase();
                if (targetLower.contains(menuTargetLower) && whitelist.contains(teleport.getId()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Find a submenu destination teleport that matches the option and target.
     */
    private TeleportDefinition findSubmenuTeleport(String option, String targetClean)
    {
        if (option == null) return null;
        
        for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
        {
            if (option.equalsIgnoreCase(teleport.getName()) && targetMatchesSubmenu(targetClean, teleport))
            {
                return teleport;
            }
        }
        return null;
    }

    @Subscribe
    public void onClientTick(ClientTick t)
    {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        
        // Filter jewellery box widgets if interface is open
        Set<String> whitelist = getTeleportWhitelist();
        if (whitelist != null)
        {
            net.runelite.api.widgets.Widget jewelleryBoxWidget = client.getWidget(590, 0);
            if (jewelleryBoxWidget != null && !jewelleryBoxWidget.isHidden())
            {
                // Build map of teleport names to IDs for jewellery box items
                java.util.Map<String, String> teleportNameToId = new java.util.HashMap<>();
                for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
                {
                    if (isJewelleryBoxCategory(teleport.getCategory()))
                    {
                        teleportNameToId.put(Text.standardize(teleport.getName()), teleport.getId());
                    }
                }
                
                // Filter widgets - hide non-whitelisted teleports
                filterJewelleryBoxWidgetsRecursive(jewelleryBoxWidget, teleportNameToId, whitelist, 0);
            }
        }
        
        // Filter charter ship interface if open
        if (whitelist != null)
        {
            filterCharterShipInterface();
        }
        
        // Filter teleport entries (both minimenu and direct menu options) on every tick as a fallback
        if (whitelist != null)
        {
            final var entries = client.getMenuEntries();
            final List<net.runelite.api.MenuEntry> keep = new ArrayList<>(entries.length);
            
            for (var me : entries)
            {
                String option = me.getOption();
                String targetClean = cleanTarget(me.getTarget());
                TeleportDefinition matchedTeleport = findMatchingTeleport(option, targetClean);
                
                // Keep if it's whitelisted or not a teleport destination entry
                if (matchedTeleport == null || whitelist.contains(matchedTeleport.getId()))
                {
                    keep.add(me);
                }
            }
            
            if (keep.size() != entries.length)
            {
                client.setMenuEntries(keep.toArray(new net.runelite.api.MenuEntry[0]));
            }
        }
    }

    // ForwardsMenuIterator pattern from Custom Menu Swaps to handle submenus
    // This iterator automatically traverses submenus: when it finds a main menu entry with a submenu,
    // it iterates through ALL submenu entries before moving to the next main menu entry.
    private static class MenuIterator
    {
        net.runelite.api.Menu submenu = null;
        int index = -1;  // Current main menu entry index
        int submenuIndex = -1;  // Current submenu entry index (-1 if not in submenu)
        int nextIndex = 0;  // Next main menu entry to process
        int nextSubmenuIndex = -1;  // Next submenu entry to process (-1 if not in submenu)
        net.runelite.api.MenuEntry[] menuEntries;
        
        MenuIterator(net.runelite.api.MenuEntry[] menuEntries)
        {
            this.menuEntries = menuEntries;
        }
        
        boolean hasNext()
        {
            return nextIndex < menuEntries.length;
        }
        
        net.runelite.api.MenuEntry next()
        {
            // Save current position
            index = nextIndex;
            submenuIndex = nextSubmenuIndex;
            
            // Get the entry: from submenu if we're in one, otherwise from main menu
            net.runelite.api.MenuEntry entry = submenuIndex != -1 
                ? submenu.getMenuEntries()[submenuIndex] 
                : menuEntries[index];
            
            // If we're on a main menu entry, check if it has a submenu
            if (submenuIndex == -1)
            {
                submenu = entry.getSubMenu();
            }
            
            // Update next position:
            // 1. If we're in a submenu and there are more submenu entries, continue in submenu
            if (submenu != null && submenuIndex + 1 < submenu.getMenuEntries().length)
            {
                nextSubmenuIndex++;
            }
            // 2. If we just discovered a submenu (we're on main entry that has submenu), start submenu
            else if (submenu != null && submenuIndex == -1 && submenu.getMenuEntries().length > 0)
            {
                nextSubmenuIndex = 0;
            }
            // 3. Otherwise, move to next main menu entry and reset submenu
            else
            {
                nextIndex++;
                nextSubmenuIndex = -1;
            }
            
            return entry;
        }
        
        boolean inSubmenu()
        {
            return submenu != null && submenuIndex != -1;
        }
    }

    @Subscribe(priority = -1) // Run after other plugins to catch minimenu entries, same as Custom Menu Swaps
    public void onMenuOpened(MenuOpened e)
    {
        boolean shiftDown = client.isKeyPressed(KeyCode.KC_SHIFT);

        // If in editing mode and shift is held, disable all interact clicks (except Cancel)
        if (editor.editing && shiftDown)
        {
            final var entries = client.getMenuEntries();
            List<net.runelite.api.MenuEntry> filtered = new ArrayList<>();
            
            for (net.runelite.api.MenuEntry entry : entries)
            {
                String option = entry.getOption();
                // Only keep "Cancel" option, remove all other interactions
                if (option != null && option.equalsIgnoreCase("Cancel"))
                {
                    filtered.add(entry);
                }
            }
            
            if (filtered.size() != entries.length)
            {
                client.setMenuEntries(filtered.toArray(new net.runelite.api.MenuEntry[0]));
            }
            return; // Don't process teleport filtering when shift-editing
        }
        
        // Filter teleport entries using the exact same approach as Custom Menu Swaps custom hides
        // This uses ForwardsMenuIterator pattern to handle submenus correctly
        Set<String> whitelist = getTeleportWhitelist();
        if (whitelist == null)
        {
            return;
        }
        final var entries = client.getMenuEntries();
        
        // Use the same pattern as Custom Menu Swaps: separate lists for main menu and submenu
        List<net.runelite.api.MenuEntry> filtered = new ArrayList<>();
        List<net.runelite.api.MenuEntry> submenuFiltered = new ArrayList<>();
        MenuIterator menuIterator = new MenuIterator(entries);
        
        while (menuIterator.hasNext())
        {
            // If we've moved to a new submenu, set the filtered entries on the previous submenu
            if (menuIterator.submenu != null && menuIterator.nextIndex != menuIterator.index)
            {
                menuIterator.submenu.setMenuEntries(submenuFiltered.toArray(new net.runelite.api.MenuEntry[0]));
                submenuFiltered.clear();
            }
            
            net.runelite.api.MenuEntry entry = menuIterator.next();
            boolean isSubmenu = menuIterator.inSubmenu();
            var entryList = isSubmenu ? submenuFiltered : filtered;
            
            // Use Text.standardize() exactly like Custom Menu Swaps does
            String option = Text.standardize(entry.getOption());
            String target = Text.standardize(entry.getTarget());
            
            // For submenu entries, get the parent entry's target to identify the item
            String effectiveTarget = target;
            if (isSubmenu && menuIterator.index >= 0 && menuIterator.index < menuIterator.menuEntries.length)
            {
                net.runelite.api.MenuEntry parentEntry = menuIterator.menuEntries[menuIterator.index];
                effectiveTarget = Text.standardize(parentEntry.getTarget());
            }
            
            // Check if this matches any teleport destination (not whitelisted = hide it)
            boolean shouldHide = false;
            
            // Check if this is a jewellery box interface teleport (widget group 590)
            // These have the teleport name as the option and empty target
            if (option != null && !option.isEmpty() && (target == null || target.isEmpty()))
            {
                // Match option against jewellery box teleport names
                String standardizedOption = Text.standardize(option);
                for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
                {
                    if (isJewelleryBoxCategory(teleport.getCategory()) &&
                        Text.standardize(teleport.getName()).equals(standardizedOption))
                    {
                        // This is a jewellery box teleport - check if whitelisted
                        if (!whitelist.contains(teleport.getId()))
                        {
                            shouldHide = true;
                        }
                        break;
                    }
                }
            }
            
            // Check if this is a "Break" option on a teleport tablet
            if (option != null && option.equals("break") && effectiveTarget != null)
            {
                String spellTeleportId = teleportRegistry.getSpellTeleportIdForTablet(effectiveTarget);
                if (spellTeleportId != null)
                {
                    // This is a tablet break option - hide it if the corresponding spell is not whitelisted
                    if (!whitelist.contains(spellTeleportId))
                    {
                        shouldHide = true;
                    }
                }
            }
            
            if (!shouldHide && option != null)
            {
                for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
                {
                    // For OBJECT and NPC_DIALOG teleports, match on menu option and target directly
                    // (e.g., "Travel" option on "Primio" target)
                    if (teleport.getType() == TeleportType.OBJECT || teleport.getType() == TeleportType.NPC_DIALOG)
                    {
                        String teleportOption = Text.standardize(teleport.getMenuOption() != null ? teleport.getMenuOption() : "");
                        String teleportTarget = teleport.getMenuTarget() != null ? Text.standardize(teleport.getMenuTarget()) : "";
                        
                        // Match option and target directly (e.g., "Travel" -> "Primio")
                        boolean optionMatches = option.equals(teleportOption);
                        boolean targetMatches = teleportTarget.isEmpty() || (effectiveTarget != null && effectiveTarget.equals(teleportTarget));
                        
                        if (optionMatches && targetMatches)
                        {
                            // Found matching teleport - check if whitelisted
                            if (!whitelist.contains(teleport.getId()))
                            {
                                shouldHide = true;
                            }
                            break;
                        }
                        continue; // Skip the spell/item matching logic below
                    }
                    
                    // Standardize teleport definition values exactly like CustomSwap does
                    String teleportOption = Text.standardize(teleport.getName());
                    String teleportTarget = teleport.getMenuTarget() != null ? Text.standardize(teleport.getMenuTarget()) : "";
                    
                    // Exact matching like CustomSwap.matches() does (EQUALS match type)
                    // For submenu entries, use parent target; for main entries, use entry target
                    // option is guaranteed to be non-null here (checked at line 524)
                    boolean optionMatches = option.equals(teleportOption);
                    boolean targetMatches = targetMatchesWithSpecialCases(effectiveTarget, teleportTarget);
                    
                    if (optionMatches && targetMatches)
                    {
                        // This is a teleport destination entry
                        // Hide it if it's NOT whitelisted (same logic as custom hides)
                        if (!whitelist.contains(teleport.getId()))
                        {
                            shouldHide = true;
                            break;
                        }
                    }
                }
            }
            
            // Keep entry if we shouldn't hide it (same as Custom Menu Swaps filterEntries logic)
            if (!shouldHide)
            {
                entryList.add(entry);
            }
        }
        
        // Set filtered entries on the last submenu if it exists
        if (menuIterator.submenu != null && menuIterator.nextIndex != menuIterator.index)
        {
            menuIterator.submenu.setMenuEntries(submenuFiltered.toArray(new net.runelite.api.MenuEntry[0]));
        }
        
        // Set filtered main menu entries
        if (filtered.size() != entries.length)
        {
            client.setMenuEntries(filtered.toArray(new net.runelite.api.MenuEntry[0]));
        }
    }

    /**
     * Detect when charter ship interface is loaded.
     */
    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        int groupId = event.getGroupId();
        
        net.runelite.api.widgets.Widget widget = client.getWidget(groupId, 0);
        if (widget != null)
        {
            // Build map of charter ship destination names
            java.util.Map<String, String> destinationNameToId = new java.util.HashMap<>();
            for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
            {
                if (teleport.getCategory().equals("Charter Ships"))
                {
                    String destinationName = Text.standardize(teleport.getName());
                    destinationNameToId.put(destinationName, teleport.getId());
                }
            }
            
            // Check if this widget contains charter destinations
            // Widget groups 72 and 885 are both relevant (one is the map, one is the interface)
            if (checkWidgetTreeForCharterDestinations(widget, destinationNameToId, 0, 10))
            {
                lastCharterShipWidgetId = groupId; // Cache it
            }
            else if (groupId == 72 || groupId == 885)
            {
                // These are known charter ship widget groups
                lastCharterShipWidgetId = groupId; // Cache it
            }
        }
    }
    
    /**
     * Filter spellbook teleports using the same approach as RuneLite's SpellbookPlugin.
     * This intercepts the spellbookSort script callback and filters the spell array
     * to remove non-whitelisted teleport spells, preventing gaps in the spellbook.
     */
    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event)
    {
        // Handle spellbook filtering
        if ("spellbookSort".equals(event.getEventName()))
        {
            handleSpellbookFiltering();
        }
    }
    
    /**
     * Filter jewellery box and charter ship teleports using the same approach as Better Teleport Menu plugin.
     * This intercepts script callbacks which are called when teleport buttons are added to interfaces.
     */
    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        int scriptId = event.getScriptId();
        
        // Script ID 1688 = POH_JEWELLERY_BOX_ADDBUTTON (from Better Teleport Menu)
        if (scriptId == 1688)
        {
            handleJewelleryBoxFiltering();
            return;
        }
        
        // Script ID 7336 = Charter ship destination button (similar to jewellery box)
        if (scriptId == 7336)
        {
            handleCharterShipFiltering();
        }
    }
    
    /**
     * Handle charter ship filtering (same approach as jewellery box).
     */
    private void handleCharterShipFiltering()
    {
        Set<String> whitelist = getTeleportWhitelist();
        if (whitelist == null)
        {
            return;
        }
        
        // Get the text widget for this destination option (same as jewellery box)
        net.runelite.api.widgets.Widget textWidget = client.getScriptActiveWidget();
        if (textWidget == null)
        {
            return;
        }
        
        // Get the text from the widget and extract the destination name
        String widgetText = textWidget.getText();
        if (widgetText == null || widgetText.isEmpty())
        {
            return;
        }
        
        // Parse destination name: remove HTML tags and key prefix (e.g., "0: Port Sarim")
        String destinationName = cleanTarget(widgetText);
        if (destinationName != null)
        {
            destinationName = destinationName.replaceAll("^[0-9A-Za-z]\\s*:\\s*", "").trim();
        }
        
        // Check if this destination is whitelisted
        boolean isWhitelisted = false;
        if (destinationName != null && !destinationName.isEmpty())
        {
            String standardizedName = Text.standardize(destinationName);
            for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
            {
                if (teleport.getCategory().equals("Charter Ships") &&
                    Text.standardize(teleport.getName()).equals(standardizedName) &&
                    whitelist.contains(teleport.getId()))
                {
                    isWhitelisted = true;
                    break;
                }
            }
        }
        
        textWidget.setHidden(!isWhitelisted);
    }
    
    /**
     * Handle jewellery box filtering.
     */
    private void handleJewelleryBoxFiltering()
    {
        Set<String> whitelist = getTeleportWhitelist();
        if (whitelist == null)
        {
            return;
        }
        
        // Get the text widget for this teleport option (same as Better Teleport Menu)
        net.runelite.api.widgets.Widget textWidget = client.getScriptActiveWidget();
        if (textWidget == null)
        {
            return;
        }
        
        // Get the text from the widget and extract the teleport name
        String widgetText = textWidget.getText();
        if (widgetText == null || widgetText.isEmpty())
        {
            return;
        }
        
        // Parse the teleport name from the widget text
        // Format is typically: "<col=735a28>X</col>: Teleport Name" or just "Teleport Name"
        String teleportName = parseTeleportNameFromWidget(widgetText);
        
        // Match against jewellery box teleport names
        String standardizedName = Text.standardize(teleportName);
        boolean isWhitelisted = false;
        
        for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
        {
            if (isJewelleryBoxCategory(teleport.getCategory()) &&
                Text.standardize(teleport.getName()).equals(standardizedName))
            {
                // Found matching teleport - check if whitelisted
                isWhitelisted = whitelist.contains(teleport.getId());
                break;
            }
        }
        
        // Hide the widget if not whitelisted (same approach as Better Teleport Menu)
        if (!isWhitelisted)
        {
            textWidget.setHidden(true);
            // Adjust the stack to account for hidden widget (same as Better Teleport Menu)
            int[] stack = client.getIntStack();
            int size = client.getIntStackSize();
            if (size > 0)
            {
                stack[size - 1] -= textWidget.getOriginalHeight();
            }
        }
    }
    
    /**
     * Filter charter ship destinations from the interface.
     * This runs on ClientTick to continuously filter the destination list and map markers.
     */
    private void filterCharterShipInterface()
    {
        Set<String> whitelist = getTeleportWhitelist();
        if (whitelist == null)
        {
            lastCharterShipWidgetId = -1; // Reset if conditions not met
            return;
        }
        
        // Build map of charter ship destination names to IDs
        java.util.Map<String, String> destinationNameToId = new java.util.HashMap<>();
        for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
        {
            if (teleport.getCategory().equals("Charter Ships"))
            {
                String destinationName = Text.standardize(teleport.getName());
                destinationNameToId.put(destinationName, teleport.getId());
            }
        }
        
        // First, try using cached widget ID
        if (lastCharterShipWidgetId != -1)
        {
            net.runelite.api.widgets.Widget cachedWidget = client.getWidget(lastCharterShipWidgetId, 0);
            if (cachedWidget != null && !cachedWidget.isHidden())
            {
                // Verify it still contains charter destinations
                if (checkWidgetTreeForCharterDestinations(cachedWidget, destinationNameToId, 0, 3))
                {
                    filterCharterShipWidgetsRecursive(cachedWidget, destinationNameToId, whitelist, 0);
                    return;
                }
                else
                {
                    // Widget changed, reset cache
                    lastCharterShipWidgetId = -1;
                }
            }
            else
            {
                // Widget closed, reset cache
                lastCharterShipWidgetId = -1;
            }
        }
        
        // If not cached, search for the interface (throttled to avoid spam)
        charterShipSearchCooldown--;
        if (charterShipSearchCooldown > 0)
        {
            return; // Skip search this tick
        }
        charterShipSearchCooldown = 1; // Search every tick (0.6s)
        
        // Search for charter ship interface - look for widgets containing destination names
        for (int widgetGroup = 1; widgetGroup <= 300; widgetGroup++)
        {
            net.runelite.api.widgets.Widget testWidget = client.getWidget(widgetGroup, 0);
            if (testWidget != null && !testWidget.isHidden())
            {
                // Check if this widget tree contains charter destinations
                if (checkWidgetTreeForCharterDestinations(testWidget, destinationNameToId, 0, 5))
                {
                    log.debug("Found charter ship interface - Widget Group: {}", widgetGroup);
                    lastCharterShipWidgetId = widgetGroup; // Cache it
                    filterCharterShipWidgetsRecursive(testWidget, destinationNameToId, whitelist, 0);
                    return;
                }
            }
        }
    }
    
    /**
     * Generic widget tree traversal method.
     * @param widget The widget to start traversal from
     * @param matcher Function that returns true if widget matches (stops traversal), false to continue
     * @param depth Current depth
     * @param maxDepth Maximum depth to traverse
     * @return true if a match was found, false otherwise
     */
    private boolean traverseWidgetTree(net.runelite.api.widgets.Widget widget,
                                     java.util.function.Function<net.runelite.api.widgets.Widget, Boolean> matcher,
                                                         int depth,
                                                         int maxDepth)
    {
        if (widget == null || depth > maxDepth)
        {
            return false;
        }
        
        Boolean match = matcher.apply(widget);
        if (match != null && match)
            {
                return true;
            }
            
        // Recursively check children
        net.runelite.api.widgets.Widget[] children = widget.getChildren();
        if (children != null)
        {
            for (net.runelite.api.widgets.Widget child : children)
            {
                if (traverseWidgetTree(child, matcher, depth + 1, maxDepth))
                {
                    return true;
                }
            }
        }
        
        // Check dynamic children
        net.runelite.api.widgets.Widget[] dynamicChildren = widget.getDynamicChildren();
        if (dynamicChildren != null)
        {
            for (net.runelite.api.widgets.Widget child : dynamicChildren)
            {
                if (traverseWidgetTree(child, matcher, depth + 1, maxDepth))
                {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Generic widget tree traversal method for filtering (modifies widgets).
     * @param widget The widget to start traversal from
     * @param matcher Function that returns true if widget matches (stops traversal), false to continue
     * @param depth Current depth
     * @param maxDepth Maximum depth to traverse
     */
    private void traverseWidgetTreeForFiltering(net.runelite.api.widgets.Widget widget,
                                               java.util.function.Function<net.runelite.api.widgets.Widget, Boolean> matcher,
                                               int depth,
                                               int maxDepth)
    {
        if (widget == null || depth > maxDepth)
        {
            return;
        }
        
        Boolean match = matcher.apply(widget);
        if (match != null && match)
        {
            return; // Found a match, no need to recurse further
        }
        
        // Recursively check children
        net.runelite.api.widgets.Widget[] children = widget.getChildren();
        if (children != null)
        {
            for (net.runelite.api.widgets.Widget child : children)
            {
                traverseWidgetTreeForFiltering(child, matcher, depth + 1, maxDepth);
            }
        }
        
        // Check dynamic children
        net.runelite.api.widgets.Widget[] dynamicChildren = widget.getDynamicChildren();
        if (dynamicChildren != null)
        {
            for (net.runelite.api.widgets.Widget child : dynamicChildren)
            {
                traverseWidgetTreeForFiltering(child, matcher, depth + 1, maxDepth);
            }
        }
        
        // Check nested children
        net.runelite.api.widgets.Widget[] nestedChildren = widget.getNestedChildren();
        if (nestedChildren != null)
                {
            for (net.runelite.api.widgets.Widget child : nestedChildren)
            {
                traverseWidgetTreeForFiltering(child, matcher, depth + 1, maxDepth);
            }
        }
    }
    
    /**
     * Quick check to see if a widget tree contains charter ship destinations (without full filtering).
     */
    private boolean checkWidgetTreeForCharterDestinations(net.runelite.api.widgets.Widget widget,
                                                         java.util.Map<String, String> destinationNameToId,
                                                         int depth,
                                                         int maxDepth)
    {
        return traverseWidgetTree(widget, w -> {
            String widgetText = w.getText();
        if (widgetText != null && !widgetText.isEmpty())
        {
                // Check for "Destination" title
                if (widgetText.contains("Destination"))
                {
                    return true;
                }
                
                // Parse destination name from text (format: "0: Port Sarim", "1: Brimhaven", etc.)
                String destinationName = widgetText;
                // Remove number/letter prefix (e.g., "0: ", "1: ", "B: ", etc.)
                destinationName = destinationName.replaceAll("^[0-9A-Za-z]:\\s*", "");
                destinationName = destinationName.trim();
                
                String standardizedText = Text.standardize(destinationName);
                return destinationNameToId.keySet().stream()
                    .anyMatch(key -> standardizedText.equals(key));
                }
        return false;
        }, depth, maxDepth);
    }
    
    /**
     * Recursively filter charter ship widgets by hiding non-whitelisted destinations.
     */
    private void filterCharterShipWidgetsRecursive(net.runelite.api.widgets.Widget widget,
                                                  java.util.Map<String, String> destinationNameToId,
                                                  Set<String> whitelist,
                                                  int depth)
    {
        traverseWidgetTreeForFiltering(widget, w -> {
            String widgetText = w.getText();
        if (widgetText != null && !widgetText.isEmpty())
        {
            // Parse destination name from text (format: "0: Port Sarim", "1: Brimhaven", etc.)
            String destinationName = widgetText;
            // Remove number prefix (e.g., "0: ", "1: ", "B: ", etc.)
            destinationName = destinationName.replaceAll("^[0-9A-Za-z]:\\s*", "");
            destinationName = destinationName.trim();
            
            String standardizedName = Text.standardize(destinationName);
            String destinationId = destinationNameToId.get(standardizedName);
            
            if (destinationId != null)
            {
                // Found a charter ship destination - hide if not whitelisted
                    w.setHidden(!whitelist.contains(destinationId));
                    return true; // Found a match, stop traversal
                }
            }
            return false;
        }, depth, 20);
    }
    
    /**
     * Handle spellbook filtering via script callback.
     */
    private void handleSpellbookFiltering()
    {

        // Get spell array from script stack (same as SpellbookPlugin)
        int[] stack = client.getIntStack();
        int size = client.getIntStackSize();

        int spellbookEnumId = stack[size - 3];
        int spellArrayId = stack[size - 2];
        int numSpells = stack[size - 1];

        EnumComposition spellbookEnum = client.getEnum(spellbookEnumId);
        int[] spells = client.getArray(spellArrayId); // enum indices

        // If teleport filtering is disabled or no profile, ensure all spells are visible and don't filter
        Set<String> whitelist = getTeleportWhitelist();
        if (whitelist == null)
        {
            // Unhide all spell widgets
            for (int i = 0; i < numSpells; ++i)
            {
                ItemComposition spellObj = client.getItemDefinition(spellbookEnum.getIntValue(spells[i]));
                net.runelite.api.widgets.Widget spellWidget = client.getWidget(spellObj.getIntValue(ParamID.SPELL_BUTTON));
                if (spellWidget != null)
                {
                    spellWidget.setHidden(false);
                }
            }
            // Don't modify the spell array - let it pass through unchanged
            return;
        }

        // Build a map of spell names to teleport IDs for faster lookup
        java.util.Map<String, String> spellNameToTeleportId = new java.util.HashMap<>();
        for (TeleportDefinition teleport : teleportRegistry.getAllTeleports())
        {
            if (teleport.getType() == TeleportType.SPELL && teleport.getMenuTarget() != null)
            {
                spellNameToTeleportId.put(teleport.getMenuTarget().toLowerCase(), teleport.getId());
            }
        }
        int[] newSpells = new int[numSpells];
        int numNewSpells = 0;

        for (int i = 0; i < numSpells; ++i)
        {
            ItemComposition spellObj = client.getItemDefinition(spellbookEnum.getIntValue(spells[i]));
            String spellName = spellObj.getStringValue(ParamID.SPELL_NAME);
            
            // Get the widget for this spell
            net.runelite.api.widgets.Widget spellWidget = client.getWidget(spellObj.getIntValue(ParamID.SPELL_BUTTON));
            
            if (spellName == null || spellName.isEmpty())
            {
                // Keep non-teleport spells and ensure widget is visible
                if (spellWidget != null)
                {
                    spellWidget.setHidden(false);
                }
                newSpells[numNewSpells++] = spells[i];
                continue;
            }

            // Check if this spell matches any teleport definition (fast lookup)
            String spellLower = spellName.toLowerCase();
            String teleportId = spellNameToTeleportId.get(spellLower);
            boolean isTeleportSpell = teleportId != null;
            boolean isWhitelisted = isTeleportSpell && whitelist.contains(teleportId);

            // If it's a teleport spell, hide/show the widget based on whitelist
            if (isTeleportSpell && spellWidget != null)
            {
                spellWidget.setHidden(!isWhitelisted);
            }
            else if (spellWidget != null)
            {
                // Not a teleport spell, ensure it's visible
                spellWidget.setHidden(false);
            }

            // Only add spell to new array if it's not a teleport spell, or if it's whitelisted
            if (!isTeleportSpell || isWhitelisted)
            {
                newSpells[numNewSpells++] = spells[i];
            }
        }

        // Copy filtered spells back to original array (removes gaps)
        System.arraycopy(newSpells, 0, spells, 0, numNewSpells);
        stack[size - 1] = numNewSpells;
    }

    /**
     * Recursively filter jewellery box widgets by hiding those that match non-whitelisted teleports.
     */
    private void filterJewelleryBoxWidgetsRecursive(net.runelite.api.widgets.Widget widget,
                                                    java.util.Map<String, String> teleportNameToId,
                                                    Set<String> whitelist,
                                                    int depth)
    {
        traverseWidgetTreeForFiltering(widget, w -> {
            // Check widget text
            String widgetText = w.getText();
        if (widgetText != null && !widgetText.isEmpty())
        {
            String standardizedText = Text.standardize(widgetText);
            String teleportId = teleportNameToId.get(standardizedText);
            if (teleportId != null)
            {
                    w.setHidden(!whitelist.contains(teleportId));
                    return true; // Found a match, stop traversal
            }
        }
        
            // Check widget name
            String widgetName = w.getName();
        if (widgetName != null && !widgetName.isEmpty())
        {
            String standardizedName = Text.standardize(widgetName);
            String teleportId = teleportNameToId.get(standardizedName);
            if (teleportId != null)
            {
                    w.setHidden(!whitelist.contains(teleportId));
                    return true; // Found a match, stop traversal
            }
        }
        
            return false;
        }, depth, 20);
    }

    /**
     * Redraw the spellbook to apply filtering changes.
     * This triggers the spellbook to redraw, which will call the spellbookSort script callback.
     * Uses the same approach as RuneLite's SpellbookPlugin.
     * This method must be called on the client thread, so it uses invokeLater internally.
     */
    public void redrawSpellbook()
    {
        // Invoke on client thread to avoid "must be called on client thread" errors
        clientThread.invokeLater(() ->
        {
            // Only redraw if client is logged in and ready
            if (client.getGameState() != GameState.LOGGED_IN)
            {
                return;
            }
            
            // Use the spellbook UNIVERSE widget (same as SpellbookPlugin)
            // Widget ID 218 is the spellbook interface, widget 0 is the UNIVERSE container
            net.runelite.api.widgets.Widget spellbookWidget = client.getWidget(218, 0);
            if (spellbookWidget != null && spellbookWidget.getOnInvTransmitListener() != null)
            {
                client.createScriptEvent(spellbookWidget.getOnInvTransmitListener())
                    .setSource(spellbookWidget)
                    .run();
            }
        });
    }


    // Consume the action when clicking a marked tile (and record marks in editor mode)
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked e)
    {
        if (config.disableBorder()) return;

        final int typeId = e.getMenuAction().getId();
        Tile hovered = client.getSelectedSceneTile();
        WorldPoint wp = hovered != null ? hovered.getWorldLocation() : null;
        
        // TODO: Handle world map chunk clicks when in editing mode
        // Note: RuneLite's MouseAdapter has a different signature than standard Java MouseAdapter,
        // so direct mouse event handling isn't straightforward. World map clicks may need to be
        // detected through widget script events or a custom overlay approach.
        // The chunk methods (addChunk, removeChunk, isChunkFullyContained) are implemented
        // and ready to use once world map click detection is working.

        boolean shiftDown = client.isKeyPressed(KeyCode.KC_SHIFT);

        // Editor: shift-click toggles tile marking (mark if unmarked, unmark if marked)
        if (editor.editing && shiftDown && wp != null && currentRegion != null
                && (typeId == MenuAction.WALK.getId()
                || typeId == MenuAction.GAME_OBJECT_FIRST_OPTION.getId()
                || typeId == MenuAction.GROUND_ITEM_FIRST_OPTION.getId()
                || typeId == MenuAction.NPC_FIRST_OPTION.getId()))
        {
            e.consume();
            // Toggle: if marked, unmark it; if unmarked, mark it
            if (currentRegion.getBoundaryTiles().contains(wp))
            {
                currentRegion.removeTile(wp);
            }
            else
            {
                currentRegion.addTile(wp);
            }
                // Clear inner tiles when boundary changes
            currentRegion.getInnerTiles().clear();
            saveRegions(); // Save immediately on change
            notifyRegionsChanged();
            return;
        }


        // Check menu block rules first
        if (currentProfile != null && currentProfile.getMenuRules() != null)
        {
            String menuOption = e.getMenuOption();
            String menuTarget = e.getMenuTarget();
            for (MenuBlockRule rule : currentProfile.getMenuRules())
            {
                if (rule.isEnabled() && rule.matches(typeId, menuOption, menuTarget))
                {
                        // Blocked menu action
                    e.consume();
                    return;
                }
            }
        }

        // Block ALL clicks outside the bordered region (only when inner tiles are computed)
        // Only block within normal surface map bounds (excludes underground, instances, upper/lower floors)
        // Only block game world actions (WALK, GAME_OBJECT, GROUND_ITEM, NPC, etc.), not UI actions, player interactions, or "Walk here" on players
        if (wp != null && currentRegion != null && !currentRegion.getInnerTiles().isEmpty())
        {
            // Only apply click blocking within normal surface map bounds
            if (isWithinSurfaceBounds(wp))
            {
                // Allow "Walk here" if it's on a player, block it otherwise
                if (typeId == MenuAction.WALK.getId())
                {
                    // Check if there are player actions in the menu (indicates we clicked on a player)
                    final var entries = client.getMenuEntries();
                    boolean isOnPlayer = false;
                    for (var me : entries)
                    {
                        if (isPlayerAction(me.getType()))
                        {
                            isOnPlayer = true;
                            break;
                        }
                    }
                    // If not on a player, block "Walk here"
                    if (!isOnPlayer)
                    {
                        Set<WorldPoint> clickableTiles = currentRegion.getAllClickableTiles();
                        if (!clickableTiles.contains(wp))
                        {
                            e.consume();
                        }
                    }
                }
                // Block other game world actions
                else if (isGameWorldAction(typeId))
                {
                    Set<WorldPoint> clickableTiles = currentRegion.getAllClickableTiles();
                    if (!clickableTiles.contains(wp))
                    {
                        e.consume();
                    }
                }
            }
        }
        // If wp is null or not a game world action, this is a UI click, player interaction, etc. - don't block it
    }

    // Also hide walk/interact entries in the menu for marked tiles (clean UX)
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded e)
    {
        if (config.disableBorder()) return;

        Tile hovered = client.getSelectedSceneTile();
        WorldPoint hoveredWp = hovered != null ? hovered.getWorldLocation() : null;
        
        // Filter teleports based on whitelist (if region has teleport whitelist configured)
        // We need to filter on EVERY MenuEntryAdded event to catch submenu entries as they're added
        Set<String> whitelist = getTeleportWhitelist();
        if (whitelist != null)
        {
            final var entries = client.getMenuEntries();
            final List<net.runelite.api.MenuEntry> keep = new ArrayList<>(entries.length);
            
            for (var me : entries)
            {
                String option = me.getOption();
                String targetClean = cleanTarget(me.getTarget());
                
                // Check if this is a submenu destination entry first
                TeleportDefinition matchedTeleport = findSubmenuTeleport(option, targetClean);
                
                if (matchedTeleport != null)
                {
                    // Submenu destination entry - keep if whitelisted
                    if (whitelist.contains(matchedTeleport.getId()))
                    {
                        keep.add(me);
                    }
                }
                else if (isParentTeleportEntry(option, targetClean))
                {
                    // Parent teleport entry - keep if any teleport for this item is whitelisted
                    if (hasAnyWhitelistedTeleport(targetClean, whitelist))
                    {
                        keep.add(me);
                    }
                }
                else
                {
                    // Not a teleport entry, keep it
                    keep.add(me);
                }
            }
            
            if (keep.size() != entries.length)
            {
                client.setMenuEntries(keep.toArray(new net.runelite.api.MenuEntry[0]));
            }
        }

        // Check menu block rules to filter menu entries
        if (currentProfile != null && currentProfile.getMenuRules() != null)
        {
            final var entries = client.getMenuEntries();
            final List<net.runelite.api.MenuEntry> keep = new ArrayList<>(entries.length);
            
            for (var me : entries)
            {
                boolean shouldBlock = false;
                
                // Check against menu block rules
                for (MenuBlockRule rule : currentProfile.getMenuRules())
                {
                    if (rule.isEnabled() && rule.matches(me.getType().getId(), me.getOption(), me.getTarget()))
                    {
                        shouldBlock = true;
                        break;
                    }
                }
                
                if (!shouldBlock)
                {
                    keep.add(me);
                }
            }
            
            if (keep.size() != entries.length)
            {
                client.setMenuEntries(keep.toArray(new net.runelite.api.MenuEntry[0]));
            }
        }

        // Remove ALL menu entries for tiles outside the border (only when inner tiles are computed)
        // Only filter within normal surface map bounds (excludes underground, instances, upper/lower floors)
        // Whitelist approach: filter everything, then explicitly allow only UI actions, player interactions, and "Walk here" on players
        if (hoveredWp != null && !editor.editing && currentRegion != null && !currentRegion.getInnerTiles().isEmpty())
        {
            // Only apply menu filtering within normal surface map bounds
            if (isWithinSurfaceBounds(hoveredWp))
            {
                Set<WorldPoint> clickableTiles = currentRegion.getAllClickableTiles();
                if (!clickableTiles.contains(hoveredWp))
                {
                    // Check if we're hovering over a player (do this once before filtering)
                    boolean hoveringOverPlayer = isHoveringOverPlayer(hoveredWp);
                    
                    // Filter ALL menu entries, then explicitly allow only UI actions, player interactions, and "Walk here" on players
                    final var entries = client.getMenuEntries();
                    final List<net.runelite.api.MenuEntry> keep = new ArrayList<>(entries.length);
                    
                    for (var me : entries)
                    {
                        MenuAction actionType = me.getType();
                        // Allow UI actions and player interactions
                        if (isUIAction(actionType) || isPlayerAction(actionType))
                        {
                            keep.add(me);
                        }
                        // Allow "Walk here" only if it's targeting a player
                        else if (actionType == MenuAction.WALK && hoveringOverPlayer)
                        {
                            keep.add(me);
                        }
                    }
                    
                    // Update menu entries (even if empty, to remove all game world options)
                    client.setMenuEntries(keep.toArray(new net.runelite.api.MenuEntry[0]));
                }
            }
        }
        // If hoveredWp is null, this is a UI hover (inventory, chat, etc.) - don't filter menu entries
    }

    // Minimal editor: hotkey toggles edit mode; left-click toggles tile marking
    class EditorInput extends MouseAdapter
    {
        @Getter boolean editing = false;

        final HotkeyListener toggleEditHotkey = new HotkeyListener(() -> config.toggleEditor())
        {
            @Override public void hotkeyPressed() 
            { 
                // Only allow toggle if current profile is in edit mode (has no inner tiles)
                // If profile is finished (has inner tiles), don't allow toggle
                if (currentRegion != null && currentRegion.getInnerTiles().isEmpty())
                {
                    editing = !editing;
                }
                // If no region exists, create one and enable editing
                else if (currentRegion == null)
                {
                    createRegion(null); // Will auto-generate name
                    editing = true;
                }
            }
        };

    }

    /**
     * Check if a tile has at least one outer edge (neighbor not in hull).
     */
    private boolean hasOuterEdge(WorldPoint tile, Set<WorldPoint> hullTiles, int plane)
    {
        int x = tile.getX();
        int y = tile.getY();
        
        WorldPoint northNeighbor = new WorldPoint(x, y + 1, plane);
        WorldPoint southNeighbor = new WorldPoint(x, y - 1, plane);
        WorldPoint eastNeighbor = new WorldPoint(x + 1, y, plane);
        WorldPoint westNeighbor = new WorldPoint(x - 1, y, plane);
        
        return !hullTiles.contains(northNeighbor) || 
               !hullTiles.contains(southNeighbor) || 
               !hullTiles.contains(eastNeighbor) || 
               !hullTiles.contains(westNeighbor);
    }

    /**
     * Check if a MenuAction is a UI-related action (widget, inventory, etc.).
     */
    private boolean isUIAction(MenuAction actionType)
    {
        return actionType == MenuAction.CC_OP ||
               actionType == MenuAction.WIDGET_TARGET ||
               actionType == MenuAction.WIDGET_TYPE_1 ||
               actionType == MenuAction.WIDGET_TYPE_4 ||
               actionType == MenuAction.WIDGET_TYPE_5 ||
               actionType == MenuAction.CANCEL ||
               actionType.name().contains("WIDGET") ||
               actionType.name().contains("ITEM_USE");
    }

    /**
     * Check if a MenuAction is a player interaction (follow, trade, attack, etc.).
     */
    private boolean isPlayerAction(MenuAction actionType)
    {
        return actionType == MenuAction.PLAYER_FIRST_OPTION ||
               actionType == MenuAction.PLAYER_SECOND_OPTION ||
               actionType == MenuAction.PLAYER_THIRD_OPTION ||
               actionType == MenuAction.PLAYER_FOURTH_OPTION ||
               actionType == MenuAction.PLAYER_FIFTH_OPTION ||
               actionType == MenuAction.PLAYER_SIXTH_OPTION ||
               actionType == MenuAction.PLAYER_SEVENTH_OPTION ||
               actionType == MenuAction.PLAYER_EIGHTH_OPTION ||
               actionType.name().contains("PLAYER");
    }

    /**
     * Check if there's a player near the hovered tile location (within 3x3 area).
     * When you right-click a player, "Walk here" appears in the menu, and we want to allow it.
     * Returns true if hovering within 1 tile of a player (3x3 area).
     */
    private boolean isHoveringOverPlayer(WorldPoint hoveredWp)
    {
        if (hoveredWp == null) return false;
        
        // Check the scene for players within 1 tile (3x3 area)
        try
        {
            if (client.getPlayers() != null)
            {
                for (net.runelite.api.Player player : client.getPlayers())
                {
                    if (player != null && player.getWorldLocation() != null)
                    {
                        WorldPoint playerWp = player.getWorldLocation();
                        // Check if player is within 1 tile (3x3 area)
                        if (playerWp.getPlane() == hoveredWp.getPlane() &&
                            Math.abs(playerWp.getX() - hoveredWp.getX()) <= 1 &&
                            Math.abs(playerWp.getY() - hoveredWp.getY()) <= 1)
                        {
                            return true;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            // getPlayers() might throw exceptions in some cases, ignore them
        }
        
        return false;
    }

    /**
     * Check if a menu action type is a game world action (walk, interact with objects/NPCs/items).
     * Note: Player interactions (PLAYER_*, FOLLOW, TRADE, etc.) are explicitly excluded and will not be blocked.
     */
    private boolean isGameWorldAction(int typeId)
    {
        return typeId == MenuAction.WALK.getId() ||
               typeId == MenuAction.GAME_OBJECT_FIRST_OPTION.getId() ||
               typeId == MenuAction.GAME_OBJECT_SECOND_OPTION.getId() ||
               typeId == MenuAction.GAME_OBJECT_THIRD_OPTION.getId() ||
               typeId == MenuAction.GAME_OBJECT_FOURTH_OPTION.getId() ||
               typeId == MenuAction.GAME_OBJECT_FIFTH_OPTION.getId() ||
               typeId == MenuAction.GROUND_ITEM_FIRST_OPTION.getId() ||
               typeId == MenuAction.GROUND_ITEM_SECOND_OPTION.getId() ||
               typeId == MenuAction.GROUND_ITEM_THIRD_OPTION.getId() ||
               typeId == MenuAction.GROUND_ITEM_FOURTH_OPTION.getId() ||
               typeId == MenuAction.GROUND_ITEM_FIFTH_OPTION.getId() ||
               typeId == MenuAction.NPC_FIRST_OPTION.getId() ||
               typeId == MenuAction.NPC_SECOND_OPTION.getId() ||
               typeId == MenuAction.NPC_THIRD_OPTION.getId() ||
               typeId == MenuAction.NPC_FOURTH_OPTION.getId() ||
               typeId == MenuAction.NPC_FIFTH_OPTION.getId();
        // Player actions (PLAYER_*, FOLLOW, TRADE, etc.) are NOT included here, so they won't be blocked
    }

    /**
     * Parse teleport name from widget text by removing HTML tags and key prefixes.
     */
    private String parseTeleportNameFromWidget(String widgetText)
    {
        String teleportName = widgetText;
            
        // Remove HTML color tags and key prefix (e.g., "<col=735a28>X</col>: " or "<col=735a28>: ")
        teleportName = teleportName.replaceAll("<col=[^>]*>", "");
        teleportName = teleportName.replaceAll("</col>", "");
        teleportName = teleportName.replaceAll("^[A-Za-z0-9]:\\s*", ""); // Remove "X: " prefix
        teleportName = teleportName.replaceAll("^:\\s*", ""); // Remove ": " prefix if no key
        teleportName = teleportName.trim();
        
        return teleportName;
    }

    /**
     * Check if target matches teleport menu target (simple contains check).
     * Returns true if target is empty (for direct menu options).
     */
    private boolean targetMatchesSimple(String targetClean, TeleportDefinition teleport)
    {
        if (targetClean != null && teleport.getMenuTarget() != null)
        {
            String targetLower = targetClean.toLowerCase();
            String menuTargetLower = teleport.getMenuTarget().toLowerCase();
            return targetLower.contains(menuTargetLower);
        }
        else if (targetClean == null || targetClean.isEmpty())
        {
            // Target is empty - still match if option matches (for direct menu options)
            return true;
        }
        return false;
    }

    /**
     * Check if target matches teleport with special cases for amulet of glory and slayer ring.
     */
    private boolean targetMatchesWithSpecialCases(String effectiveTarget, String teleportTarget)
    {
        boolean targetMatches = teleportTarget.isEmpty() || (effectiveTarget != null && effectiveTarget.equals(teleportTarget));
        
        // Special case: Amulet of glory teleports also match "Amulet of eternal glory"
        if (!targetMatches && teleportTarget.equals("amulet of glory") && effectiveTarget != null)
        {
            targetMatches = effectiveTarget.equals("amulet of eternal glory");
        }
        if (!targetMatches && teleportTarget.equals("amulet of eternal glory") && effectiveTarget != null)
        {
            targetMatches = effectiveTarget.equals("amulet of glory");
        }
        
        // Special case: Slayer ring teleports also match "Slayer ring (eternal)"
        if (!targetMatches && teleportTarget.equals("slayer ring") && effectiveTarget != null)
        {
            targetMatches = effectiveTarget.equals("slayer ring (eternal)");
        }
        if (!targetMatches && teleportTarget.equals("slayer ring (eternal)") && effectiveTarget != null)
        {
            targetMatches = effectiveTarget.equals("slayer ring");
        }
        
        return targetMatches;
    }

    /**
     * Check if target matches teleport for submenu entries (target contains item name OR matches destination name).
     */
    private boolean targetMatchesSubmenu(String targetClean, TeleportDefinition teleport)
    {
        if (targetClean == null || targetClean.isEmpty())
        {
            // Target is empty - this could be a submenu entry
            return true;
        }
        else if (teleport.getMenuTarget() != null)
        {
            String targetLower = targetClean.toLowerCase();
            String menuTargetLower = teleport.getMenuTarget().toLowerCase();
            String nameLower = teleport.getName().toLowerCase();
            
            // Target contains item name OR target matches destination name
            return targetLower.contains(menuTargetLower) || targetLower.equals(nameLower);
        }
        return false;
    }

    /**
     * Import a region from a file (called from sidebar).
     */
    public void importRegionFromFile(String filePath)
    {
        Region imported = importRegion(filePath);
        
        if (imported != null)
        {
            // Check if profile with same name exists
            boolean exists = regions.stream()
                    .anyMatch(p -> p.getName().equals(imported.getName()));
            
            if (exists)
            {
                // Append number to name
                int counter = 1;
                String baseName = imported.getName();
                String newName;
                while (true)
                {
                    newName = baseName + " (" + counter + ")";
                    final String checkName = newName;
                    if (!regions.stream().anyMatch(p -> p.getName().equals(checkName)))
                    {
                        break;
                    }
                    counter++;
                }
                imported.setName(newName);
            }
            
            regions.add(imported);
            currentRegion = imported;
            saveRegions();
            notifyRegionsChanged();
            
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "Region imported successfully: " + imported.getName(),
                "Import Successful",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        }
        else
        {
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "Failed to import region. Please check the file format.",
                "Import Error",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }


    /**
     * Load regions from config.
     */
    private void loadRegions()
    {
        String profilesStr = configManager.getConfiguration(RegionLockEnforcerConfig.GROUP, "regions");
        regions.clear();
        if (profilesStr != null && !profilesStr.isEmpty())
        {
            List<Region> loaded = RegionSerializer.deserializeRegions(profilesStr);
            // Cache is already pre-computed in deserializeRegion
            regions.addAll(loaded);
        }
        
        // Load selected region
        String selectedName = configManager.getConfiguration(RegionLockEnforcerConfig.GROUP, "selectedRegion");
        if (selectedName != null && !selectedName.isEmpty())
        {
            currentRegion = regions.stream()
                    .filter(p -> p.getName().equals(selectedName))
                    .findFirst()
                    .orElse(null);
        }
        
        // If no region selected but we have regions, select the first one
        if (currentRegion == null && !regions.isEmpty())
        {
            currentRegion = regions.get(0);
        }
    }

    /**
     * Save regions to config.
     */
    public void saveRegions()
    {
        String profilesStr = RegionSerializer.serializeRegions(regions);
        configManager.setConfiguration(RegionLockEnforcerConfig.GROUP, "regions", profilesStr);
        
        if (currentRegion != null)
        {
            configManager.setConfiguration(RegionLockEnforcerConfig.GROUP, "selectedRegion", currentRegion.getName());
        }
    }

    /**
     * Create a new region with the given name.
     */
    public Region createRegion(String name)
    {
        if (name == null || name.trim().isEmpty())
        {
            name = "Region " + (regions.size() + 1);
        }
        Region profile = new Region(name.trim());
        regions.add(profile);
        currentRegion = profile;
        saveRegions();
        notifyRegionsChanged();
        return profile;
    }

    /**
     * Notify that regions have changed (for UI refresh).
     */
    public void notifyRegionsChanged()
    {
        if (panel != null && panel.getBorderComponent() != null)
        {
            panel.getBorderComponent().refreshRegionList();
            panel.getBorderComponent().refreshBorderList();
            panel.getBorderComponent().refreshTeleportsList();
        }
    }

    /**
     * Check if the editor is currently in editing mode.
     */
    public boolean isEditing()
    {
        return editor.editing;
    }

    /**
     * Set the editing mode state.
     */
    public void setEditing(boolean editing)
    {
        editor.editing = editing;
    }
    
    /**
     * Get the toggle editor keybind as a display string.
     * Returns "Tab" if not set (default).
     */
    public String getToggleEditorKeybindString()
    {
        net.runelite.client.config.Keybind keybind = config.toggleEditor();
        if (keybind == null)
        {
            return "Tab";
        }
        
        int keyCode = keybind.getKeyCode();
        int notSetKeyCode = net.runelite.client.config.Keybind.NOT_SET.getKeyCode();
        
        if (keyCode == notSetKeyCode)
        {
            return "Tab";
        }
        
        // Build modifier string
        StringBuilder result = new StringBuilder();
        int modifiers = keybind.getModifiers();
        
        if ((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0)
        {
            result.append("Ctrl+");
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0)
        {
            result.append("Alt+");
        }
        if ((modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0)
        {
            result.append("Shift+");
        }
        
        // Convert key code to readable text string
        result.append(keyCodeToText(keyCode));
        
        return result.toString();
    }
    
    /**
     * Convert a key code to a readable text string.
     */
    private String keyCodeToText(int keyCode)
    {
        // Function keys
        if (keyCode >= java.awt.event.KeyEvent.VK_F1 && keyCode <= java.awt.event.KeyEvent.VK_F12)
        {
            int fNumber = keyCode - java.awt.event.KeyEvent.VK_F1 + 1;
            return "F" + fNumber;
        }
        
        // Special keys
        switch (keyCode)
        {
            case java.awt.event.KeyEvent.VK_TAB:
                return "Tab";
            case java.awt.event.KeyEvent.VK_ENTER:
                return "Enter";
            case java.awt.event.KeyEvent.VK_SPACE:
                return "Space";
            case java.awt.event.KeyEvent.VK_SHIFT:
                return "Shift";
            case java.awt.event.KeyEvent.VK_CONTROL:
                return "Ctrl";
            case java.awt.event.KeyEvent.VK_ALT:
                return "Alt";
            case java.awt.event.KeyEvent.VK_ESCAPE:
                return "Esc";
            case java.awt.event.KeyEvent.VK_BACK_SPACE:
                return "Backspace";
            case java.awt.event.KeyEvent.VK_DELETE:
                return "Delete";
            case java.awt.event.KeyEvent.VK_INSERT:
                return "Insert";
            case java.awt.event.KeyEvent.VK_HOME:
                return "Home";
            case java.awt.event.KeyEvent.VK_END:
                return "End";
            case java.awt.event.KeyEvent.VK_PAGE_UP:
                return "Page Up";
            case java.awt.event.KeyEvent.VK_PAGE_DOWN:
                return "Page Down";
            case java.awt.event.KeyEvent.VK_UP:
                return "Up";
            case java.awt.event.KeyEvent.VK_DOWN:
                return "Down";
            case java.awt.event.KeyEvent.VK_LEFT:
                return "Left";
            case java.awt.event.KeyEvent.VK_RIGHT:
                return "Right";
            default:
                // For regular keys, try to get the character
                if (keyCode >= java.awt.event.KeyEvent.VK_A && keyCode <= java.awt.event.KeyEvent.VK_Z)
                {
                    return String.valueOf((char)('A' + (keyCode - java.awt.event.KeyEvent.VK_A)));
                }
                if (keyCode >= java.awt.event.KeyEvent.VK_0 && keyCode <= java.awt.event.KeyEvent.VK_9)
                {
                    return String.valueOf((char)('0' + (keyCode - java.awt.event.KeyEvent.VK_0)));
                }
                // Fallback: use KeyEvent key name
                // getKeyText() never returns null
                return java.awt.event.KeyEvent.getKeyText(keyCode);
        }
    }

    /**
     * Re-enable editing mode for a border profile by clearing inner tiles.
     * This returns the border to the state before "Finish" was clicked.
     */
    public void enableEditingMode(Region profile)
    {
        if (profile == null) return;
        
        // Clear inner tiles to return to darkened tile mode
        profile.setInnerTiles(new HashSet<>());
        
        // Enable editing mode AND toggle editing on
        editor.editing = true;
        
        // Select this profile if not already selected
        if (currentRegion != profile)
        {
            currentRegion = profile;
        }
        
        saveRegions();
        notifyRegionsChanged();
    }

    /**
     * Select a region by name.
     */
    public void selectRegion(String name)
    {
        if (name == null) return;
        currentRegion = regions.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
        if (currentRegion != null)
        {
            saveRegions();
            notifyRegionsChanged();
        }
    }

    /**
     * Delete a region by name.
     */
    public void deleteRegion(String name)
    {
        if (name == null) return;
        boolean removed = regions.removeIf(p -> p.getName().equals(name));
        if (removed)
        {
            // If we deleted the current profile, select another one or null
            if (currentRegion != null && currentRegion.getName().equals(name))
            {
                currentRegion = regions.isEmpty() ? null : regions.get(0);
            }
            saveRegions();
            notifyRegionsChanged();
        }
    }

    /**
     * Check if a WorldPoint is within normal surface map bounds.
     * This excludes underground areas (far north), instances, and upper/lower floors.
     * 
     * Map dimensions: 47 chunks × 64 = 3008 tiles wide, 33 chunks × 64 = 2112 tiles tall
     * Left edge X = 960, Bottom edge Y = 1984
     * 
     * @param wp The WorldPoint to check
     * @return true if the point is within normal surface map bounds
     */
    private boolean isWithinSurfaceBounds(WorldPoint wp)
    {
        if (wp == null) return false;
        
        // Normal OSRS surface map bounds
        // Map is 3008 tiles wide (47 chunks × 64) and 2112 tiles tall (33 chunks × 64)
        // Left edge X = 960, Bottom edge Y = 1984
        // Underground areas are placed far north (outside these bounds)
        // Instances and other special areas are outside these bounds
        final int MIN_X = 960;
        final int MAX_X = 960 + 3008 - 1; // 3967
        final int MIN_Y = 1984;
        final int MAX_Y = 1984 + 2112 - 1; // 4095
        
        int x = wp.getX();
        int y = wp.getY();
        
        return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y;
    }


    /**
     * Compute all inner tiles (tiles inside the boundary) using flood-fill.
     * This is a one-time computation that saves all inner tiles to the profile.
     * If the shape is not closed, it will automatically close it first.
     * 
     * @param profile The border profile to compute inner tiles for
     * @return true if computation was successful, false if the shape cannot be processed
     */
    public boolean computeInnerTiles(Region profile)
    {
        if (profile == null || profile.getBoundaryTiles().isEmpty())
        {
            if (profile != null)
            {
                profile.setInnerTiles(new HashSet<>());
            }
            return true;
        }

        Set<WorldPoint> boundaryTiles = profile.getBoundaryTiles();
        if (boundaryTiles.isEmpty()) return true;
        
        // Get the plane from first boundary tile (assume all are on same plane)
        int plane = boundaryTiles.iterator().next().getPlane();

        // Filter boundary tiles to the same plane
        Set<WorldPoint> samePlaneBoundary = boundaryTiles.stream()
            .filter(bp -> bp.getPlane() == plane)
            .collect(java.util.stream.Collectors.toSet());
        
        if (samePlaneBoundary.isEmpty()) return true;

        // Compute the rectilinear convex hull
        // This fills all tiles in the bounding box of the marked tiles, creating a closed shape.
        // The hull includes:
        // 1. All marked tiles (they're clickable)
        // 2. All tiles that fill gaps to create the minimal axis-aligned enclosure
        // 
        // For already-closed/filled shapes, the hull is idempotent (no change).
        // For unclosed shapes, it fills gaps to create the minimal enclosure.
        computeConvexHull(profile, samePlaneBoundary, plane);
        Set<WorldPoint> hullTiles = profile.getBoundaryTiles();
        
        // All tiles in the convex hull are inner tiles (they're all clickable)
        // The hull already includes all marked tiles plus any gaps that were filled
        profile.setInnerTiles(new HashSet<>(hullTiles));
        
        // Filter boundary tiles to only include tiles that have border lines (outer edges)
        // A tile has a border line if at least one of its 4 neighbors is outside
        Set<WorldPoint> borderTiles = new HashSet<>();
        for (WorldPoint tile : hullTiles)
        {
            if (hasOuterEdge(tile, hullTiles, plane))
            {
                borderTiles.add(tile);
            }
        }
        
        // Update boundary tiles to only include tiles with border lines
        profile.getBoundaryTiles().clear();
        profile.getBoundaryTiles().addAll(borderTiles);
        
        return true;
    }

    /**
     * Compute the rectilinear (orthogonal) convex hull of the marked tiles.
     * This finds the minimal axis-aligned enclosure of the marked tiles.
     * 
     * This is always called to guarantee a closed boundary, regardless of whether
     * the user's marked tiles form a closed shape or not. For already-closed shapes,
     * the hull is idempotent (no change).
     * 
     * Algorithm:
     * 1. For each row (y) with marked tiles, fill from min_x to max_x (horizontal close)
     * 2. For each column (x) with marked tiles, fill from min_y to max_y (vertical close)
     * 3. Take the union of these filled tiles
     * 
     * This yields the smallest-area, axis-aligned enclosure of the given tiles.
     */
    private void computeConvexHull(Region profile, Set<WorldPoint> markedTiles, int plane)
    {
        if (markedTiles.isEmpty()) return;
        
        // Step 1: Compute row spans (horizontal close) and column spans (vertical close)
        java.util.Map<Integer, int[]> rows = new java.util.HashMap<>(); // y -> [minX, maxX]
        java.util.Map<Integer, int[]> cols = new java.util.HashMap<>(); // x -> [minY, maxY]
        
        for (WorldPoint bp : markedTiles)
        {
            int x = bp.getX();
            int y = bp.getY();
            
            // Update row span
            rows.compute(y, (key, span) -> {
                if (span == null) return new int[]{x, x};
                if (x < span[0]) span[0] = x;
                if (x > span[1]) span[1] = x;
                return span;
            });
            
            // Update column span
            cols.compute(x, (key, span) -> {
                if (span == null) return new int[]{y, y};
                if (y < span[0]) span[0] = y;
                if (y > span[1]) span[1] = y;
                return span;
            });
        }
        
        // Step 2: Fill horizontal spans (row by row) and vertical spans (column by column)
        Set<WorldPoint> hullTiles = new HashSet<>();
        
        // Fill horizontal spans
        rows.forEach((y, span) -> {
            for (int x = span[0]; x <= span[1]; x++)
            {
                hullTiles.add(new WorldPoint(x, y, plane));
            }
        });
        
        // Fill vertical spans
        cols.forEach((x, span) -> {
            for (int y = span[0]; y <= span[1]; y++)
            {
                hullTiles.add(new WorldPoint(x, y, plane));
            }
        });
        
        // Step 5: Replace profile's boundary tiles with the convex hull
        // Clear existing boundary tiles and set to hull tiles
        // This ensures we always have a closed boundary
        profile.getBoundaryTiles().clear();
        profile.getBoundaryTiles().addAll(hullTiles);
    }

    /**
     * Export a region to a JSON file.
     * 
     * @param profile The region to export
     * @param filePath The file path to save to
     * @return true if export was successful, false otherwise
     */
    public boolean exportRegion(Region profile, String filePath)
    {
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath))
        {
            Gson prettyGson = gson.newBuilder().setPrettyPrinting().create();
            
            // Create a serializable version of the profile
            RegionExport exportData = new RegionExport();
            exportData.name = profile.getName();
            exportData.boundaryTiles = profile.getBoundaryTiles().stream()
                .map(wp -> new TileData(wp.getX(), wp.getY(), wp.getPlane()))
                .collect(java.util.stream.Collectors.toList());
            exportData.innerTiles = profile.getInnerTiles().stream()
                .map(wp -> new TileData(wp.getX(), wp.getY(), wp.getPlane()))
                .collect(java.util.stream.Collectors.toList());
            exportData.teleportWhitelist = new java.util.ArrayList<>(
                profile.getTeleportWhitelist() != null ? profile.getTeleportWhitelist() : java.util.Collections.emptySet());
            
            prettyGson.toJson(exportData, writer);
            return true;
        }
        catch (Exception e)
        {
            log.error("Failed to export region", e);
            return false;
        }
    }

    /**
     * Import a region from a JSON file.
     * 
     * @param filePath The file path to load from
     * @return The imported Region, or null if import failed
     */
    public Region importRegion(String filePath)
    {
        try (java.io.FileReader reader = new java.io.FileReader(filePath))
        {
            RegionExport exportData = gson.fromJson(reader, RegionExport.class);
            
            if (exportData == null || exportData.name == null) return null;
            
            Region profile = new Region(exportData.name);
            
            // Convert tile data back to WorldPoints
            Set<WorldPoint> boundaryTiles = exportData.boundaryTiles != null
                ? exportData.boundaryTiles.stream()
                    .map(td -> new WorldPoint(td.x, td.y, td.plane))
                    .collect(java.util.stream.Collectors.toSet())
                : new HashSet<>();
            profile.setBoundaryTiles(boundaryTiles);
            
            Set<WorldPoint> innerTiles = exportData.innerTiles != null
                ? exportData.innerTiles.stream()
                    .map(td -> new WorldPoint(td.x, td.y, td.plane))
                    .collect(java.util.stream.Collectors.toSet())
                : new HashSet<>();
            profile.setInnerTiles(innerTiles);
            
            profile.setTeleportWhitelist(exportData.teleportWhitelist != null
                ? new HashSet<>(exportData.teleportWhitelist)
                : new HashSet<>());
            
            return profile;
        }
        catch (Exception e)
        {
            log.error("Failed to import region", e);
            return null;
        }
    }

    /**
     * Helper classes for JSON export/import
     */
    private static class RegionExport
    {
        String name;
        List<TileData> boundaryTiles;
        List<TileData> innerTiles;
        List<String> teleportWhitelist;
    }

    private static class TileData
    {
        int x;
        int y;
        int plane;
        
        TileData() {} // Default constructor for Gson
        
        TileData(int x, int y, int plane)
        {
            this.x = x;
            this.y = y;
            this.plane = plane;
        }
    }
}


