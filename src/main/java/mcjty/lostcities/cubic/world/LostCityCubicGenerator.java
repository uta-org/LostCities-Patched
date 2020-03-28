package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import javafx.beans.binding.MapExpression;
import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.api.*;
import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.cubic.world.driver.CubeDriver;
import mcjty.lostcities.cubic.world.driver.ICubeDriver;
import mcjty.lostcities.cubic.world.generators.BuildingGenerator;
import mcjty.lostcities.dimensions.world.ChunkHeightmap;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.WorldTypeTools;
import mcjty.lostcities.dimensions.world.driver.IPrimerDriver;
import mcjty.lostcities.dimensions.world.driver.OptimizedDriver;
import mcjty.lostcities.dimensions.world.driver.SafeDriver;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Railway;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import mcjty.lostcities.dimensions.world.lost.cityassets.WorldStyle;
import mcjty.lostcities.varia.Cardinal;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.varia.Coord;
import mcjty.lostcities.varia.VanillaStructure;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

import net.minecraft.block.material.Material;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import javax.annotation.Nonnull;

import static mcjty.lostcities.dimensions.world.terraingen.LostCitiesTerrainGenerator.bedrockChar;

public class LostCityCubicGenerator implements ICommonGeneratorProvider {
    // private static LostCityChunkGenerator provider;

    @Nonnull
    private static CubeDriver driver;

    private static ICubicWorld world;
    private static LostCityProfile profile;

    public static char liquidChar;
    public static char baseChar;
    public static char airChar;
    public static char hardAirChar;

    // Flags

    private static boolean isSpawnedOnce,
                           isGenerating;

    // Generators

    private static BuildingGenerator buildingGenerator;

    // Needed fields
    private static int dimensionId;
    private static long seed;

    private static World worldObj;

    private static WorldStyle worldStyle;
    private Map<ChunkCoord, CubePrimer> cachedPrimers = new HashMap<>();
    private Map<ChunkCoord, ICommonHeightmap> cachedHeightmaps = new HashMap<>();

    private static Random random;

    private LostCityCubicGenerator() {}

    public LostCityCubicGenerator(World _world, Random _random) {
        if(world != null) return;

        driver = new CubeDriver();
        world = (ICubicWorld) _world;
        worldObj = _world;

        random = _random;

        // provider = world.provider;

        /*if(generator != null) return;*/ // TODO: Btm, I'll use this implementation, but we need to do something similar to WorldProvider.createChunkGenerator

        //provider = new LostWorldProvider();
        //provider.setWorld(world);

        // provider = new LostCityChunkGenerator(world, (world.getSeed() >> 3) ^ 34328884229L);
                // (LostCityChunkGenerator) worldProvider.createChunkGenerator();

        profile = WorldTypeTools.getProfile(_world);

        liquidChar = (char) Block.BLOCK_STATE_IDS.get(profile.getLiquidBlock());
        baseChar = (char) Block.BLOCK_STATE_IDS.get(profile.getBaseBlock());
        airChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.AIR.getDefaultState());
        hardAirChar = (char) Block.BLOCK_STATE_IDS.get(Blocks.COMMAND_BLOCK.getDefaultState());

        // Create generator instances
        buildingGenerator = new BuildingGenerator(driver);

        dimensionId = _world.provider.getDimension();
        seed = _world.provider.getSeed();

