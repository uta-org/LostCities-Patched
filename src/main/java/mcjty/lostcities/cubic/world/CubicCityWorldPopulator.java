package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.terra121.populator.RoadGenerator;
import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.api.*;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Railway;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import mcjty.lostcities.dimensions.world.lost.cityassets.CityStyle;
import mcjty.lostcities.dimensions.world.lost.cityassets.WorldStyle;
import mcjty.lostcities.varia.Cardinal;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.varia.Coord;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import org.spongepowered.noise.module.source.Perlin;

import java.util.*;

import static mcjty.lostcities.cubic.world.CubeCityUtils.*;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.*;

public class CubicCityWorldPopulator implements ICommonGeneratorProvider, ICubicPopulator, Comparable<Object>
{

    private static Map<CubePos, CubicHeightmap> cachedHeightmaps = new HashMap<>();
    private static Map<ChunkCoord, Integer> groundLevels = new HashMap<>();

    // Needed fields
    private Random random;
    private int dimensionId;
    private long seed;

    // Singleton
    public static CubicCityWorldPopulator provider;

    private int currentChunkY;

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

        if (canSpawnInChunk(chunkX, chunkY, chunkZ) && !groundLevels.containsKey(chunkCoord)) {
            // TODO: This will be wrong
            int x = chunkX * 16 + 8;
            int y = chunkY * 16 + 8;
            int z = chunkZ * 16 + 8;

            generateNear(random, x, y, z, chunkX, chunkY, chunkZ);

            // isGenerating =
            // isSpawnedOnce = isGenerating;
        }
    }

    private boolean canSpawnInChunk(int chunkX, int chunkY, int chunkZ) {
        if (chunkX >= -20 && chunkX <= 20 || chunkZ >= -20 && chunkZ <= 20)
            return false; // don't spawn nothing on 20x20 chunks on spawn
        if (!isCityChunk(chunkX, chunkZ)) return false;

        // Add road chunk to hashset, so we will not generate any building at this column
        // ChunkCoord chunkCoord = new ChunkCoord(dimensionId, chunkX, chunkZ);
        // CubePos cubePos = new CubePos(chunkX, chunkY, chunkZ);
        if (RoadGenerator.isRoad(chunkX, chunkY, chunkZ)) {
            if(LostCitiesDebug.debug) System.out.println("["+chunkX+", "+chunkZ+"] Detected road chunk!");

            // roadChunks.add(chunkCoord);
            return false;
        }

        double spawnChance = 1.0; // RogueConfig.getDouble(RogueConfig.SPAWNCHANCE); // TODO
        Random rand = new Random(Objects.hash(chunkX, chunkZ, 31));

        return rand.nextFloat() < spawnChance;
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

    private boolean generateNear(Random rand, int x, int y, int z, int chunkX, int chunkY, int chunkZ) {
        int attempts = 50;

        for (int i = 0; i < attempts; i++) {
            Coord location = getNearbyCoord(rand, x, z, 40, 100);
            // if(isCubicWorld) // This is always true
            location.add(Cardinal.UP, y);

            if (!validLocation(rand, location))
                continue;

            // if(LostCitiesDebug.debug) System.out.println("["+chunkX+", "+chunkZ+"] Generating a part of the city on this chunk!");

            // Update profile GROUNDLEVEL for this city
            ChunkCoord chunkCoord = new ChunkCoord(dimensionId, chunkX, chunkZ);

            /*
            if(!groundLevels.containsKey(chunkCoord)) {
                groundLevels.put(chunkCoord, y);
                profile.GROUNDLEVEL = y;
            }
            else {
                int groundlevel = groundLevels.get(chunkCoord);

                int groundDiff = y - groundlevel;
                if(!(groundDiff >= -2 && groundDiff <= 3)) {
                    return false; // We are outside of the level bounds (-3 * 6 -- 6 * 6)
                }

                profile.GROUNDLEVEL = groundlevel;
            }
             */

            profile.GROUNDLEVEL = y;

            // Btm, use this impl, because we check for entire columns above.
            if (!groundLevels.containsKey(chunkCoord)) {
                groundLevels.put(chunkCoord, y);
            }

            BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, this);

            generate(chunkX, chunkY, chunkZ, info);

            return true;
        }

        // if(LostCitiesDebug.debug) System.out.println("Surpassed maximum ("+attempts+") attempts!");
        return false;
    }

    public void generate(int chunkX, int chunkY, int chunkZ, BuildingInfo info) {
        // driver.setPrimer(primer);
        // BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, this);

        // @todo this setup is not very clean
        CityStyle cityStyle = info.getCityStyle();

        // TODO ?? (see usages on original code)

        street = info.getCompiledPalette().get(cityStyle.getStreetBlock());
        streetBase = info.getCompiledPalette().get(cityStyle.getStreetBaseBlock());
        street2 = info.getCompiledPalette().get(cityStyle.getStreetVariantBlock());
        streetBorder = (16 - cityStyle.getStreetWidth()) / 2;

        doCityChunk(chunkX, chunkY, chunkZ, info);

        Railway.RailChunkInfo railInfo = info.getRailInfo();
        if (railInfo.getType() != RailChunkType.NONE) {
            // System.out.println("Generating railways");
            railsGenerator.generateRailways(info, railInfo);
        }
        railsGenerator.generateRailwayDungeons(info);

        if (profile.isSpace()) {
            // System.out.println("Generating monorails");
            railsGenerator.generateMonorails(info);
        }

        // fixTorches(info); // TODO

        // We make a new random here because the primer for a normal chunk may have
        // been cached and we want to be able to do the same when returning from a cached
        // primer vs generating it here
        // TODO
        // provider.rand.setSeed(chunkX * 257017164707L + chunkZ * 101754694003L);

        //LostCityEvent.PreExplosionEvent event = new LostCityEvent.PreExplosionEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //if (!MinecraftForge.EVENT_BUS.post(event)) {
        // TODO
        if (info.getDamageArea().hasExplosions()) {
            //breakBlocksForDamage(chunkX, chunkZ, info);
            //fixAfterExplosionNew(info, provider.rand);
        }
        //generateDebris(provider.rand, info);
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

    private void doCityChunk(int chunkX, int chunkY, int chunkZ, BuildingInfo info) {
        boolean building = info.hasBuilding;

        // TODO: Create custom heightmap for Cubic Worlds
        ICommonHeightmap heightmap = getHeightmap(info.chunkX, info.chunkZ);

        Random rand = new Random(worldObj.getSeed() * 377 + chunkZ * 341873128712L + chunkX * 132897987541L);
        rand.nextFloat();
        rand.nextFloat();

        driver.setLocalChunk(chunkX, chunkZ);

        if (info.profile.isDefault()) {
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    driver.setBlockRange(x, 0, z, info.profile.BEDROCK_LAYER, bedrockChar);
                }
            }

            if (info.waterLevel > info.groundLevel) {
                // Special case for a high water level
                for (int x = 0; x < 16; ++x) {
                    for (int z = 0; z < 16; ++z) {
                        driver.setBlockRange(x, info.groundLevel, z, info.waterLevel, liquidChar);
                    }
                }
            }
        }

        // TODO: Events
        //LostCityEvent.PreGenCityChunkEvent event = new LostCityEvent.PreGenCityChunkEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //if (!MinecraftForge.EVENT_BUS.post(event)) {
        if (building) {
            // System.out.println("Generating building at ["+(chunkX*16)+", "+(info.profile.GROUNDLEVEL/16)+", "+(chunkZ*16)+"]");
            buildingGenerator.generate(info, heightmap);
        } else {
            // System.out.println("Generating street");
            streetGenerator.generateStreet(info, heightmap, rand); // TODO
        }
        //}
        //LostCityEvent.PostGenCityChunkEvent postevent = new LostCityEvent.PostGenCityChunkEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //MinecraftForge.EVENT_BUS.post(postevent);

        if (info.profile.RUINS) {
            // System.out.println("Generating ruins");
            // generateRuins(info); // TODO
        }

        int levelX = info.getHighwayXLevel();
        int levelZ = info.getHighwayZLevel();
        if (!building) {
            Railway.RailChunkInfo railInfo = info.getRailInfo();
            if (levelX < 0 && levelZ < 0 && !railInfo.getType().isSurface()) {
                // System.out.println("Generating street decorations");
                streetGenerator.generateStreetDecorations(info); // TODO
            }
        }
        if (levelX >= 0 || levelZ >= 0) {
            // System.out.println("Generating highways");
            streetGenerator.generateHighways(chunkX, chunkZ, info); // TODO
        }

        if (info.profile.RUBBLELAYER) {
            if (!info.hasBuilding || info.ruinHeight >= 0) {
                // System.out.println("Generating rubble");
                rubbleGenerator.generateRubble(chunkX, chunkZ, info); // TODO
            }
        }
    }

    public boolean validLocation(Random rand, Coord column) {
        Biome biome = worldObj.getBiome(column.getBlockPos());
        // editor.getInfo(column).getBiome();

        Type[] invalidBiomes = new Type[]{
                BiomeDictionary.Type.RIVER,
                BiomeDictionary.Type.BEACH,
                BiomeDictionary.Type.MUSHROOM,
                BiomeDictionary.Type.OCEAN
        };

        for (Type type : invalidBiomes) {
            if (BiomeDictionary.hasType(biome, type)) return false;
        }

        int y = column.getY();

        // Check at least two chunks (so if we have water under it we won't spawn anything)
        int upperLimit = y + 16; //isCubicWorld ? y + 16 : RogueConfig.getInt(RogueConfig.UPPERLIMIT);
        int lowerLimit = y - 16; //isCubicWorld ? y : RogueConfig.getInt(RogueConfig.LOWERLIMIT);

        Coord cursor = new Coord(column.getX(), upperLimit, column.getZ());

        if (!isAirBlock(cursor)) {
            return false;
        }

        while (!validGroundBlock(cursor)) {
            cursor.add(Cardinal.DOWN);
            if (cursor.getY() < lowerLimit) return false;
            if (worldObj.getBlockState(cursor.getBlockPos()).getMaterial() == Material.WATER) return false;
        }

        return true;
    }

    public static Coord getNearbyCoord(Random rand, int x, int z, int min, int max) {

        int distance = min + rand.nextInt(max - min);

        double angle = rand.nextDouble() * 2 * Math.PI;

        int xOffset = (int) (Math.cos(angle) * distance);
        int zOffset = (int) (Math.sin(angle) * distance);

        Coord nearby = new Coord(x + xOffset, 0, z + zOffset);
        return nearby;
    }

    public boolean isAirBlock(Coord pos) {
        return worldObj.isAirBlock(pos.getBlockPos());
    }

    public boolean validGroundBlock(Coord pos) {
        if (isAirBlock(pos)) return false;
        IBlockState block = worldObj.getBlockState(pos.getBlockPos());
        return !invalid.contains(block.getMaterial());
    }

    private static List<Material> invalid;

    {
        invalid = new ArrayList<Material>();
        invalid.add(Material.WOOD);
        invalid.add(Material.WATER);
        invalid.add(Material.CACTUS);
        invalid.add(Material.SNOW);
        invalid.add(Material.GRASS);
        invalid.add(Material.GOURD);
        invalid.add(Material.LEAVES);
        invalid.add(Material.PLANTS);
    }

    ;

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

        if (cachedHeightmaps.containsKey(key)) {
            return cachedHeightmaps.get(key);
        }

        if (cachedPrimers.containsKey(key)) {
            char baseChar = (char) Block.BLOCK_STATE_IDS.get(profile.getBaseBlock());
            CubePrimer primer = cachedPrimers.get(key);
            driver.setPrimer(primer);
            CubicHeightmap heightmap = new CubicHeightmap(driver, profile.LANDSCAPE_TYPE, profile.GROUNDLEVEL, baseChar);
            heightmap.setChunkY(currentChunkY);
            cachedHeightmaps.put(key, heightmap);
            return heightmap;
        }

        throw new IllegalStateException();
    }
}
