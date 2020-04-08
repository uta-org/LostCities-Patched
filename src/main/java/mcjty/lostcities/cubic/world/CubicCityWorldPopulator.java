package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.terra121.EarthTerrainProcessor;
import io.github.terra121.dataset.HeightmapModel;
import io.github.terra121.populator.RoadGenerator;
import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.api.*;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Railway;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import mcjty.lostcities.dimensions.world.lost.cityassets.CityStyle;
import mcjty.lostcities.dimensions.world.lost.cityassets.WorldStyle;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.varia.Coord;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import org.spongepowered.noise.module.source.Perlin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static mcjty.lostcities.cubic.world.CubicCityUtils.*;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.*;
import static mcjty.lostcities.cubic.world.generators.Utils.fixTorches;

public class CubicCityWorldPopulator implements ICommonGeneratorProvider, ICubicPopulator, Comparable<Object> {

    private static Map<CubePos, CubicHeightmap> cachedHeightmaps = new HashMap<>();
    private static Map<ChunkCoord, Integer> groundLevels = new HashMap<>();

    // Needed fields
    private Random random;
    private int dimensionId;
    private long seed;

    // Singleton
    public static CubicCityWorldPopulator provider;

    private int currentChunkY;
    private HeightmapModel currentModel;

    public CubicCityWorldPopulator() {
        // TODO: Refactor this
        if (provider == null) {
            provider = this;
        }

        random = worldObj.rand;

        dimensionId = worldObj.provider.getDimension();
        seed = worldObj.provider.getSeed();
    }

