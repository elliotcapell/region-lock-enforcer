package com.regionlockenforcer;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Utility class for serializing/deserializing Region objects to/from strings.
 * Uses a versioned format to support multiple borders per region.
 */
@Slf4j
public class RegionSerializer
{
    private static final String VERSION_PREFIX = "v2|";

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
     * Serialize a Region to a versioned string.
     */
    public static String serializeRegion(Region region, Gson gson)
    {
        if (region == null) return "";
        try
        {
            RegionData data = new RegionData();
            data.name = region.getName() != null ? region.getName() : "Untitled Region";
            data.teleportWhitelist = new ArrayList<>(region.getTeleportWhitelist() != null ? region.getTeleportWhitelist() : new HashSet<>());
            data.borders = new ArrayList<>();

            for (Border border : region.getBorders())
            {
                BorderData bd = new BorderData();
                bd.name = border.getName();
                bd.propStyle = border.getPropStyle() != null ? border.getPropStyle().name() : null;
                bd.renderMode = border.getRenderMode() != null ? border.getRenderMode().name() : null;
                bd.lineColor = border.getLineColor() != null ? border.getLineColor().getRGB() : null;
                bd.boundaryTiles = border.getBoundaryTiles().stream()
                        .map(wp -> new TileData(wp.getX(), wp.getY(), wp.getPlane()))
                        .collect(Collectors.toList());
                bd.innerTiles = border.getInnerTiles().stream()
                        .map(wp -> new TileData(wp.getX(), wp.getY(), wp.getPlane()))
                        .collect(Collectors.toList());
                data.borders.add(bd);
            }

            return VERSION_PREFIX + gson.toJson(data);
        }
        catch (Exception e)
        {
            log.warn("Failed to serialize Region", e);
            return "";
        }
    }

