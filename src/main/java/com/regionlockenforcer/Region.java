package com.regionlockenforcer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

/**
 * Represents a named region containing one or more borders and a teleport whitelist.
 */
@Data
public class Region
{
    private String name;
    private List<Border> borders = new CopyOnWriteArrayList<>();
    private Set<String> teleportWhitelist = ConcurrentHashMap.newKeySet();

    // Cached union of all clickable tiles across every border
    private transient Set<WorldPoint> cachedClickableTiles;
    // Cached union of all inner tiles across every border (for quick finished checks)
    private transient Set<WorldPoint> cachedAllInnerTiles;

    public Region()
    {
        this.name = "Untitled Region";
    }

    public Region(String name)
    {
        this.name = name != null ? name : "Untitled Region";
    }

    public List<Border> getBorders()
    {
        if (borders == null)
        {
            borders = new CopyOnWriteArrayList<>();
        }
        return borders;
    }

    public Border getPrimaryBorder()
    {
        if (borders == null || borders.isEmpty())
        {
            return null;
        }
        return borders.get(0);
    }

    public Border addBorder(String preferredName)
    {
        String borderName = preferredName;
        if (borderName == null || borderName.trim().isEmpty())
        {
            borderName = nextDefaultBorderName();
        }
        Border border = new Border(borderName.trim());
        attachBorder(border);
        getBorders().add(border);
        invalidateClickableTilesCache();
        return border;
    }

    public void removeBorder(Border border)
    {
        if (border == null)
        {
            return;
        }
        getBorders().remove(border);
        invalidateClickableTilesCache();
    }

    public void setBorders(List<Border> borders)
    {
        this.borders = borders != null ? new CopyOnWriteArrayList<>(borders) : new CopyOnWriteArrayList<>();
        if (this.borders != null)
        {
            for (Border border : this.borders)
            {
                attachBorder(border);
            }
        }
        invalidateClickableTilesCache();
    }

    public Set<WorldPoint> getAllBoundaryTiles()
    {
        Set<WorldPoint> all = new HashSet<>();
        for (Border border : getBorders())
        {
            all.addAll(border.getBoundaryTiles());
        }
        return all;
    }

    public Set<String> getTeleportWhitelist()
    {
        if (teleportWhitelist == null)
        {
            teleportWhitelist = ConcurrentHashMap.newKeySet();
        }
        return teleportWhitelist;
    }

    public Set<WorldPoint> getAllClickableTiles()
    {
        Set<WorldPoint> cached = cachedClickableTiles;
        if (cached != null)
        {
            return cached;
        }

        Set<WorldPoint> all = new HashSet<>();
        for (Border border : getBorders())
        {
            all.addAll(border.getAllClickableTiles());
        }
        cachedClickableTiles = all;
        return all;
    }
    
    public void invalidateClickableTilesCache()
    {
        cachedClickableTiles = null;
        cachedAllInnerTiles = null;
    }
    
    public void ensureCacheComputed()
    {
        for (Border border : getBorders())
        {
            border.ensureCacheComputed();
        }
        getAllClickableTiles();
        getAllInnerTiles();
    }

    public Set<WorldPoint> getAllInnerTiles()
    {
        Set<WorldPoint> cached = cachedAllInnerTiles;
        if (cached != null)
        {
            return cached;
        }

        Set<WorldPoint> all = new HashSet<>();
        for (Border border : getBorders())
        {
            all.addAll(border.getInnerTiles());
        }
        cachedAllInnerTiles = all;
        return all;
    }

    public void setTeleportWhitelist(Set<String> teleportWhitelist)
    {
        Set<String> newSet = ConcurrentHashMap.newKeySet();
        if (teleportWhitelist != null)
        {
            newSet.addAll(teleportWhitelist);
        }
        this.teleportWhitelist = newSet;
    }

    // Legacy helpers retained for compatibility. They operate on the primary border.
    public Set<WorldPoint> getBoundaryTiles() { Border b = getPrimaryBorder(); return b != null ? b.getBoundaryTiles() : new HashSet<>(); }
    public Set<WorldPoint> getInnerTiles() { Border b = getPrimaryBorder(); return b != null ? b.getInnerTiles() : new HashSet<>(); }
    public boolean addTile(WorldPoint tile) { Border b = getPrimaryBorder(); return b != null && b.addTile(tile); }
    public boolean removeTile(WorldPoint tile) { Border b = getPrimaryBorder(); return b != null && b.removeTile(tile); }
    public boolean toggleTile(WorldPoint tile) { Border b = getPrimaryBorder(); return b != null && b.toggleTile(tile); }
    public int addArea(int startX, int startY, int plane, int size) { Border b = getPrimaryBorder(); return b != null ? b.addArea(startX, startY, plane, size) : 0; }
    public int addChunk(int chunkX, int chunkY, int plane) { Border b = getPrimaryBorder(); return b != null ? b.addChunk(chunkX, chunkY, plane) : 0; }
    public int removeArea(int startX, int startY, int plane, int size) { Border b = getPrimaryBorder(); return b != null ? b.removeArea(startX, startY, plane, size) : 0; }
    public int removeChunk(int chunkX, int chunkY, int plane) { Border b = getPrimaryBorder(); return b != null ? b.removeChunk(chunkX, chunkY, plane) : 0; }
    public boolean isAreaFullyContained(int startX, int startY, int plane, int size) { Border b = getPrimaryBorder(); return b != null && b.isAreaFullyContained(startX, startY, plane, size); }
    public boolean isChunkFullyContained(int chunkX, int chunkY, int plane) { Border b = getPrimaryBorder(); return b != null && b.isChunkFullyContained(chunkX, chunkY, plane); }
    public boolean hasAnyTileInArea(int startX, int startY, int plane, int size) { Border b = getPrimaryBorder(); return b != null && b.hasAnyTileInArea(startX, startY, plane, size); }
    public void setBoundaryTiles(Set<WorldPoint> boundaryTiles) { Border b = getPrimaryBorder(); if (b != null) { b.setBoundaryTiles(boundaryTiles); invalidateClickableTilesCache(); } }
    public void setInnerTiles(Set<WorldPoint> innerTiles) { Border b = getPrimaryBorder(); if (b != null) { b.setInnerTiles(innerTiles); invalidateClickableTilesCache(); } }

    private void attachBorder(Border border)
    {
        if (border != null)
        {
            border.setOnChange(this::invalidateClickableTilesCache);
        }
    }

    private String nextDefaultBorderName()
    {
        int index = (borders != null ? borders.size() : 0) + 1;
        return "Border " + index;
    }
}

