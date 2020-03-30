package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import mcjty.lostcities.api.*;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.cubic.world.generators.*;
import mcjty.lostcities.dimensions.world.WorldTypeTools;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Railway;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import mcjty.lostcities.dimensions.world.lost.cityassets.CityStyle;
import mcjty.lostcities.dimensions.world.lost.cityassets.WorldStyle;
import mcjty.lostcities.varia.Cardinal;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.varia.Coord;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import org.spongepowered.noise.module.source.Perlin;

import java.util.*;

import static mcjty.lostcities.cubic.CubicCityWorldProcessor.*;

public class LostCityCubicGenerator implements ICommonGeneratorProvider
{
    public static LostCityProfile profile;

    public static char liquidChar;
    public static char baseChar;
    public static char airChar;
    public static char hardAirChar;
    public static char glowstoneChar;
    public static char gravelChar;
    public static char glassChar;       // @todo: for space: depend on city style
    public static char leavesChar;
    public static char leaves2Char;
    public static char leaves3Char;
    public static char ironbarsChar;
    public static char grassChar;
    public static char bedrockChar;
    public static char endportalChar;
    public static char endportalFrameChar;
    public static char goldBlockChar;
    public static char diamondBlockChar;

    public static char street;
    public static char street2;
    public static char streetBase;
    public static int streetBorder;

    // Flags

    // public static boolean isSpawnedOnce;
    // private static boolean isGenerating;

    // Generators

    public static BuildingGenerator buildingGenerator;
    public static PartGenerator partGenerator;
    public static RailsGenerator railsGenerator;
    public static StreetGenerator streetGenerator;
    public static RubbleGenerator rubbleGenerator;

    // Needed fields
    private static int dimensionId;
    private static long seed;

    private static WorldStyle worldStyle;
    private static Map<CubePos, CubicHeightmap> cachedHeightmaps = new HashMap<>();
    private static Map<ChunkCoord, Integer> groundLevels = new HashMap<>();
    private static HashSet<ChunkCoord> roadChunks = new HashSet<>();

    private static Random random;

    private static Perlin perlin;

    // Singleton
    public static LostCityCubicGenerator provider;

    private int currentChunkY;

    public LostCityCubicGenerator() {
        // TODO: Refactor this
        if(provider == null) {
            provider = this;

            random = worldObj.rand;

            profile = WorldTypeTools.getProfile(worldObj);

            liquidChar = (char) Block.BLOCK_STATE_IDS.get(profile.getLiquidBlock());
            baseChar = (char) Block.BLOCK_STATE_IDS.get(profile.getBaseBlock());
            airChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.AIR.getDefaultState());
            hardAirChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.COMMAND_BLOCK.getDefaultState());

            glowstoneChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.GLOWSTONE.getDefaultState());
            gravelChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.GRAVEL.getDefaultState());

            // @todo
            glassChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.GLASS.getDefaultState());

            leavesChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.LEAVES.getDefaultState()
                    .withProperty(BlockLeaves.DECAYABLE, false));
            leaves2Char = (char) Block.BLOCK_STATE_IDS.get(Blocks.LEAVES.getDefaultState()
                    .withProperty(BlockLeaves.DECAYABLE, false)
                    .withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE));
            leaves3Char = (char) Block.BLOCK_STATE_IDS.get(Blocks.LEAVES.getDefaultState()
                    .withProperty(BlockLeaves.DECAYABLE, false)
                    .withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.SPRUCE));

            ironbarsChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.IRON_BARS.getDefaultState());
            grassChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.GRASS.getDefaultState());
            bedrockChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.BEDROCK.getDefaultState());
            endportalChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.END_PORTAL.getDefaultState());
            endportalFrameChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.END_PORTAL_FRAME.getDefaultState());
            goldBlockChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.GOLD_BLOCK.getDefaultState());
            diamondBlockChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.DIAMOND_BLOCK.getDefaultState());

            dimensionId = worldObj.provider.getDimension();
            seed = worldObj.provider.getSeed();

            worldStyle = AssetRegistries.WORLDSTYLES.get(profile.getWorldStyle());
            if (worldStyle == null) {
                throw new RuntimeException("Unknown worldstyle '" + profile.getWorldStyle() + "'!");
            }

            perlin = new Perlin();
            perlin.setSeed((int)seed);
            perlin.setOctaveCount(5);
            perlin.setFrequency(0.1);
            perlin.setPersistence(0.8);
            perlin.setLacunarity(1.25);

            // TODO: not used, review on refactor
            partGenerator = new PartGenerator();
        }
    }

    public void spawnInChunk(int chunkX, int chunkY, int chunkZ) {
        currentChunkY = chunkY;

        // We need this in order to generate once per column
        ChunkCoord chunkCoord = new ChunkCoord(dimensionId, chunkX, chunkZ);

        if(canSpawnInChunk(chunkX, chunkY, chunkZ) && !groundLevels.containsKey(chunkCoord))
        {
            // TODO: This will be wrong
            int x = chunkX * 16 + 8;
            int y = chunkY * 16 + 8;
            int z = chunkZ * 16 + 8;

            generateNear(random, x, y, z, chunkX, chunkY, chunkZ);

            // isGenerating =
            // isSpawnedOnce = isGenerating;
        }
    }

    public static boolean canSpawnInChunk(int chunkX, int chunkY, int chunkZ)
    {
        if(chunkX >= -20 && chunkX <= 20 || chunkZ >= -20 && chunkZ <= 20) return false; // don't spawn nothing on 20x20 chunks on spawn
        if(!isCityChunk(chunkX, chunkZ)) return false;

        // Add road chunk to hashset, so we will not generate any building at this column
        ChunkCoord chunkCoord = new ChunkCoord(dimensionId, chunkX, chunkZ);
        if(roadChunks.contains(chunkCoord) || isRoadChunk(chunkX, chunkY, chunkZ)) {
            roadChunks.add(chunkCoord);
            return false;
        }

        double spawnChance = 1.0; // RogueConfig.getDouble(RogueConfig.SPAWNCHANCE); // TODO
        Random rand = new Random(Objects.hash(chunkX, chunkZ, 31));

        return rand.nextFloat() < spawnChance;
    }

    private static boolean isCityChunk(int chunkX, int chunkZ) {
        // return perlin.getValue(chunkX, 0, chunkZ) >= 0.5;

        double d = interpolate(perlin, perlin.getValue(chunkX, 0, chunkZ));
        return d >= 0.5;
    }

    private static boolean isRoadChunk(int chunkX, int chunkY, int chunkZ) {
        // Blocks.CONCRETE
        for (int x = chunkX; x < chunkX + 16; ++x) {
            for (int y = chunkY; y < chunkY + 16; ++y) {
                if(driver.getBlockState(x, y, chunkZ + 8) == Blocks.CONCRETE)
                    return true;
            }
        }

        for (int z = chunkZ; z < chunkZ + 16; ++z) {
            for (int y = chunkY; y < chunkY + 16; ++y) {
                if(driver.getBlockState(chunkX + 8, y, z) == Blocks.CONCRETE)
                    return true;
            }
        }

        return false;
    }

    private static double interpolate(Perlin perlin, double d) {
        double max = perlin.getMaxValue();
        double min = 0; // TODO: Is 0?

        return (d - min) / (max - min);
    }

    private boolean generateNear(Random rand, int x, int y, int z, int chunkX, int chunkY, int chunkZ){
        int attempts = 50;

        for(int i = 0; i < attempts; i++){
            Coord location = getNearbyCoord(rand, x, z, 40, 100);
            // if(isCubicWorld) // This is always true
                location.add(Cardinal.UP, y);

            if(!validLocation(rand, location))
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
            if(!groundLevels.containsKey(chunkCoord)) {
                groundLevels.put(chunkCoord, y);
            }

            BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, this);

            generate(chunkX, chunkY, chunkZ, info);

            return true;
        }

        // if(LostCitiesDebug.debug) System.out.println("Surpassed maximum ("+attempts+") attempts!");
        return false;
    }

    /*
    *
                // Create generator instances
            partGenerator = new PartGenerator();
            buildingGenerator = new BuildingGenerator();
            railsGenerator = new RailsGenerator();
            streetGenerator = new StreetGenerator();
            rubbleGenerator = new RubbleGenerator();
    * */

    private void setCube(int chunkX, int chunkY, int chunkZ) {
        CubePos key = new CubePos(chunkX, chunkY, chunkZ);
        if(!cachedCubes.containsKey(key)) throw new IllegalStateException();
        driver.setCube(cachedCubes.get(key));
    }

    private BuildingGenerator getBuildingGenerator(int chunkX, int chunkY, int chunkZ) {
        setCube(chunkX, chunkY, chunkZ);
        if(buildingGenerator == null) buildingGenerator = new BuildingGenerator();
        return buildingGenerator;
    }

    private RailsGenerator getRailsGenerator(int chunkX, int chunkY, int chunkZ) {
        setCube(chunkX, chunkY, chunkZ);
        if(railsGenerator == null) railsGenerator = new RailsGenerator();
        return railsGenerator;
    }

    private StreetGenerator getStreetGenerator(int chunkX, int chunkY, int chunkZ) {
        setCube(chunkX, chunkY, chunkZ);
        if(streetGenerator == null) streetGenerator = new StreetGenerator();
        return streetGenerator;
    }

    private RubbleGenerator getRubbleGenerator(int chunkX, int chunkY, int chunkZ) {
        setCube(chunkX, chunkY, chunkZ);
        if(rubbleGenerator == null) rubbleGenerator = new RubbleGenerator();
        return rubbleGenerator;
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

        RailsGenerator railGen = getRailsGenerator(chunkX, chunkY, chunkZ);

        Railway.RailChunkInfo railInfo = info.getRailInfo();
        if (railInfo.getType() != RailChunkType.NONE) {
            // System.out.println("Generating railways");
            railGen.generateRailways(info, railInfo);
        }
        railGen.generateRailwayDungeons(info);

        if (profile.isSpace()) {
            // System.out.println("Generating monorails");
            railGen.generateMonorails(info);
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

        BuildingGenerator buildingGen = getBuildingGenerator(chunkX, chunkY, chunkZ);
        StreetGenerator streetGen = getStreetGenerator(chunkX, chunkY, chunkZ);
        RubbleGenerator rubbleGen = getRubbleGenerator(chunkX, chunkY, chunkZ);

        // TODO: Events
        //LostCityEvent.PreGenCityChunkEvent event = new LostCityEvent.PreGenCityChunkEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //if (!MinecraftForge.EVENT_BUS.post(event)) {
            if (building) {
                // System.out.println("Generating building at ["+(chunkX*16)+", "+(info.profile.GROUNDLEVEL/16)+", "+(chunkZ*16)+"]");
                buildingGen.generate(info, heightmap);
            } else {
                // System.out.println("Generating street");
                streetGen.generateStreet(info, heightmap, rand); // TODO
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
                streetGen.generateStreetDecorations(info); // TODO
            }
        }
        if (levelX >= 0 || levelZ >= 0) {
            // System.out.println("Generating highways");
            streetGen.generateHighways(chunkX, chunkZ, info); // TODO
        }

        if (info.profile.RUBBLELAYER) {
            if (!info.hasBuilding || info.ruinHeight >= 0) {
                // System.out.println("Generating rubble");
                rubbleGen.generateRubble(chunkX, chunkZ, info); // TODO
            }
        }
    }

    public boolean validLocation(Random rand, Coord column){
        Biome biome = worldObj.getBiome(column.getBlockPos());
                // editor.getInfo(column).getBiome();

        Type[] invalidBiomes = new Type[]{
                BiomeDictionary.Type.RIVER,
                BiomeDictionary.Type.BEACH,
                BiomeDictionary.Type.MUSHROOM,
                BiomeDictionary.Type.OCEAN
        };

        for(Type type : invalidBiomes){
            if(BiomeDictionary.hasType(biome, type)) return false;
        }

        int y = column.getY();

        // Check at least two chunks (so if we have water under it we won't spawn anything)
        int upperLimit = y + 16; //isCubicWorld ? y + 16 : RogueConfig.getInt(RogueConfig.UPPERLIMIT);
        int lowerLimit = y - 16; //isCubicWorld ? y : RogueConfig.getInt(RogueConfig.LOWERLIMIT);

        Coord cursor = new Coord(column.getX(), upperLimit, column.getZ());

        if(!isAirBlock(cursor)){
            return false;
        }

        while(!validGroundBlock(cursor)){
            cursor.add(Cardinal.DOWN);
            if(cursor.getY() < lowerLimit) return false;
            if(worldObj.getBlockState(cursor.getBlockPos()).getMaterial() == Material.WATER) return false;
        }

        return true;
    }

    public static Coord getNearbyCoord(Random rand, int x, int z, int min, int max){

        int distance = min + rand.nextInt(max - min);

        double angle = rand.nextDouble() * 2 * Math.PI;

        int xOffset = (int) (Math.cos(angle) * distance);
        int zOffset = (int) (Math.sin(angle) * distance);

        Coord nearby = new Coord(x + xOffset, 0, z + zOffset);
        return nearby;
    }

    public boolean isAirBlock(Coord pos){
        return worldObj.isAirBlock(pos.getBlockPos());
    }

    public boolean validGroundBlock(Coord pos){
        if(isAirBlock(pos)) return false;
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
    };

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