        worldStyle = AssetRegistries.WORLDSTYLES.get(profile.getWorldStyle());
        if (worldStyle == null) {
            throw new RuntimeException("Unknown worldstyle '" + profile.getWorldStyle() + "'!");
        }
    }

    // world, random, chunkX, 0, chunkZ
    public void spawnInChunk(World world, Random random, int chunkX, int chunkY, int chunkZ) {
        // TODO: Find suitable chunks

        boolean isDebug = LostCitiesDebug.debug;

        // System.out.println("("+chunkX+", "+chunkY+", "+chunkZ+")");

        // flag created to test
        boolean canSpawnInDebugMode = isDebug && chunkY >= 25;
        if(canSpawnInChunk(world, chunkX, chunkZ) && canSpawnInDebugMode){
            int x = chunkX * 16 + 4;
            int z = chunkZ * 16 + 4;

            int y = chunkY * 16;

            if(isGenerating)
                return;

            if(isDebug) {
                System.out.println("Attempting to generate city at chunk ("+x+", "+z+"), y = "+y);
            }

            isGenerating = generateNear(world, random, x, y, z, chunkX, chunkZ);
            isSpawnedOnce = isGenerating;
        }
    }

    public static boolean canSpawnInChunk(World world, int chunkX, int chunkZ){

        // if(!RogueConfig.getBoolean(RogueConfig.DONATURALSPAWN)) return false;

        // TODO
        /*
        int dim = editor.getInfo(new Coord(chunkX * 16, 0, chunkZ * 16)).getDimension();
        List<Integer> wl = new ArrayList<Integer>();
        wl.addAll(RogueConfig.getIntList(RogueConfig.DIMENSIONWL));
        List<Integer> bl = new ArrayList<Integer>();
        bl.addAll(RogueConfig.getIntList(RogueConfig.DIMENSIONBL));
        if(!SpawnCriteria.isValidDimension(dim, wl, bl)) return false;
        */

        boolean spawn = !isSpawnedOnce && LostCitiesDebug.debug;
        // if(!isVillageChunk(world, chunkX, chunkZ) && !spawn) return false;

        double spawnChance = 1.0; // RogueConfig.getDouble(RogueConfig.SPAWNCHANCE); // * 0.05;
        Random rand = new Random(Objects.hash(chunkX, chunkZ, 31));

        float f = rand.nextFloat();

        return rand.nextFloat() < spawnChance;
    }

    public static boolean isVillageChunk(World world, int chunkX, int chunkZ){
        int frequency = 10; // RogueConfig.getInt(RogueConfig.SPAWNFREQUENCY);
        int min = 8 * frequency / 10;
        int max = 32 * frequency / 10;

        min = min < 2 ? 2 : min;
        max = max < 8 ? 8 : max;

        int tempX = chunkX < 0 ? chunkX - (max - 1) : chunkX;
        int tempZ = chunkZ < 0 ? chunkZ - (max - 1) : chunkZ;

        int m = tempX / max;
        int n = tempZ / max;

        Random r =  world.setRandomSeed(m, n, 10387312);
                // editor.getSeededRandom(m, n, 10387312);

        m *= max;
        n *= max;

        m += r.nextInt(max - min);
        n += r.nextInt(max - min);

        return chunkX == m && chunkZ == n;
    }

    public boolean generateNear(World world, Random rand, int x, int y, int z, int chunkX, int chunkZ){
        int attempts = 50;

        for(int i = 0; i < attempts; i++){
            Coord location = getNearbyCoord(rand, x, z, 40, 100);
            // if(isCubicWorld) // This is always true
                location.add(Cardinal.UP, y);

            if(!validLocation(world, rand, location))
                continue;

            if(LostCitiesDebug.debug) System.out.println("["+chunkX+", "+chunkZ+"] Generating city chunk!");
            // generator.generateChunk(chunkX, chunkZ, true);

            // Update profile GROUNDLEVEL for this city
            profile.GROUNDLEVEL = y;

            // TODO: new CubePrimer()?
            generate(chunkX, chunkZ, new CubePrimer()); // .getCubeFromCubeCoords(x, y, z)

            return true;
        }

        if(LostCitiesDebug.debug) System.out.println("Surpassed maximum ("+attempts+") attempts!");
        return false;
    }


    public void generate(int chunkX, int chunkZ, CubePrimer primer) {
        driver.setPrimer(primer);
        BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, this);

        // @todo this setup is not very clean
        // CityStyle cityStyle = info.getCityStyle();

        // TODO ?? (see usages on original code)
        /*
        char street = info.getCompiledPalette().get(cityStyle.getStreetBlock());
        char streetBase = info.getCompiledPalette().get(cityStyle.getStreetBaseBlock());
        char street2 = info.getCompiledPalette().get(cityStyle.getStreetVariantBlock());
        int streetBorder = (16 - cityStyle.getStreetWidth()) / 2;
        */

        doCityChunk(chunkX, chunkZ, info);

        // TODO
        /*
        Railway.RailChunkInfo railInfo = info.getRailInfo();
        if (railInfo.getType() != RailChunkType.NONE) {
            generateRailways(info, railInfo);
        }
        generateRailwayDungeons(info);

        if (provider.getProfile().isSpace()) {
            generateMonorails(info);
        }

        fixTorches(info);

        // We make a new random here because the primer for a normal chunk may have
        // been cached and we want to be able to do the same when returning from a cached
        // primer vs generating it here
        provider.rand.setSeed(chunkX * 257017164707L + chunkZ * 101754694003L);

        LostCityEvent.PreExplosionEvent event = new LostCityEvent.PreExplosionEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            if (info.getDamageArea().hasExplosions()) {
                breakBlocksForDamage(chunkX, chunkZ, info);
                fixAfterExplosionNew(info, provider.rand);
            }
            generateDebris(provider.rand, info);
        }
        */
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

    private void doCityChunk(int chunkX, int chunkZ, BuildingInfo info) {
        boolean building = info.hasBuilding;

        // TODO: Create custom heightmap for Cubic Worlds
        ICommonHeightmap heightmap = getHeightmap(info.chunkX, info.chunkZ);

        Random rand = new Random(((World)world).getSeed() * 377 + chunkZ * 341873128712L + chunkX * 132897987541L);
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

        // TODO: Events
        //LostCityEvent.PreGenCityChunkEvent event = new LostCityEvent.PreGenCityChunkEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //if (!MinecraftForge.EVENT_BUS.post(event)) {
            if (building) {
                buildingGenerator.generate(info, (CubicHeightmap) heightmap);
            } else {
                // generateStreet(info, heightmap, rand); // TODO
            }
        //}
        //LostCityEvent.PostGenCityChunkEvent postevent = new LostCityEvent.PostGenCityChunkEvent(provider.worldObj, provider, chunkX, chunkZ, driver.getPrimer());
        //MinecraftForge.EVENT_BUS.post(postevent);

        if (info.profile.RUINS) {
            // generateRuins(info); // TODO
        }

        int levelX = info.getHighwayXLevel();
        int levelZ = info.getHighwayZLevel();
        if (!building) {
            Railway.RailChunkInfo railInfo = info.getRailInfo();
            if (levelX < 0 && levelZ < 0 && !railInfo.getType().isSurface()) {
                // generateStreetDecorations(info); // TODO
            }
        }
        if (levelX >= 0 || levelZ >= 0) {
            // generateHighways(chunkX, chunkZ, info); // TODO
        }

        if (info.profile.RUBBLELAYER) {
            if (!info.hasBuilding || info.ruinHeight >= 0) {
                // generateRubble(chunkX, chunkZ, info); // TODO
            }
        }
    }




    // TODO
    /*
    private ChunkHeightmap getHeightmap(int chunkX, int chunkZ) {
        ChunkCoord key = new ChunkCoord(worldObj.provider.getDimension(), chunkX, chunkZ);
        if (cachedHeightmaps.containsKey(key)) {
            return cachedHeightmaps.get(key);
        } else if (cachedPrimers.containsKey(key)) {
            char baseChar = (char) Block.BLOCK_STATE_IDS.get(profile.getBaseBlock());
            ChunkPrimer primer = cachedPrimers.get(key);
            IPrimerDriver driver = LostCityConfiguration.OPTIMIZED_CHUNKGEN ? new OptimizedDriver() : new SafeDriver();
            driver.setPrimer(primer);
            ChunkHeightmap heightmap = new ChunkHeightmap(driver, profile.LANDSCAPE_TYPE, profile.GROUNDLEVEL, baseChar);
            cachedHeightmaps.put(key, heightmap);
            return heightmap;
        } else {
            ChunkPrimer primer = generatePrimer(chunkX, chunkZ);
            cachedPrimers.put(key, primer);
            char baseChar = (char) Block.BLOCK_STATE_IDS.get(profile.getBaseBlock());
            IPrimerDriver driver = LostCityConfiguration.OPTIMIZED_CHUNKGEN ? new OptimizedDriver() : new SafeDriver();
            driver.setPrimer(primer);
            ChunkHeightmap heightmap = new ChunkHeightmap(driver, profile.LANDSCAPE_TYPE, profile.GROUNDLEVEL, baseChar);
            cachedHeightmaps.put(key, heightmap);
            return heightmap;
        }
    }
     */

    public boolean validLocation(World world, Random rand, Coord column){

        Biome biome = world.getBiome(column.getBlockPos());
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

        // Strongholds doesn't need to be take in care.
        /*
        Coord stronghold = findNearestStructure(world, VanillaStructure.STRONGHOLD, column);
        if(stronghold != null){
            double strongholdDistance = column.distance(stronghold);
            if(strongholdDistance < 300) return false;
        }
        */

        int y = column.getY();
        int upperLimit = y + 16; //isCubicWorld ? y + 16 : RogueConfig.getInt(RogueConfig.UPPERLIMIT);
        int lowerLimit = y; //isCubicWorld ? y : RogueConfig.getInt(RogueConfig.LOWERLIMIT);

        Coord cursor = new Coord(column.getX(), upperLimit, column.getZ());

        if(!isAirBlock(world, cursor)){
            return false;
        }

        while(!validGroundBlock(world, cursor)){
            cursor.add(Cardinal.DOWN);
            if(cursor.getY() < lowerLimit) return false;
            if(world.getBlockState(cursor.getBlockPos()).getMaterial() == Material.WATER) return false;
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

    public Coord findNearestStructure(World world, VanillaStructure type, Coord pos) {

        ChunkProviderServer chunkProvider = ((WorldServer)world).getChunkProvider();
        String structureName = VanillaStructure.getName(type);

        BlockPos structurebp = null;

        try{
            structurebp = chunkProvider.getNearestStructurePos(world, structureName, pos.getBlockPos(), false);
        } catch(NullPointerException e){
            // happens for some reason if structure type is disabled in Chunk Generator Settings
        }

        if(structurebp == null) return null;

        return new Coord(structurebp);
    }

    public boolean isAirBlock(World world, Coord pos){
        return world.isAirBlock(pos.getBlockPos());
    }

    public boolean validGroundBlock(World world, Coord pos){
        if(isAirBlock(world, pos)) return false;
        IBlockState block = world.getBlockState(pos.getBlockPos());
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
        // TODO
        ChunkCoord key = new ChunkCoord(worldObj.provider.getDimension(), chunkX, chunkZ);
        if (cachedHeightmaps.containsKey(key)) {
            return cachedHeightmaps.get(key);
        } else if (cachedPrimers.containsKey(key)) {
            char baseChar = (char) Block.BLOCK_STATE_IDS.get(profile.getBaseBlock());
            CubePrimer primer = cachedPrimers.get(key);
            ICubeDriver driver = new CubeDriver();
            driver.setPrimer(primer);
            CubicHeightmap heightmap = new CubicHeightmap(driver, profile.LANDSCAPE_TYPE, profile.GROUNDLEVEL, baseChar);
            cachedHeightmaps.put(key, (ICommonHeightmap) heightmap);
            return (ICommonHeightmap) heightmap;
        } else {
            CubePrimer primer = generatePrimer(chunkX, chunkZ);
            cachedPrimers.put(key, primer);
            char baseChar = (char) Block.BLOCK_STATE_IDS.get(profile.getBaseBlock());
            ICubeDriver driver = new CubeDriver();
            driver.setPrimer(primer);
            CubicHeightmap heightmap = new CubicHeightmap(driver, profile.LANDSCAPE_TYPE, profile.GROUNDLEVEL, baseChar);
            cachedHeightmaps.put(key, (ICommonHeightmap) heightmap);
            return (ICommonHeightmap) heightmap;
        }
    }

    public CubePrimer generatePrimer(int chunkX, int chunkZ) {
        random.setSeed(chunkX * 341873128712L + chunkZ * 132897987541L);
        CubePrimer cubePrimer = new CubePrimer();

        // TODO?
        /*
        if (otherGenerator != null) {
            // For ATG, experimental
            otherGenerator.fillChunk(chunkX, chunkZ, chunkprimer);
        } else {
            terrainGenerator.doCoreChunk(chunkX, chunkZ, chunkprimer);
        }
        */

        return cubePrimer;
    }
}
