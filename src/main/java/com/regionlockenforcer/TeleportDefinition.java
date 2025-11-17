package com.regionlockenforcer;

import lombok.Data;

/**
 * Represents a single teleport option in the game.
 * This can be a menu option on an item, object, NPC dialog, or a spell.
 */
@Data
public class TeleportDefinition
{
    private String id; // Unique identifier for this teleport
    private String name; // Display name (e.g., "Edgeville", "Varrock")
    private String category; // Category this teleport belongs to (e.g., "Ring of Duelling")
    private TeleportType type; // Type of teleport (ITEM, SPELL, OBJECT, NPC_DIALOG)
    private String menuOption; // Menu option text (e.g., "Edgeville", "Teleport")
    private String menuTarget; // Menu target text (e.g., "Ring of duelling", "Varrock Teleport")
    private Integer itemId; // Item ID if this is an item teleport (null otherwise)
    private Integer spellId; // Spell ID if this is a spell teleport (null otherwise)
    private Integer objectId; // Object ID if this is an object teleport (null otherwise)
    private Integer npcId; // NPC ID if this is an NPC dialog teleport (null otherwise)
    
    public TeleportDefinition()
    {
    }
    
    public TeleportDefinition(String id, String name, String category, TeleportType type)
    {
        this.id = id;
        this.name = name;
        this.category = category;
        this.type = type;
    }
    
    /**
     * Check if this teleport matches a menu entry.
     * Based on logs: Target is like "<col=ff9040>Pendant of ates</col>" (item name with HTML tags)
     * Option is "Teleport" for all entries, so destination must be identified another way.
     * For now, we'll match based on: option matches menuOption AND target contains item name.
     * The specific destination will need to be identified via identifier or item ID.
     */
    public boolean matchesMenuEntry(String option, String target)
    {
        // First check if option matches
        if (menuOption != null && !menuOption.equalsIgnoreCase(option))
        {
            return false;
        }
        
        // Strip HTML color tags from target for comparison
        if (menuTarget != null && target != null)
        {
            String targetClean = target.replaceAll("<col=[^>]*>", "").replaceAll("</col>", "");
            String targetLower = targetClean.toLowerCase();
            String menuTargetLower = menuTarget.toLowerCase();
            
            // Target must contain the item name (after stripping HTML)
            if (!targetLower.contains(menuTargetLower))
            {
                return false;
            }
        }
        
        // If we get here, option matches and target contains item name
        // But we can't distinguish which specific destination this is from option/target alone
        // We'll need to use identifier or item ID - for now return true if basic match
        return true;
    }
    
    /**
     * Check if this teleport matches a menu entry with identifier.
     * The identifier might encode which specific destination this menu entry represents.
     * For items with multiple teleport options, the identifier might be an index or encoded value.
     */
    public boolean matchesMenuEntry(String option, String target, int identifier)
    {
        // Basic match first (option and item name in target)
        if (!matchesMenuEntry(option, target))
        {
            return false;
        }
        
        // If we have an itemId, check if identifier matches
        // Note: identifier might be the item ID for the teleport item itself, not the destination
        if (itemId != null && identifier == itemId)
        {
            return true;
        }
        
        // The problem: we can't distinguish which specific destination this is
        // All "Pendant of ates" teleports will match because they all have:
        // - Option: "Teleport"
        // - Target: "<col=ff9040>Pendant of ates</col>"
        // 
        // We need to find another way to identify the destination.
        // Possible approaches:
        // 1. Menu entry order/index (but this is fragile)
        // 2. Identifier encoding (need to decode it)
        // 3. Different menu structure than we're assuming
        
        // For now, return true if basic match passes
        // This means ALL teleports for the same item will match, which is wrong
        // We need more information to fix this properly
        return true;
    }
}

