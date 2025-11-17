package com.regionlockenforcer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

/**
 * Represents a named region containing boundary tiles and inner tiles, and a teleport whitelist.
 * Boundary tiles define the border shape and are used for drawing borders.
 * Inner tiles are all tiles inside the boundary and are used for click blocking.
 */
@Data
public class Region
{
    private String name;
    private Set<WorldPoint> boundaryTiles = ConcurrentHashMap.newKeySet(); // Tiles that define the border
    private Set<WorldPoint> innerTiles = ConcurrentHashMap.newKeySet(); // All tiles inside the boundary (for click blocking)
    private Set<String> teleportWhitelist = ConcurrentHashMap.newKeySet(); // IDs of whitelisted teleports
    
    // Cached clickable tiles set - computed once and reused for performance
    // Null when cache is invalid, non-null when valid
    private transient Set<WorldPoint> cachedClickableTiles = null;

    public Region()
    {
        this.name = "Untitled Region";
    }

    public Region(String name)
    {
        this.name = name != null ? name : "Untitled Region";
    }

    public Set<WorldPoint> getBoundaryTiles()
    {
        if (boundaryTiles == null)
        {
            boundaryTiles = ConcurrentHashMap.newKeySet();
        }
        return boundaryTiles;
    }

    public Set<WorldPoint> getInnerTiles()
    {
        if (innerTiles == null)
        {
            innerTiles = ConcurrentHashMap.newKeySet();
        }
        return innerTiles;
    }

    public Set<String> getTeleportWhitelist()
    {
        if (teleportWhitelist == null)
        {
            teleportWhitelist = ConcurrentHashMap.newKeySet();
        }
        return teleportWhitelist;
    }

    /**
     * Add a boundary tile to this region.
     * @return true if the tile was added (wasn't already present)
     */
    public boolean addTile(WorldPoint tile)
    {
        boolean added = boundaryTiles.add(tile);
        if (added)
        {
            invalidateClickableTilesCache();
        }
        return added;
    }

    /**
     * Remove a boundary tile from this region.
     * @return true if the tile was removed (was present)
     */
    public boolean removeTile(WorldPoint tile)
    {
        boolean removed = boundaryTiles.remove(tile);
        if (removed)
        {
            invalidateClickableTilesCache();
        }
        return removed;
    }

    /**
     * Toggle a boundary tile in this region (add if absent, remove if present).
     * @return true if the tile is now present after toggle
     */
    public boolean toggleTile(WorldPoint tile)
    {
        if (boundaryTiles.contains(tile))
        {
            boundaryTiles.remove(tile);
            invalidateClickableTilesCache();
            return false;
        }
        else
        {
            boundaryTiles.add(tile);
            invalidateClickableTilesCache();
            return true;
        }
    }

    /**
     * Get all clickable tiles (boundary + inner tiles).
     * Used for determining click blocking.
     * This is cached for performance - the cache is invalidated when tiles change.
     */
    public Set<WorldPoint> getAllClickableTiles()
    {
        // Return cached version if available
        if (cachedClickableTiles != null)
        {
            return cachedClickableTiles;
        }
        
        // Compute and cache
        Set<WorldPoint> all = new HashSet<>(boundaryTiles);
        all.addAll(innerTiles);
        cachedClickableTiles = all;
        return all;
    }
    
    /**
     * Invalidate the cached clickable tiles set.
     * Should be called whenever boundaryTiles or innerTiles are modified.
     */
    private void invalidateClickableTilesCache()
    {
        cachedClickableTiles = null;
    }
    
    /**
     * Pre-compute the clickable tiles cache if it's not already computed.
     * This should be called after deserialization/import to ensure the cache is ready immediately.
     */
    public void ensureCacheComputed()
    {
        if (cachedClickableTiles == null)
        {
            getAllClickableTiles(); // This will compute and cache
        }
    }
    
    /**
     * Set boundary tiles and invalidate cache.
     * Overrides Lombok-generated setter to ensure cache invalidation.
     */
    public void setBoundaryTiles(Set<WorldPoint> boundaryTiles)
    {
        Set<WorldPoint> newSet = ConcurrentHashMap.newKeySet();
        if (boundaryTiles != null)
        {
            newSet.addAll(boundaryTiles);
        }
        this.boundaryTiles = newSet;
        invalidateClickableTilesCache();
    }
    
    /**
     * Set inner tiles and invalidate cache.
     * Overrides Lombok-generated setter to ensure cache invalidation.
     */
    public void setInnerTiles(Set<WorldPoint> innerTiles)
    {
        Set<WorldPoint> newSet = ConcurrentHashMap.newKeySet();
        if (innerTiles != null)
        {
            newSet.addAll(innerTiles);
        }
        this.innerTiles = newSet;
        invalidateClickableTilesCache();
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

    /**
     * Add all tiles in a square area to the border.
     *
     * @param startX the lower-left tile X coordinate
     * @param startY the lower-left tile Y coordinate
     * @param plane the plane
     * @param size the width/height of the square in tiles
     * @return number of tiles added
     */
    public int addArea(int startX, int startY, int plane, int size)
    {
        if (size <= 0)
        {
            return 0;
        }

        int added = 0;
        
        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                WorldPoint tile = new WorldPoint(startX + x, startY + y, plane);
                if (boundaryTiles.add(tile))
                {
                    added++;
                }
            }
        }
        
        // Clear inner tiles when boundary changes
        innerTiles.clear();
        invalidateClickableTilesCache();
        return added;
    }

    /**
     * Add all tiles in a 64x64 chunk to the border.
     * A chunk is defined by its base coordinates (chunkX, chunkY) where
     * chunkX = worldX / 64 and chunkY = worldY / 64.
     */
    public int addChunk(int chunkX, int chunkY, int plane)
    {
        return addArea(chunkX * 64, chunkY * 64, plane, 64);
    }

    /**
     * Remove all tiles in a 64x64 chunk from the border.
     * 
     * @param chunkX The chunk X coordinate (worldX / 64)
     * @param chunkY The chunk Y coordinate (worldY / 64)
     * @param plane The plane (0 = surface, 1 = underground, etc.)
     * @return The number of tiles removed
     */
    public int removeArea(int startX, int startY, int plane, int size)
    {
        if (size <= 0)
        {
            return 0;
        }

        int removed = 0;
        
        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                WorldPoint tile = new WorldPoint(startX + x, startY + y, plane);
                if (boundaryTiles.remove(tile))
                {
                    removed++;
                }
            }
        }
        
        // Clear inner tiles when boundary changes
        innerTiles.clear();
        invalidateClickableTilesCache();
        return removed;
    }

    public int removeChunk(int chunkX, int chunkY, int plane)
    {
        return removeArea(chunkX * 64, chunkY * 64, plane, 64);
    }

    /**
     * Check if a chunk is fully contained in the border (all 64x64 tiles are present).
     * 
     * @param chunkX The chunk X coordinate (worldX / 64)
     * @param chunkY The chunk Y coordinate (worldY / 64)
     * @param plane The plane (0 = surface, 1 = underground, etc.)
     * @return true if all tiles in the chunk are in the border
     */
    public boolean isAreaFullyContained(int startX, int startY, int plane, int size)
    {
        if (size <= 0)
        {
            return false;
        }
        
        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                WorldPoint tile = new WorldPoint(startX + x, startY + y, plane);
                if (!boundaryTiles.contains(tile))
                {
                    return false;
                }
            }
        }
        
        return true;
    }

    public boolean isChunkFullyContained(int chunkX, int chunkY, int plane)
    {
        return isAreaFullyContained(chunkX * 64, chunkY * 64, plane, 64);
    }
}

