package com.regionlockenforcer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * Registry of all teleports in the game, organized by category.
 * This will be populated with all teleport definitions.
 */
@Singleton
@Getter
public class TeleportRegistry
{
    private final Map<String, List<TeleportDefinition>> teleportsByCategory = new HashMap<>();
    private final Map<String, TeleportDefinition> teleportsById = new HashMap<>();
    
    public TeleportRegistry()
    {
        initializeTeleports();
    }
    
    /**
     * Initialize all teleport definitions.
     * This will be expanded with all teleports in the game.
     */
    private void initializeTeleports()
    {
        // Ring of dueling teleports
        addTeleportCategory("Ring of dueling", createRingOfDuelingTeleports());
        
        // Games necklace teleports
        addTeleportCategory("Games necklace", createGamesNecklaceTeleports());
        
        // Pendant of ates teleports
        addTeleportCategory("Pendant of ates", createPendantOfAtesTeleports());
        
        // Standard spellbook teleports
        addTeleportCategory("Standard Spellbook", createStandardSpellbookTeleports());
        
        // Ancient Magicks spellbook teleports
        addTeleportCategory("Ancient Magicks", createAncientMagicksTeleports());
        
        // Lunar spellbook teleports
        addTeleportCategory("Lunar Spellbook", createLunarSpellbookTeleports());
        
        // Arceuus spellbook teleports
        addTeleportCategory("Arceuus Spellbook", createArceuusSpellbookTeleports());
        
        // Combat bracelet teleports
        addTeleportCategory("Combat bracelet", createCombatBraceletTeleports());
        
        // Skills necklace teleports
        addTeleportCategory("Skills necklace", createSkillsNecklaceTeleports());
        
        // Amulet of glory teleports (applies to both Amulet of glory and Amulet of eternal glory)
        addTeleportCategory("Amulet of glory", createAmuletOfGloryTeleports());
        
        // Ring of wealth teleports
        addTeleportCategory("Ring of wealth", createRingOfWealthTeleports());
        
        // Slayer ring teleports (applies to both Slayer ring and Slayer ring (eternal))
        addTeleportCategory("Slayer ring", createSlayerRingTeleports());
        
        // Digsite pendant teleports
        addTeleportCategory("Digsite pendant", createDigsitePendantTeleports());
        
        // Necklace of passage teleports
        addTeleportCategory("Necklace of passage", createNecklaceOfPassageTeleports());
        
        // Burning amulet teleports
        addTeleportCategory("Burning amulet", createBurningAmuletTeleports());
        
        // Ring of returning teleports
        addTeleportCategory("Ring of returning", createRingOfReturningTeleports());
        
        // Treasure Trail Scrolls teleports
        addTeleportCategory("Treasure Trail Scrolls", createTreasureTrailScrollsTeleports());
        
        // Chronicle teleports
        addTeleportCategory("Chronicle", createChronicleTeleports());
        
        // Giantsoul amulet teleports
        addTeleportCategory("Giantsoul amulet", createGiantsoulAmuletTeleports());
        
        // Xeric's talisman teleports
        addTeleportCategory("Xeric's talisman", createXericsTalismanTeleports());
        
        // Primio teleports
        addTeleportCategory("Primio", createPrimioTeleports());
        
        // Ring of the elements teleports
        addTeleportCategory("Ring of the elements", createRingOfElementsTeleports());
        
        // Charter Ships teleports
        addTeleportCategory("Charter Ships", createCharterShipsTeleports());
    }
    
    /**
     * Add a category of teleports.
     */
    private void addTeleportCategory(String categoryName, List<TeleportDefinition> teleports)
    {
        teleportsByCategory.put(categoryName, teleports);
        for (TeleportDefinition teleport : teleports)
        {
            teleportsById.put(teleport.getId(), teleport);
        }
    }
    