    /**
     * Deserialize a Region from string format.
     * Supports v2 JSON format and legacy pipe-delimited formats.
     */
    public static Region deserializeRegion(String str, Gson gson)
    {
        if (str == null || str.isEmpty()) return new Region();
        try
        {
            // New format: v2|{json}
            if (str.startsWith(VERSION_PREFIX))
            {
                String json = str.substring(VERSION_PREFIX.length());
                RegionData data = gson.fromJson(json, RegionData.class);
                Region region = new Region(data != null && data.name != null ? data.name : "Untitled Region");

                List<Border> borders = new ArrayList<>();
                if (data != null && data.borders != null && !data.borders.isEmpty())
                {
                    for (BorderData bd : data.borders)
                    {
                        Border border = new Border(bd != null && bd.name != null ? bd.name : "Border");
                        if (bd != null)
                        {
                            Set<WorldPoint> boundaryTiles = bd.boundaryTiles != null
                                    ? bd.boundaryTiles.stream()
                                        .map(td -> new WorldPoint(td.x, td.y, td.plane))
                                        .collect(Collectors.toSet())
                                    : new HashSet<>();
                            Set<WorldPoint> innerTiles = bd.innerTiles != null
                                    ? bd.innerTiles.stream()
                                        .map(td -> new WorldPoint(td.x, td.y, td.plane))
                                        .collect(Collectors.toSet())
                                    : new HashSet<>();
                            border.setBoundaryTiles(boundaryTiles);
                            border.setInnerTiles(innerTiles);
                            RegionLockEnforcerConfig.PropStyle mappedStyle = mapPropStyle(bd.propStyle);
                            if (mappedStyle != null)
                            {
                                border.setPropStyle(mappedStyle);
                            }

                            Border.RenderMode mappedMode = mapRenderMode(bd.renderMode);
                            if (mappedMode == null && mappedStyle != null)
                            {
                                // If a prop style exists but render mode missing, default to PROPS
                                mappedMode = Border.RenderMode.PROPS;
                            }
                            if (mappedMode != null)
                            {
                                border.setRenderMode(mappedMode);
                            }
                            if (bd.lineColor != null)
                            {
                                border.setLineColor(new java.awt.Color(bd.lineColor, true));
                            }
                        }
                        borders.add(border);
                    }
                }

                region.setBorders(borders);
                region.setTeleportWhitelist(data != null && data.teleportWhitelist != null
                        ? new HashSet<>(data.teleportWhitelist)
                        : new HashSet<>());
                region.ensureCacheComputed();
                return region;
            }

            // Legacy formats below
            int firstPipeIndex = str.indexOf('|');
            if (firstPipeIndex == -1) return new Region();

            String name = str.substring(0, firstPipeIndex);
            name = name.replace("|;", ";").replace("||", "|");

            String rest = str.substring(firstPipeIndex + 1);
            Region region = new Region(name);

            String[] parts = rest.split("\\|", -1);

            if (parts.length >= 3)
            {
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
                region.setBoundaryTiles(deserializeTiles(parts[0]));
                region.setInnerTiles(deserializeTiles(parts[1]));
                region.setTeleportWhitelist(new HashSet<>());
            }
            else
            {
                region.setBoundaryTiles(deserializeTiles(rest));
                region.setInnerTiles(new HashSet<>());
                region.setTeleportWhitelist(new HashSet<>());
            }

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
    public static String serializeRegions(List<Region> regions, Gson gson)
    {
        if (regions == null || regions.isEmpty()) return "";
        return regions.stream()
                .map(region -> RegionSerializer.serializeRegion(region, gson))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Deserialize a list of Regions from string format.
     * Regions are separated by newlines.
     */
    public static List<Region> deserializeRegions(String str, Gson gson)
    {
        List<Region> regions = new ArrayList<>();
        if (str == null || str.isEmpty()) return regions;
        try
        {
            String[] lines = str.split("\n");
            for (String line : lines)
            {
                if (line.isEmpty()) continue;
                Region region = deserializeRegion(line, gson);
                if (region != null)
                {
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

    private static class RegionData
    {
        String name;
        List<BorderData> borders;
        List<String> teleportWhitelist;
    }

    private static class BorderData
    {
        String name;
        List<TileData> boundaryTiles;
        List<TileData> innerTiles;
        String propStyle;
        String renderMode;
        Integer lineColor;
    }

    private static class TileData
    {
        int x;
        int y;
        int plane;

        @SuppressWarnings("unused")
        TileData() { }

        TileData(int x, int y, int plane)
        {
            this.x = x;
            this.y = y;
            this.plane = plane;
        }
    }

    private static RegionLockEnforcerConfig.PropStyle mapPropStyle(String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return null;
        }
        try
        {
            return RegionLockEnforcerConfig.PropStyle.valueOf(raw);
        }
        catch (IllegalArgumentException ignored)
        {
            // legacy mappings
            switch (raw)
            {
                case "OBJ_58598":
                case "ROCK_SMALL":
                case "CRYSTAL_PURPLE":
                    return RegionLockEnforcerConfig.PropStyle.SEA_ROCK;
                case "OBJ_17319":
                case "ROCK_SMOOTH":
                    return RegionLockEnforcerConfig.PropStyle.ROCK_WALL;
                case "OBJ_6745":
                case "SKULL_PILE":
                    return RegionLockEnforcerConfig.PropStyle.IRON_FENCE;
                case "OBJ_42889":
                    return RegionLockEnforcerConfig.PropStyle.LOG_FENCE;
                default:
                    return null;
            }
        }
    }

    private static Border.RenderMode mapRenderMode(String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return null;
        }
        try
        {
            return Border.RenderMode.valueOf(raw);
        }
        catch (IllegalArgumentException ignored)
        {
            // legacy names from old config: LINES / ROCKS / BOTH
            switch (raw)
            {
                case "LINES":
                    return Border.RenderMode.LINES;
                case "ROCKS":
                case "BOTH": // best-effort: prefer props when both was set
                    return Border.RenderMode.PROPS;
                default:
                    return null;
            }
        }
    }
}

