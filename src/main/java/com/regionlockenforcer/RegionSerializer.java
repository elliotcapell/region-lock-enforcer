package com.regionlockenforcer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Utility class for serializing/deserializing Region objects to/from strings.
 * Uses a simple format: "name|tile1;tile2;tile3" where tiles are "x,y,plane"
 */
@Slf4j
public class RegionSerializer
{

    /**
     * Serialize a WorldPoint to a string format: "x,y,plane"
     */
    public static String worldPointToString(WorldPoint wp)
    {
        if (wp == null) return null;
        return wp.getX() + "," + wp.getY() + "," + wp.getPlane();
    }

    /**
     * Deserialize a WorldPoint from a string format: "x,y,plane"
     */
    public static WorldPoint stringToWorldPoint(String str)
    {
        if (str == null || str.isEmpty()) return null;
        try
        {
            String[] parts = str.split(",");
            if (parts.length != 3) return null;
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int plane = Integer.parseInt(parts[2].trim());
            return new WorldPoint(x, y, plane);
        }
        catch (Exception e)
        {
            log.warn("Failed to parse WorldPoint from string: {}", str, e);
            return null;
        }
    }

    /**
     * Serialize a set of WorldPoints to a semicolon-separated string.
     */
    public static String serializeTiles(Set<WorldPoint> tiles)
    {
        if (tiles == null || tiles.isEmpty()) return "";
        return tiles.stream()
                .map(RegionSerializer::worldPointToString)
                .filter(s -> s != null)
                .collect(Collectors.joining(";"));
    }

    /**
     * Deserialize a set of WorldPoints from a semicolon-separated string.
     */
    public static Set<WorldPoint> deserializeTiles(String str)
    {
        Set<WorldPoint> tiles = new HashSet<>();
        if (str == null || str.isEmpty()) return tiles;
        try
        {
            String[] parts = str.split(";");
            for (String part : parts)
            {
                if (part.isEmpty()) continue;
                WorldPoint wp = stringToWorldPoint(part);
                if (wp != null) tiles.add(wp);
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to deserialize tiles from string: {}", str, e);
        }
        return tiles;
    }

    /**
     * Serialize a set of strings to a semicolon-separated string.
     */
    public static String serializeStrings(Set<String> strings)
    {
        if (strings == null || strings.isEmpty()) return "";
        return String.join(";", strings);
    }
    
    /**
     * Deserialize a set of strings from a semicolon-separated string.
     */
    public static Set<String> deserializeStrings(String str)
    {
        Set<String> strings = new HashSet<>();
        if (str == null || str.isEmpty()) return strings;
        try
        {
            String[] parts = str.split(";");
            for (String part : parts)
            {
                if (!part.isEmpty()) strings.add(part);
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to deserialize strings from string: {}", str, e);
        }
        return strings;
    }

    /**
     * Serialize a Region to string format: "name|boundaryTiles|innerTiles|teleportWhitelist"
     * Format: "name|tile1;tile2;tile3|tile4;tile5;tile6|teleport1;teleport2"
     * Special characters in name are escaped: | becomes ||, ; becomes |;
     */
    public static String serializeRegion(Region region)
    {
        if (region == null) return "";
        try
        {
            String name = region.getName() != null ? region.getName() : "Untitled Region";
            // Escape special characters: | -> ||, ; -> |;
            name = name.replace("|", "||").replace(";", "|;");
            String boundaryTiles = serializeTiles(region.getBoundaryTiles());
            String innerTiles = serializeTiles(region.getInnerTiles());
            String teleportWhitelist = serializeStrings(region.getTeleportWhitelist());
            return name + "|" + boundaryTiles + "|" + innerTiles + "|" + teleportWhitelist;
        }
        catch (Exception e)
        {
            log.warn("Failed to serialize Region", e);
            return "";
        }
    }

    /**
     * Deserialize a Region from string format.
     * Supports old formats for backward compatibility:
     * - Old format 1: "name|tiles" (just boundary tiles)
     * - Old format 2: "name|boundaryTiles|innerTiles" (no teleport whitelist)
     * - New format: "name|boundaryTiles|innerTiles|teleportWhitelist"
     */
    public static Region deserializeRegion(String str)
    {
        if (str == null || str.isEmpty()) return new Region();
        try
        {
            int firstPipeIndex = str.indexOf('|');
            if (firstPipeIndex == -1) return new Region();
            
            String name = str.substring(0, firstPipeIndex);
            // Unescape special characters: || -> |, |; -> ;
            name = name.replace("|;", ";").replace("||", "|");
            
            String rest = str.substring(firstPipeIndex + 1);
            Region region = new Region(name);
            
            // Split by | to get all parts
            String[] parts = rest.split("\\|", -1); // -1 to keep trailing empty strings
            
            if (parts.length >= 3)
            {
                // New format: boundaryTiles|innerTiles|teleportWhitelist
                region.setBoundaryTiles(deserializeTiles(parts[0]));
                region.setInnerTiles(deserializeTiles(parts[1]));
                if (!parts[2].isEmpty())
                {
                    region.setTeleportWhitelist(deserializeStrings(parts[2]));
                }
                else
                {
                    region.setTeleportWhitelist(new HashSet<>());
                }
            }
            else if (parts.length == 2)
            {
                // Old format 2: boundaryTiles|innerTiles (no teleport whitelist)
                region.setBoundaryTiles(deserializeTiles(parts[0]));
                region.setInnerTiles(deserializeTiles(parts[1]));
                region.setTeleportWhitelist(new HashSet<>());
            }
            else
            {
                // Old format 1: just tiles (treat as boundary tiles for backward compatibility)
                region.setBoundaryTiles(deserializeTiles(rest));
                region.setInnerTiles(new HashSet<>());
                region.setTeleportWhitelist(new HashSet<>());
            }
            
            // Pre-compute the cache so it's ready immediately (no lag on first access)
            region.ensureCacheComputed();
            
            return region;
        }
        catch (Exception e)
        {
            log.warn("Failed to deserialize Region from string: {}", str, e);
            return new Region();
        }
    }

    /**
     * Serialize a list of Regions to string format.
     * Regions are separated by newlines.
     */
    public static String serializeRegions(List<Region> regions)
    {
        if (regions == null || regions.isEmpty()) return "";
        return regions.stream()
                .map(RegionSerializer::serializeRegion)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Deserialize a list of Regions from string format.
     * Regions are separated by newlines.
     */
    public static List<Region> deserializeRegions(String str)
    {
        List<Region> regions = new ArrayList<>();
        if (str == null || str.isEmpty()) return regions;
        try
        {
            String[] lines = str.split("\n");
            for (String line : lines)
            {
                if (line.isEmpty()) continue;
                Region region = deserializeRegion(line);
                if (region != null)
                {
                    // Cache is already pre-computed in deserializeRegion
                    regions.add(region);
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to deserialize regions from string: {}", str, e);
        }
        return regions;
    }
}

