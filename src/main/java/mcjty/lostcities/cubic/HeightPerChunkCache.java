package mcjty.lostcities.cubic;

import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.varia.ChunkCoord;

import java.util.HashMap;
import java.util.Map;

public class HeightPerChunkCache {
    private static Map<ChunkCoord, Integer> cache = new HashMap<>();

    public static void add(ChunkCoord coord, int y) {
        // if(cache.containsKey(coord)) return;
        cache.put(coord, y);
    }

    public static Integer getCityLevel(ChunkCoord coord) {
        if(!cache.containsKey(coord)) return null;
        return cache.get(coord);
    }

    public static Integer getCityLevel(BuildingInfo info) {
        // TODO: dimension
        return getCityLevel(new ChunkCoord(0, info.chunkX, info.chunkX));
    }
}