    /**
     * Create Ring of Dueling teleport definitions.
     */
    private List<TeleportDefinition> createRingOfDuelingTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Ring of dueling teleports
        teleports.add(createTeleport("ring_dueling_emirs_arena", "Emir's Arena", "Ring of dueling", 
            TeleportType.ITEM, "Rub", "Ring of dueling", null, null, null, null));
        teleports.add(createTeleport("ring_dueling_ferox_enclave", "Ferox Enclave", "Ring of dueling", 
            TeleportType.ITEM, "Rub", "Ring of dueling", null, null, null, null));
        teleports.add(createTeleport("ring_dueling_castle_wars", "Castle Wars", "Ring of dueling", 
            TeleportType.ITEM, "Rub", "Ring of dueling", null, null, null, null));
        teleports.add(createTeleport("ring_dueling_fortis_colosseum", "Fortis Colosseum", "Ring of dueling", 
            TeleportType.ITEM, "Rub", "Ring of dueling", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Games Necklace teleport definitions.
     */
    private List<TeleportDefinition> createGamesNecklaceTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Games necklace teleports
        teleports.add(createTeleport("games_necklace_barbarian_assault", "Barbarian Assault", "Games necklace", 
            TeleportType.ITEM, "Rub", "Games necklace", null, null, null, null));
        teleports.add(createTeleport("games_necklace_burthorpe", "Burthorpe Games Room", "Games necklace", 
            TeleportType.ITEM, "Rub", "Games necklace", null, null, null, null));
        teleports.add(createTeleport("games_necklace_tears_of_guthix", "Tears of Guthix", "Games necklace", 
            TeleportType.ITEM, "Rub", "Games necklace", null, null, null, null));
        teleports.add(createTeleport("games_necklace_corporeal_beast", "Corporeal Beast", "Games necklace", 
            TeleportType.ITEM, "Rub", "Games necklace", null, null, null, null));
        teleports.add(createTeleport("games_necklace_wintertodt", "Wintertodt Camp", "Games necklace", 
            TeleportType.ITEM, "Rub", "Games necklace", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Pendant of ates teleport definitions.
     */
    private List<TeleportDefinition> createPendantOfAtesTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Pendant of ates teleports
        teleports.add(createTeleport("pendant_ates_darkfrost", "Darkfrost", "Pendant of ates", 
            TeleportType.ITEM, "Teleport", "Pendant of ates", null, null, null, null));
        teleports.add(createTeleport("pendant_ates_twilight_temple", "Twilight Temple", "Pendant of ates", 
            TeleportType.ITEM, "Teleport", "Pendant of ates", null, null, null, null));
        teleports.add(createTeleport("pendant_ates_ralos_rise", "Ralos' Rise", "Pendant of ates", 
            TeleportType.ITEM, "Teleport", "Pendant of ates", null, null, null, null));
        teleports.add(createTeleport("pendant_ates_north_aldarin", "North Aldarin", "Pendant of ates", 
            TeleportType.ITEM, "Teleport", "Pendant of ates", null, null, null, null));
        teleports.add(createTeleport("pendant_ates_kastori", "Kastori", "Pendant of ates", 
            TeleportType.ITEM, "Teleport", "Pendant of ates", null, null, null, null));
        teleports.add(createTeleport("pendant_ates_nemus_retreat", "Nemus Retreat", "Pendant of ates", 
            TeleportType.ITEM, "Teleport", "Pendant of ates", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Standard Spellbook teleport definitions.
     */
    private List<TeleportDefinition> createStandardSpellbookTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Standard spellbook teleports in the order they appear in the spellbook
        teleports.add(createTeleport("spell_lumbridge_home", "Lumbridge Home Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Lumbridge Home Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_varrock", "Varrock Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Varrock Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lumbridge", "Lumbridge Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Lumbridge Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_falador", "Falador Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Falador Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_teleport_to_house", "Teleport to House", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Teleport to House", null, null, null, null));
        teleports.add(createTeleport("spell_camelot", "Camelot Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Camelot Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_kourend_castle", "Kourend Castle Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Kourend Castle Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ardougne", "Ardougne Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Ardougne Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_civitas_illa_fortis", "Civitas illa Fortis Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Civitas illa Fortis Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_watchtower", "Watchtower Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Watchtower Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_trollheim", "Trollheim Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Trollheim Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ape_atoll", "Ape Atoll Teleport", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Ape Atoll Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_teleport_me_to_boat", "Teleport me to Boat", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Teleport me to Boat", null, null, null, null));
        teleports.add(createTeleport("spell_teleport_to_target", "Teleport to Target", "Standard Spellbook", 
            TeleportType.SPELL, "Cast", "Teleport to Target", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Ancient Magicks spellbook teleport definitions.
     */
    private List<TeleportDefinition> createAncientMagicksTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Ancient Magicks spellbook teleports in the order they appear in the spellbook
        teleports.add(createTeleport("spell_ancient_edgeville_home", "Edgeville Home Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Edgeville Home Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ancient_paddewwa", "Paddewwa Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Paddewwa Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ancient_senntisten", "Senntisten Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Senntisten Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ancient_kharyrll", "Kharyrll Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Kharyrll Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ancient_lassar", "Lassar Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Lassar Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ancient_dareeyak", "Dareeyak Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Dareeyak Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ancient_carrallanger", "Carrallanger Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Carrallanger Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ancient_annakarl", "Annakarl Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Annakarl Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_ancient_ghorrock", "Ghorrock Teleport", "Ancient Magicks", 
            TeleportType.SPELL, "Cast", "Ghorrock Teleport", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Lunar spellbook teleport definitions.
     */
    private List<TeleportDefinition> createLunarSpellbookTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Lunar spellbook teleports in the order they appear in the spellbook
        teleports.add(createTeleport("spell_lunar_home", "Lunar Home Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Lunar Home Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_moonclan", "Moonclan Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Moonclan Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_ourania", "Ourania Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Ourania Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_waterbirth", "Waterbirth Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Waterbirth Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_barbarian", "Barbarian Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Barbarian Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_khazard", "Khazard Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Khazard Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_fishing_guild", "Fishing Guild Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Fishing Guild Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_catherby", "Catherby Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Catherby Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_ice_plateau", "Ice Plateau Teleport", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Ice Plateau Teleport", null, null, null, null));
        
        // Tele Group spells
        teleports.add(createTeleport("spell_lunar_tele_group_moonclan", "Tele Group Moonclan", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Tele Group Moonclan", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_tele_group_waterbirth", "Tele Group Waterbirth", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Tele Group Waterbirth", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_tele_group_barbarian", "Tele Group Barbarian", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Tele Group Barbarian", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_tele_group_khazard", "Tele Group Khazard", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Tele Group Khazard", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_tele_group_fishing_guild", "Tele Group Fishing Guild", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Tele Group Fishing Guild", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_tele_group_catherby", "Tele Group Catherby", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Tele Group Catherby", null, null, null, null));
        teleports.add(createTeleport("spell_lunar_tele_group_ice_plateau", "Tele Group Ice Plateau", "Lunar Spellbook", 
            TeleportType.SPELL, "Cast", "Tele Group Ice Plateau", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Get the Tele Group spell ID for a given single teleport spell ID.
     * Returns null if there is no corresponding Tele Group spell.
     */
    public String getTeleGroupIdForSingleTeleport(String singleTeleportId)
    {
        // Map single teleport IDs to their Tele Group equivalents
        java.util.Map<String, String> singleToGroup = new java.util.HashMap<>();
        singleToGroup.put("spell_lunar_moonclan", "spell_lunar_tele_group_moonclan");
        singleToGroup.put("spell_lunar_waterbirth", "spell_lunar_tele_group_waterbirth");
        singleToGroup.put("spell_lunar_barbarian", "spell_lunar_tele_group_barbarian");
        singleToGroup.put("spell_lunar_khazard", "spell_lunar_tele_group_khazard");
        singleToGroup.put("spell_lunar_fishing_guild", "spell_lunar_tele_group_fishing_guild");
        singleToGroup.put("spell_lunar_catherby", "spell_lunar_tele_group_catherby");
        singleToGroup.put("spell_lunar_ice_plateau", "spell_lunar_tele_group_ice_plateau");
        
        return singleToGroup.get(singleTeleportId);
    }
    
    /**
     * Get the single teleport spell ID for a given Tele Group spell ID.
     * Returns null if there is no corresponding single teleport.
     */
    public String getSingleTeleportIdForTeleGroup(String teleGroupId)
    {
        // Map Tele Group IDs to their single teleport equivalents
        java.util.Map<String, String> groupToSingle = new java.util.HashMap<>();
        groupToSingle.put("spell_lunar_tele_group_moonclan", "spell_lunar_moonclan");
        groupToSingle.put("spell_lunar_tele_group_waterbirth", "spell_lunar_waterbirth");
        groupToSingle.put("spell_lunar_tele_group_barbarian", "spell_lunar_barbarian");
        groupToSingle.put("spell_lunar_tele_group_khazard", "spell_lunar_khazard");
        groupToSingle.put("spell_lunar_tele_group_fishing_guild", "spell_lunar_fishing_guild");
        groupToSingle.put("spell_lunar_tele_group_catherby", "spell_lunar_catherby");
        groupToSingle.put("spell_lunar_tele_group_ice_plateau", "spell_lunar_ice_plateau");
        
        return groupToSingle.get(teleGroupId);
    }
    
    /**
     * Get the spell teleport ID for a given tablet name.
     * Returns null if there is no corresponding spell teleport.
     * Used to hide "Break" option on tablets when the spell is not whitelisted.
     */
    public String getSpellTeleportIdForTablet(String tabletName)
    {
        // Map tablet names to their corresponding spell teleport IDs
        java.util.Map<String, String> tabletToSpell = new java.util.HashMap<>();
        
        // Standard spellbook tablets
        tabletToSpell.put("varrock tablet", "spell_varrock");
        tabletToSpell.put("falador tablet", "spell_falador");
        tabletToSpell.put("lumbridge tablet", "spell_lumbridge");
        tabletToSpell.put("camelot tablet", "spell_camelot");
        tabletToSpell.put("ardougne tablet", "spell_ardougne");
        tabletToSpell.put("civitas illa fortis tablet", "spell_civitas_illa_fortis");
        tabletToSpell.put("watchtower tablet", "spell_watchtower");
        tabletToSpell.put("house tablet", "spell_teleport_to_house");
        tabletToSpell.put("kourend castle tablet", "spell_kourend_castle");
        tabletToSpell.put("target teleport", "spell_teleport_to_target");
        
        // Ancient Magicks tablets
        tabletToSpell.put("paddewwa teleport tablet", "spell_ancient_paddewwa");
        tabletToSpell.put("senntisten teleport tablet", "spell_ancient_senntisten");
        tabletToSpell.put("kharyrll teleport tablet", "spell_ancient_kharyrll");
        tabletToSpell.put("lassar teleport tablet", "spell_ancient_lassar");
        tabletToSpell.put("dareeyak teleport tablet", "spell_ancient_dareeyak");
        tabletToSpell.put("carrallanger teleport tablet", "spell_ancient_carrallanger");
        tabletToSpell.put("annakarl teleport tablet", "spell_ancient_annakarl");
        tabletToSpell.put("ghorrock teleport tablet", "spell_ancient_ghorrock");
        
        // Lunar Spellbook tablets
        tabletToSpell.put("moonclan teleport tablet", "spell_lunar_moonclan");
        tabletToSpell.put("ourania teleport tablet", "spell_lunar_ourania");
        tabletToSpell.put("waterbirth teleport tablet", "spell_lunar_waterbirth");
        tabletToSpell.put("barbarian teleport tablet", "spell_lunar_barbarian");
        tabletToSpell.put("khazard teleport tablet", "spell_lunar_khazard");
        tabletToSpell.put("fishing guild teleport tablet", "spell_lunar_fishing_guild");
        tabletToSpell.put("catherby teleport tablet", "spell_lunar_catherby");
        tabletToSpell.put("ice plateau teleport tablet", "spell_lunar_ice_plateau");
        
        // Arceuus Spellbook tablets
        tabletToSpell.put("arceuus library tablet", "spell_arceuus_library");
        tabletToSpell.put("draynor manor tablet", "spell_arceuus_draynor_manor");
        tabletToSpell.put("battlefront tablet", "spell_arceuus_battlefront");
        tabletToSpell.put("mind altar tablet", "spell_arceuus_mind_altar");
        tabletToSpell.put("salve graveyard tablet", "spell_arceuus_salve_graveyard");
        tabletToSpell.put("fenkenstrain's castle tablet", "spell_arceuus_fenkenstrain");
        tabletToSpell.put("west ardougne tablet", "spell_arceuus_west_ardougne");
        tabletToSpell.put("harmony island tablet", "spell_arceuus_harmony_island");
        tabletToSpell.put("cemetery tablet", "spell_arceuus_cemetery");
        tabletToSpell.put("barrows tablet", "spell_arceuus_barrows");
        tabletToSpell.put("ape atoll tablet", "spell_arceuus_ape_atoll"); // Maps to Arceuus spellbook, not standard
        
        // Normalize tablet name (lowercase, remove extra spaces)
        String normalizedTablet = tabletName != null ? tabletName.toLowerCase().trim() : "";
        return tabletToSpell.get(normalizedTablet);
    }
    
    /**
     * Create Arceuus spellbook teleport definitions.
     */
    private List<TeleportDefinition> createArceuusSpellbookTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Arceuus spellbook teleports in the order they appear in the spellbook
        teleports.add(createTeleport("spell_arceuus_home", "Arceuus Home Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Arceuus Home Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_library", "Arceuus Library Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Arceuus Library Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_draynor_manor", "Draynor Manor Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Draynor Manor Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_battlefront", "Battlefront Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Battlefront Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_mind_altar", "Mind Altar Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Mind Altar Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_respawn", "Respawn Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Respawn Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_salve_graveyard", "Salve Graveyard Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Salve Graveyard Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_fenkenstrain", "Fenkenstrain's Castle Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Fenkenstrain's Castle Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_west_ardougne", "West Ardougne Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "West Ardougne Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_harmony_island", "Harmony Island Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Harmony Island Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_cemetery", "Cemetery Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Cemetery Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_barrows", "Barrows Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Barrows Teleport", null, null, null, null));
        teleports.add(createTeleport("spell_arceuus_ape_atoll", "Ape Atoll Teleport", "Arceuus Spellbook", 
            TeleportType.SPELL, "Cast", "Ape Atoll Teleport", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Combat Bracelet teleport definitions.
     */
    private List<TeleportDefinition> createCombatBraceletTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Combat bracelet teleports
        teleports.add(createTeleport("combat_bracelet_warriors_guild", "Warriors' Guild", "Combat bracelet", 
            TeleportType.ITEM, "Rub", "Combat bracelet", null, null, null, null));
        teleports.add(createTeleport("combat_bracelet_champions_guild", "Champions' Guild", "Combat bracelet", 
            TeleportType.ITEM, "Rub", "Combat bracelet", null, null, null, null));
        teleports.add(createTeleport("combat_bracelet_edgeville_monastery", "Edgeville Monastery", "Combat bracelet", 
            TeleportType.ITEM, "Rub", "Combat bracelet", null, null, null, null));
        teleports.add(createTeleport("combat_bracelet_ranging_guild", "Ranging Guild", "Combat bracelet", 
            TeleportType.ITEM, "Rub", "Combat bracelet", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Skills Necklace teleport definitions.
     */
    private List<TeleportDefinition> createSkillsNecklaceTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Skills necklace teleports
        teleports.add(createTeleport("skills_necklace_fishing_guild", "Fishing Guild", "Skills necklace", 
            TeleportType.ITEM, "Rub", "Skills necklace", null, null, null, null));
        teleports.add(createTeleport("skills_necklace_mining_guild", "Mining Guild", "Skills necklace", 
            TeleportType.ITEM, "Rub", "Skills necklace", null, null, null, null));
        teleports.add(createTeleport("skills_necklace_crafting_guild", "Crafting Guild", "Skills necklace", 
            TeleportType.ITEM, "Rub", "Skills necklace", null, null, null, null));
        teleports.add(createTeleport("skills_necklace_cooks_guild", "Cooks' Guild", "Skills necklace", 
            TeleportType.ITEM, "Rub", "Skills necklace", null, null, null, null));
        teleports.add(createTeleport("skills_necklace_woodcutting_guild", "Woodcutting Guild", "Skills necklace", 
            TeleportType.ITEM, "Rub", "Skills necklace", null, null, null, null));
        teleports.add(createTeleport("skills_necklace_farming_guild", "Farming Guild", "Skills necklace", 
            TeleportType.ITEM, "Rub", "Skills necklace", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Amulet of Glory teleport definitions.
     * Note: These teleports apply to both "Amulet of glory" and "Amulet of eternal glory".
     */
    private List<TeleportDefinition> createAmuletOfGloryTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Amulet of glory teleports (works for both regular and eternal)
        teleports.add(createTeleport("amulet_glory_edgeville", "Edgeville", "Amulet of glory", 
            TeleportType.ITEM, "Rub", "Amulet of glory", null, null, null, null));
        teleports.add(createTeleport("amulet_glory_karamja", "Karamja", "Amulet of glory", 
            TeleportType.ITEM, "Rub", "Amulet of glory", null, null, null, null));
        teleports.add(createTeleport("amulet_glory_draynor", "Draynor Village", "Amulet of glory", 
            TeleportType.ITEM, "Rub", "Amulet of glory", null, null, null, null));
        teleports.add(createTeleport("amulet_glory_al_kharid", "Al Kharid", "Amulet of glory", 
            TeleportType.ITEM, "Rub", "Amulet of glory", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Ring of Wealth teleport definitions.
     */
    private List<TeleportDefinition> createRingOfWealthTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Ring of wealth teleports
        teleports.add(createTeleport("ring_wealth_miscellania", "Miscellania", "Ring of wealth", 
            TeleportType.ITEM, "Rub", "Ring of wealth", null, null, null, null));
        teleports.add(createTeleport("ring_wealth_grand_exchange", "Grand Exchange", "Ring of wealth", 
            TeleportType.ITEM, "Rub", "Ring of wealth", null, null, null, null));
        teleports.add(createTeleport("ring_wealth_falador_park", "Falador Park", "Ring of wealth", 
            TeleportType.ITEM, "Rub", "Ring of wealth", null, null, null, null));
        teleports.add(createTeleport("ring_wealth_dondakan", "Dondakan", "Ring of wealth", 
            TeleportType.ITEM, "Rub", "Ring of wealth", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Slayer Ring teleport definitions.
     * Note: These teleports apply to both "Slayer ring" and "Slayer ring (eternal)".
     */
    private List<TeleportDefinition> createSlayerRingTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Slayer ring teleports (works for both regular and eternal)
        teleports.add(createTeleport("slayer_ring_slayer_tower", "Slayer Tower", "Slayer ring", 
            TeleportType.ITEM, "Rub", "Slayer ring", null, null, null, null));
        teleports.add(createTeleport("slayer_ring_fremennik_slayer_dungeon", "Fremennik Slayer Dungeon", "Slayer ring", 
            TeleportType.ITEM, "Rub", "Slayer ring", null, null, null, null));
        teleports.add(createTeleport("slayer_ring_tarns_lair", "Tarn's Lair", "Slayer ring", 
            TeleportType.ITEM, "Rub", "Slayer ring", null, null, null, null));
        teleports.add(createTeleport("slayer_ring_stronghold_slayer_cave", "Stronghold Slayer Cave", "Slayer ring", 
            TeleportType.ITEM, "Rub", "Slayer ring", null, null, null, null));
        teleports.add(createTeleport("slayer_ring_dark_beasts", "Dark Beasts", "Slayer ring", 
            TeleportType.ITEM, "Rub", "Slayer ring", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Digsite Pendant teleport definitions.
     */
    private List<TeleportDefinition> createDigsitePendantTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Digsite pendant teleports
        teleports.add(createTeleport("digsite_pendant_digsite", "Digsite", "Digsite pendant", 
            TeleportType.ITEM, "Rub", "Digsite pendant", null, null, null, null));
        teleports.add(createTeleport("digsite_pendant_fossil_island", "Fossil Island", "Digsite pendant", 
            TeleportType.ITEM, "Rub", "Digsite pendant", null, null, null, null));
        teleports.add(createTeleport("digsite_pendant_lithkren", "Lithkren", "Digsite pendant", 
            TeleportType.ITEM, "Rub", "Digsite pendant", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Necklace of Passage teleport definitions.
     */
    private List<TeleportDefinition> createNecklaceOfPassageTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Necklace of passage teleports
        teleports.add(createTeleport("necklace_passage_wizards_tower", "Wizards' Tower", "Necklace of passage", 
            TeleportType.ITEM, "Rub", "Necklace of passage", null, null, null, null));
        teleports.add(createTeleport("necklace_passage_jorrals_outpost", "Jorral's Outpost", "Necklace of passage", 
            TeleportType.ITEM, "Rub", "Necklace of passage", null, null, null, null));
        teleports.add(createTeleport("necklace_passage_desert_eagle", "Desert eagle station", "Necklace of passage", 
            TeleportType.ITEM, "Rub", "Necklace of passage", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Burning Amulet teleport definitions.
     */
    private List<TeleportDefinition> createBurningAmuletTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Burning amulet teleports (wilderness level text not included)
        teleports.add(createTeleport("burning_amulet_chaos_temple", "Chaos Temple", "Burning amulet", 
            TeleportType.ITEM, "Rub", "Burning amulet", null, null, null, null));
        teleports.add(createTeleport("burning_amulet_bandit_camp", "Bandit Camp", "Burning amulet", 
            TeleportType.ITEM, "Rub", "Burning amulet", null, null, null, null));
        teleports.add(createTeleport("burning_amulet_lava_maze", "Lava Maze", "Burning amulet", 
            TeleportType.ITEM, "Rub", "Burning amulet", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Ring of Returning teleport definitions.
     */
    private List<TeleportDefinition> createRingOfReturningTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Ring of returning teleports (just one: Respawn point)
        teleports.add(createTeleport("ring_returning_respawn", "Respawn", "Ring of returning", 
            TeleportType.ITEM, "Rub", "Ring of returning", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Treasure Trail Scrolls teleport definitions.
     */
    private List<TeleportDefinition> createTreasureTrailScrollsTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Treasure Trail Scrolls teleports
        teleports.add(createTeleport("scroll_nardah", "Nardah teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Nardah teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_digsite", "Digsite teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Digsite teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_feldip_hills", "Feldip hills teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Feldip hills teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_lunar_isle", "Lunar isle teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Lunar isle teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_mortton", "Mort'ton teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Mort'ton teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_pest_control", "Pest control teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Pest control teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_piscatoris", "Piscatoris teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Piscatoris teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_tai_bwo_wannai", "Tai bwo wannai teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Tai bwo wannai teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_iorwerth_camp", "Iorwerth camp teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Iorwerth camp teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_mos_leharmless", "Mos le'harmless teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Mos le'harmless teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_lumberyard", "Lumberyard teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Lumberyard teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_zulandra", "Zul-andra teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Zul-andra teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_key_master", "Key master teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Key master teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_revenant_cave", "Revenant cave teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Revenant cave teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_watson", "Watson teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Watson teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_guthixian_temple", "Guthixian temple teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Guthixian temple teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_spider_cave", "Spider cave teleport", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Spider cave teleport", null, null, null, null));
        teleports.add(createTeleport("scroll_colossal_wyrm", "Colossal wyrm teleport scroll", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Colossal wyrm teleport scroll", null, null, null, null));
        teleports.add(createTeleport("scroll_chasm", "Chasm teleport scroll", "Treasure Trail Scrolls", 
            TeleportType.ITEM, "Teleport", "Chasm teleport scroll", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Chronicle teleport definitions.
     */
    private List<TeleportDefinition> createChronicleTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Chronicle teleports (teleports to Champion's Guild)
        teleports.add(createTeleport("chronicle_champions_guild", "Champion's Guild", "Chronicle", 
            TeleportType.ITEM, "Teleport", "Chronicle", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Giantsoul amulet teleport definitions.
     */
    private List<TeleportDefinition> createGiantsoulAmuletTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Giantsoul amulet teleports (Rub option opens submenu with these destinations)
        teleports.add(createTeleport("giantsoul_bryophyta", "Bryophyta", "Giantsoul amulet", 
            TeleportType.ITEM, "Rub", "Giantsoul amulet", null, null, null, null));
        teleports.add(createTeleport("giantsoul_obor", "Obor", "Giantsoul amulet", 
            TeleportType.ITEM, "Rub", "Giantsoul amulet", null, null, null, null));
        teleports.add(createTeleport("giantsoul_branda_eldric", "Branda and Eldric", "Giantsoul amulet", 
            TeleportType.ITEM, "Rub", "Giantsoul amulet", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Xeric's talisman teleport definitions.
     */
    private List<TeleportDefinition> createXericsTalismanTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        
        // Xeric's talisman teleports (Rub option opens submenu with these destinations)
        teleports.add(createTeleport("xerics_lookout", "Xeric's Lookout", "Xeric's talisman", 
            TeleportType.ITEM, "Rub", "Xeric's talisman", null, null, null, null));
        teleports.add(createTeleport("xerics_glade", "Xeric's Glade", "Xeric's talisman", 
            TeleportType.ITEM, "Rub", "Xeric's talisman", null, null, null, null));
        teleports.add(createTeleport("xerics_inferno", "Xeric's Inferno", "Xeric's talisman", 
            TeleportType.ITEM, "Rub", "Xeric's talisman", null, null, null, null));
        teleports.add(createTeleport("xerics_heart", "Xeric's Heart", "Xeric's talisman", 
            TeleportType.ITEM, "Rub", "Xeric's talisman", null, null, null, null));
        teleports.add(createTeleport("xerics_honour", "Xeric's Honour", "Xeric's talisman", 
            TeleportType.ITEM, "Rub", "Xeric's talisman", null, null, null, null));
        
        return teleports;
    }
    
    /**
     * Create Primio teleport definitions.
     */
    private List<TeleportDefinition> createPrimioTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        // Primio teleports (Travel option on Primio NPC)
        teleports.add(createTeleport("primio_varrock_quetzal", "Varrock Quetzal", "Primio",
            TeleportType.OBJECT, "Travel", "Primio", null, null, null, null));
        return teleports;
    }
    
    /**
     * Create Ring of the elements teleport definitions.
     */
    private List<TeleportDefinition> createRingOfElementsTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        // Ring of the elements teleports (Rub option opens submenu with these destinations)
        teleports.add(createTeleport("ring_elements_air_altar", "Air Altar", "Ring of the elements",
            TeleportType.ITEM, "Rub", "Ring of the elements", null, null, null, null));
        teleports.add(createTeleport("ring_elements_water_altar", "Water Altar", "Ring of the elements",
            TeleportType.ITEM, "Rub", "Ring of the elements", null, null, null, null));
        teleports.add(createTeleport("ring_elements_earth_altar", "Earth Altar", "Ring of the elements",
            TeleportType.ITEM, "Rub", "Ring of the elements", null, null, null, null));
        teleports.add(createTeleport("ring_elements_fire_altar", "Fire Altar", "Ring of the elements",
            TeleportType.ITEM, "Rub", "Ring of the elements", null, null, null, null));
        return teleports;
    }
    
    /**
     * Create Charter Ships teleport definitions.
     */
    private List<TeleportDefinition> createCharterShipsTeleports()
    {
        List<TeleportDefinition> teleports = new ArrayList<>();
        // Charter Ships destinations
        teleports.add(createTeleport("charter_port_sarim", "Port Sarim", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_brimhaven", "Brimhaven", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_catherby", "Catherby", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_musa_point", "Musa Point", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_port_khazard", "Port Khazard", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_corsair_cove", "Corsair Cove", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_port_piscarilius", "Port Piscarilius", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_lands_end", "Land's End", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_aldarin", "Aldarin", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_sunset_coast", "Sunset Coast", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        teleports.add(createTeleport("charter_civitas_illa_fortis", "Civitas illa Fortis", "Charter Ships",
            TeleportType.OBJECT, null, null, null, null, null, null));
        return teleports;
    }
    
    /**
     * Helper to create a teleport definition.
     */
    private TeleportDefinition createTeleport(String id, String name, String category, TeleportType type,
        String menuOption, String menuTarget, Integer itemId, Integer spellId, Integer objectId, Integer npcId)
    {
        TeleportDefinition teleport = new TeleportDefinition(id, name, category, type);
        teleport.setMenuOption(menuOption);
        teleport.setMenuTarget(menuTarget);
        teleport.setItemId(itemId);
        teleport.setSpellId(spellId);
        teleport.setObjectId(objectId);
        teleport.setNpcId(npcId);
        return teleport;
    }
    
    /**
     * Get all teleport categories.
     */
    public List<String> getCategories()
    {
        return new ArrayList<>(teleportsByCategory.keySet());
    }
    
    /**
     * Get all teleports in a category.
     */
    public List<TeleportDefinition> getTeleportsByCategory(String category)
    {
        return teleportsByCategory.getOrDefault(category, new ArrayList<>());
    }
    
    /**
     * Get a teleport by ID.
     */
    public TeleportDefinition getTeleportById(String id)
    {
        return teleportsById.get(id);
    }
    
    /**
     * Get all teleport definitions.
     */
    public List<TeleportDefinition> getAllTeleports()
    {
        return new ArrayList<>(teleportsById.values());
    }
    
    /**
     * Get the group for a teleport category.
     * Groups: "Spellbooks", "Jewellery", "Miscellaneous"
     */
    public String getGroupForCategory(String category)
    {
        // Spellbooks
        if (category.equals("Standard Spellbook") || 
            category.equals("Ancient Magicks") || 
            category.equals("Lunar Spellbook") || 
            category.equals("Arceuus Spellbook"))
        {
            return "Spellbooks";
        }
        
        // Jewellery (rings, amulets, pendants, necklaces, bracelets, talismans)
        if (category.equals("Ring of dueling") ||
            category.equals("Games necklace") ||
            category.equals("Combat bracelet") ||
            category.equals("Skills necklace") ||
            category.equals("Amulet of glory") ||
            category.equals("Ring of wealth") ||
            category.equals("Slayer ring") ||
            category.equals("Digsite pendant") ||
            category.equals("Necklace of passage") ||
            category.equals("Burning amulet") ||
            category.equals("Ring of returning") ||
            category.equals("Giantsoul amulet") ||
            category.equals("Xeric's talisman") ||
            category.equals("Ring of the elements") ||
            category.equals("Pendant of ates"))
        {
            return "Jewellery";
        }
        
        // Miscellaneous (everything else)
        return "Miscellaneous";
    }
    
    /**
     * Get all categories grouped by their group type.
     * Returns a map of group name -> list of categories in that group.
     */
    public java.util.Map<String, List<String>> getCategoriesByGroup()
    {
        java.util.Map<String, List<String>> grouped = new java.util.HashMap<>();
        grouped.put("Spellbooks", new java.util.ArrayList<>());
        grouped.put("Jewellery", new java.util.ArrayList<>());
        grouped.put("Miscellaneous", new java.util.ArrayList<>());
        
        for (String category : getCategories())
        {
            String group = getGroupForCategory(category);
            grouped.get(group).add(category);
        }
        
        return grouped;
    }
}