    @Override
    public void generate(World world, Random random, CubePos pos, Biome biome) {
        spawnInChunk(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int compareTo(Object o) {
        return 2;
    }

    private void spawnInChunk(int chunkX, int chunkY, int chunkZ) {
        currentChunkY = chunkY;

        // We need this in order to generate once per column
        ChunkCoord chunkCoord = new ChunkCoord(dimensionId, chunkX, chunkZ);

        HeightmapModel model = canSpawnInChunk(chunkX, chunkY, chunkZ);
        if (model != null && !groundLevels.containsKey(chunkCoord)) {
            // TODO + 8?
            int x = chunkX * 16; // + 8;
            int z = chunkZ * 16; // + 8;

            currentModel = model;

            BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, this);

            CubicHeightmap heightmap = (CubicHeightmap) getHeightmap(info.chunkX, info.chunkZ);

            int y = heightmap.getFullMinHeight();

            // Update profile GROUNDLEVEL for this city
            profile.GROUNDLEVEL = y;

            // Btm, use this impl, because we check for entire columns above.
            if (!groundLevels.containsKey(chunkCoord)) {
                groundLevels.put(chunkCoord, y);
            }

            generateNear(random, x, z, chunkX, chunkY, chunkZ, info, heightmap);
        }
    }

    private HeightmapModel canSpawnInChunk(int chunkX, int chunkY, int chunkZ) {
        int spawnSize = EarthTerrainProcessor.spawnSize;

        // if (-spawnSize < chunkX && chunkX < spawnSize && -spawnSize < chunkZ && chunkZ < spawnSize)
        if (chunkX >= -spawnSize && chunkX <= spawnSize || chunkZ >= -spawnSize && chunkZ <= spawnSize)
            return null; // don't spawn nothing on 5x5 chunks on spawn

        if (!isCityChunk(chunkX, chunkZ))
            return null;

        HeightmapModel model = HeightmapModel.getModel(chunkX, chunkY, chunkZ);
        if(model == null)
            return null;

        if(!model.surface)
            return null;

        if(!(LostCitiesDebug.debug
                ? CubicHeightmap.hasValidSteepness_Debug(model.heightmap, chunkX, chunkY, chunkZ)
                : CubicHeightmap.hasValidSteepness(model.heightmap))) {
            if(LostCitiesDebug.debug) System.out.println("("+chunkX+", "+chunkY+", "+chunkZ+")");
            return null;
        }

        if (RoadGenerator.isRoad(chunkX, chunkY, chunkZ))
            return null;

        double spawnChance = 1.0; // RogueConfig.getDouble(RogueConfig.SPAWNCHANCE); // TODO
        Random rand = new Random(Objects.hash(chunkX, chunkZ, 31));

        if(rand.nextFloat() < spawnChance)
            return model;

        return null;
    }

    private boolean isCityChunk(int chunkX, int chunkZ) {
        // return perlin.getValue(chunkX, 0, chunkZ) >= 0.5;

        double d = interpolate(perlin, perlin.getValue(chunkX, 0, chunkZ));
        return d >= 0.5;
    }

    private static double interpolate(Perlin perlin, double d) {
        double max = perlin.getMaxValue();
        double min = 0; // TODO: Is 0?

        return (d - min) / (max - min);
    }

    private boolean generateNear(Random rand, int x, int z, int chunkX, int chunkY, int chunkZ, BuildingInfo info, ICommonHeightmap heightmap) {
            Coord location = getNearbyCoord(rand, x, z, 40, 100);

            if (!validLocation(location))
                return false;

            generate(chunkX, chunkY, chunkZ, info, heightmap);

            return true;
    }

    private void generate(int chunkX, int chunkY, int chunkZ, BuildingInfo info, ICommonHeightmap heightmap) {
        // driver.setPrimer(primer);
        // BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, this);

        // @todo this setup is not very clean
        CityStyle cityStyle = info.getCityStyle();

        // TODO ?? (see usages on original code)

        street = info.getCompiledPalette().get(cityStyle.getStreetBlock());
        streetBase = info.getCompiledPalette().get(cityStyle.getStreetBaseBlock());
        street2 = info.getCompiledPalette().get(cityStyle.getStreetVariantBlock());
        streetBorder = (16 - cityStyle.getStreetWidth()) / 2;

        doCityChunk(chunkX, chunkY, chunkZ, info, heightmap);

        Railway.RailChunkInfo railInfo = info.getRailInfo();
        if (railInfo.getType() != RailChunkType.NONE) {
            railsGenerator.generateRailways(info, railInfo);
        }
        railsGenerator.generateRailwayDungeons(info);

        if (profile.isSpace()) {
            railsGenerator.generateMonorails(info);
        }

        fixTorches(info);

        // We make a new random here because the primer for a normal chunk may have
        // been cached and we want to be able to do the same when returning from a cached
        // primer vs generating it here
        // TODO
        // provider.rand.setSeed(chunkX * 257017164707L + chunkZ * 101754694003L);

        //LostCityEvent.PreExplosionEvent event = new LostCityEvent.PreExplosionEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //if (!MinecraftForge.EVENT_BUS.post(event)) {

        if (info.getDamageArea().hasExplosions()) {
            debrisGenerator.breakBlocksForDamage(chunkX, chunkZ, info);
            debrisGenerator.fixAfterExplosionNew(info, worldObj.rand);
        }

        debrisGenerator.generateDebris(worldObj.rand, info);
        //}
    }

    // TODO
    /*
    *
        private void doNormalChunk(int chunkX, int chunkZ, BuildingInfo info) {
//        debugClearChunk(chunkX, chunkZ, primer);
        if (info.profile.isDefault()) {
            flattenChunkToCityBorder(chunkX, chunkZ);
        }

        LostCityEvent.PostGenOutsideChunkEvent postevent = new LostCityEvent.PostGenOutsideChunkEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        MinecraftForge.EVENT_BUS.post(postevent);

        generateBridges(info);
        generateHighways(chunkX, chunkZ, info);
    }
    *
    * */

    private void doCityChunk(int chunkX, int chunkY, int chunkZ, BuildingInfo info, ICommonHeightmap heightmap) {
        boolean building = info.hasBuilding;

        Random rand = new Random(worldObj.getSeed() * 377 + chunkZ * 341873128712L + chunkX * 132897987541L);
        rand.nextFloat();
        rand.nextFloat();

        driver.setLocalBlock(chunkX, chunkY, chunkZ);

        if (info.profile.isDefault()) {
            /* // TODO
            if (info.waterLevel > info.groundLevel) {
                // Special case for a high water level
                for (int x = 0; x < 16; ++x) {
                    for (int z = 0; z < 16; ++z) {
                        driver.setBlockRange(x, info.groundLevel, z, info.waterLevel, liquidChar);
                    }
                }
            }
             */
        }

        // TODO: Events
        //LostCityEvent.PreGenCityChunkEvent event = new LostCityEvent.PreGenCityChunkEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //if (!MinecraftForge.EVENT_BUS.post(event)) {
        if (building) {
            buildingGenerator.generate(info, heightmap);
        } else {
            CubicHeightmap _heightmap = (CubicHeightmap)heightmap;
            // info.groundLevel+" -- "+getSurfaceBlock(new CubePos(chunkX, chunkY, chunkZ))+
            // System.out.println("Min: "+_heightmap.getFullMinHeight()+"; Avg: "+_heightmap.getFullAverageHeight()+"; Max: "+_heightmap.getFullMaxHeight());
            // System.out.println(info.groundLevel+" -- "+findTopBlock(new CubePos(chunkX, chunkY, chunkZ))+"; Is Air?: "+cubicWorld.getCubeFromCubeCoords(new CubePos(chunkX, chunkY, chunkZ)).isEmpty());
            streetGenerator.generate(info, heightmap, rand);
        }
        //}
        //LostCityEvent.PostGenCityChunkEvent postevent = new LostCityEvent.PostGenCityChunkEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //MinecraftForge.EVENT_BUS.post(postevent);

        if (info.profile.RUINS) {
            // System.out.println("Generating ruins");
            ruinsGenerator.generate(info);
        }

        int levelX = info.getHighwayXLevel();
        int levelZ = info.getHighwayZLevel();
        if (!building) {
            Railway.RailChunkInfo railInfo = info.getRailInfo();
            if (levelX < 0 && levelZ < 0 && !railInfo.getType().isSurface()) {
                streetGenerator.generateStreetDecorations(info);
            }
        }
        if (levelX >= 0 || levelZ >= 0) {
            streetGenerator.generateHighways(chunkX, chunkZ, info);
        }

        if (info.profile.RUBBLELAYER) {
            if (!info.hasBuilding || info.ruinHeight >= 0) {
                rubbleGenerator.generateRubble(chunkX, chunkZ, info);
            }
        }
    }

    public boolean validLocation(Coord column) {
        Biome biome = worldObj.getBiome(column.getBlockPos());

        Type[] invalidBiomes = new Type[]{
                BiomeDictionary.Type.RIVER,
                BiomeDictionary.Type.BEACH,
                BiomeDictionary.Type.MUSHROOM,
                BiomeDictionary.Type.OCEAN
        };

        for (Type type : invalidBiomes) {
            if (BiomeDictionary.hasType(biome, type))
                return false;
        }

        return true;
    }

    public static Coord getNearbyCoord(Random rand, int x, int z, int min, int max) {

        int distance = min + rand.nextInt(max - min);

        double angle = rand.nextDouble() * 2 * Math.PI;

        int xOffset = (int) (Math.cos(angle) * distance);
        int zOffset = (int) (Math.sin(angle) * distance);

        return new Coord(x + xOffset, 0, z + zOffset);
    }

    // TODO
    @Override
    public ILostChunkInfo getChunkInfo(int chunkX, int chunkZ) {
        return BuildingInfo.getBuildingInfo(chunkX, chunkZ, this);
    }

    @Override
    public int getRealHeight(int level) {
        return profile.GROUNDLEVEL + level * 6;
    }

    @Override
    public ILostCityAssetRegistry<ILostCityBuilding> getBuildings() {
        return AssetRegistries.BUILDINGS.cast();
    }

    @Override
    public ILostCityAssetRegistry<ILostCityMultiBuilding> getMultiBuildings() {
        return AssetRegistries.MULTI_BUILDINGS.cast();
    }

    @Override
    public ILostCityAssetRegistry<ILostCityCityStyle> getCityStyles() {
        return AssetRegistries.CITYSTYLES.cast();
    }

    @Override
    public int getDimensionId() {
        return dimensionId;
    }

    @Override
    public WorldStyle getWorldStyle() {
        return worldStyle;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    public Random getRandom() {
        return random;
    }

    @Override
    public LostCityProfile getProfile() {
        return profile;
    }

    @Override
    public LostCityProfile getOutsideProfile() {
        return profile;
    }

    @Override
    public World getWorld() {
        return worldObj;
    }

    @Override
    public boolean hasMansion(int chunkX, int chunkZ) {
        return false; // TODO
    }

    @Override
    public boolean hasOceanMonument(int chunkX, int chunkZ) {
        return false; // TODO
    }

    @Override
    public ICommonHeightmap getHeightmap(int chunkX, int chunkZ) {
        CubePos key = new CubePos(chunkX, currentChunkY, chunkZ);

        if (cachedHeightmaps.containsKey(key))
            return cachedHeightmaps.get(key);

        char baseChar = (char) Block.BLOCK_STATE_IDS.get(profile.getBaseBlock());

        CubicHeightmap heightmap = new CubicHeightmap(driver, profile.LANDSCAPE_TYPE, profile.GROUNDLEVEL, baseChar);
        heightmap.setLocalChunk(chunkX, currentChunkY, chunkZ);
        heightmap.setModel(currentModel);

        cachedHeightmaps.put(key, heightmap);
        return heightmap;
    }
}
