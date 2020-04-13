package mcjty.lostcities.cubic.world.generators;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import mcjty.lostcities.api.RailChunkType;
import mcjty.lostcities.cubic.world.ICommonHeightmap;
import mcjty.lostcities.cubic.world.driver.CubeDriver;
import mcjty.lostcities.dimensions.world.lost.*;
import mcjty.lostcities.dimensions.world.lost.cityassets.BuildingPart;
import mcjty.lostcities.dimensions.world.lost.cityassets.CompiledPalette;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static mcjty.lostcities.cubic.world.CubicCityWorldPopulator.*;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.worldObj;
import static mcjty.lostcities.cubic.world.generators.PartGenerator.*;
import static mcjty.lostcities.cubic.world.generators.Utils.*;

import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.driver;

import static mcjty.lostcities.cubic.world.CubicCityUtils.*;

public class StreetGenerator {

    private static final boolean generateBorders = false;

    public void generate(BuildingInfo info, ICommonHeightmap heightmap, Random rand) {
        boolean xRail = info.hasXCorridor();
        boolean zRail = info.hasZCorridor();

        int groundLevel = info.getCityGroundLevel(!USE_HEIGHT_CACHE);

        if (xRail || zRail) {
            generateCorridors(info, xRail, zRail, groundLevel);
        }

        Railway.RailChunkInfo railInfo = info.getRailInfo();
        boolean canDoParks = info.getHighwayXLevel() != groundLevel && info.getHighwayZLevel() != groundLevel
                && railInfo.getType() != RailChunkType.STATION_SURFACE
                && (railInfo.getType() != RailChunkType.STATION_EXTENSION_SURFACE || railInfo.getLevel() < groundLevel);

        if (canDoParks) {
            int height = groundLevel;

            BuildingInfo.StreetType streetType = info.streetType;
            boolean elevated = info.isElevatedParkSection();
            if (elevated) {
                Character elevationBlock = info.getCityStyle().getParkElevationBlock();
                char elevation = info.getCompiledPalette().get(elevationBlock);
                streetType = BuildingInfo.StreetType.PARK;
                for (int x = 0; x < 16; ++x) {
                    driver.current(x, height, 0);
                    for (int z = 0; z < 16; ++z) {
                        driver.block(elevation).incZ();
                    }
                }
                height++;
            }

            switch (streetType) {
                case NORMAL:
                    generateNormalStreetSection(info, height);
                    break;
                case FULL:
                    generateFullStreetSection(height);
                    break;
                case PARK:
                    generateParkSection(info, height, elevated);
            }
            height++;

            if (streetType == BuildingInfo.StreetType.PARK || info.fountainType != null) {
                BuildingPart part;
                if (streetType == BuildingInfo.StreetType.PARK) {
                    part = info.parkType;
                } else {
                    part = info.fountainType;
                }
                generatePart(info, part, Transform.ROTATE_NONE, 0, height, 0, false);
            }

            generateRandomVegetation(info, rand, height);

            generateFrontPart(info, height, info.getXmin(), Transform.ROTATE_NONE);
            generateFrontPart(info, height, info.getZmin(), Transform.ROTATE_90);
            generateFrontPart(info, height, info.getXmax(), Transform.ROTATE_180);
            generateFrontPart(info, height, info.getZmax(), Transform.ROTATE_270);
        }

        // Not used... TODO: Check if this affect to other structures
        generateBorders(info, canDoParks, groundLevel);
    }

    public void generateStreetDecorations(BuildingInfo info, int oy) {
        Direction stairDirection = info.getActualStairDirection();
        if (stairDirection != null) {
            BuildingPart stairs = info.stairType;
            Transform transform;
            // int oy = info.getCityGroundLevel(!USE_HEIGHT_CACHE) + 1;
            switch (stairDirection) {
                case XMIN:
                    transform = Transform.ROTATE_NONE;
                    break;
                case XMAX:
                    transform = Transform.ROTATE_180;
                    break;
                case ZMIN:
                    transform = Transform.ROTATE_90;
                    break;
                case ZMAX:
                    transform = Transform.ROTATE_270;
                    break;
                default:
                    throw new RuntimeException("Cannot happen!");
            }

            generatePart(info, stairs, transform, 0, oy, 0, false);
        }
    }

    private void generateCorridors(BuildingInfo info, boolean xRail, boolean zRail, int y) {
        IBlockState railx = Blocks.RAIL.getDefaultState().withProperty(BlockRail.SHAPE, BlockRailBase.EnumRailDirection.EAST_WEST);
        char railxC = (char) Block.BLOCK_STATE_IDS.get(railx);
        IBlockState railz = Blocks.RAIL.getDefaultState();
        char railzC = (char) Block.BLOCK_STATE_IDS.get(railz);

        Character corridorRoofBlock = info.getCityStyle().getCorridorRoofBlock();
        Character corridorGlassBlock = info.getCityStyle().getCorridorGlassBlock();
        CompiledPalette palette = info.getCompiledPalette();

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                char b;
                if ((xRail && z >= 7 && z <= 10) || (zRail && x >= 7 && x <= 10)) {
                    int height = info.groundLevel - 5;
                    if (xRail && z == 10) {
                        b = railxC;
                    } else if (zRail && x == 10) {
                        b = railzC;
                    } else {
                        b = airChar;
                    }
                    driver.current(x, height, z).add(b).add(airChar).add(airChar);

                    if ((xRail && x == 7 && (z == 8 || z == 9)) || (zRail && z == 7 && (x == 8 || x == 9))) {
                        char glass = palette.get(corridorGlassBlock);
                        info.addLightingUpdateTodo(new BlockPos(x, height, z));
                        driver.add(glass).add(glowstoneChar);
                    } else {
                        char roof = palette.get(corridorRoofBlock);
                        driver.add(roof).add(roof);
                    }
                } else {
                    driver.setBlockRange(x, info.groundLevel - 5, z, y, baseChar);
                }
            }
        }
    }

    private void generateRandomVegetation(BuildingInfo info, Random rand, int height) {
        if (height == 0) return; // Leaf blocks need to sit on something, and the void isn't something
        if (info.getXmin().hasBuilding) {
            for (int x = 0; x < info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS; x++) {
                zLoop: for (int z = 0; z < 16; z++) {
                    driver.current(x, height, z);
                    // @todo can be more optimal? Only go down to non air in case random succeeds?
                    while (driver.getBlockDown() == airChar) {
                        driver.decY();
                        if (driver.getY() == 0) {
                            // whoops, it's air all the way down. No leaf blocks here
                            continue zLoop;
                        }
                    }
                    float v = Math.min(.8f, info.profile.CHANCE_OF_RANDOM_LEAFBLOCKS * (info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS + 1 - x));
                    int cnt = 0;
                    while (rand.nextFloat() < v && cnt < 30) {
                        driver.add(getRandomLeaf());
                        cnt++;
                    }
                }
            }
        }
        if (info.getXmax().hasBuilding) {
            for (int x = 15 - info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS; x < 15; x++) {
                zLoop: for (int z = 0; z < 16; z++) {
                    driver.current(x, height, z);
                    // @todo can be more optimal? Only go down to non air in case random succeeds?
                    while (driver.getBlockDown() == airChar) {
                        driver.decY();
                        if (driver.getY() == 0) {
                            // whoops, it's air all the way down. No leaf blocks here
                            continue zLoop;
                        }
                    }
                    float v = Math.min(.8f, info.profile.CHANCE_OF_RANDOM_LEAFBLOCKS * (x - 14 + info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS));
                    int cnt = 0;
                    while (rand.nextFloat() < v && cnt < 30) {
                        driver.add(getRandomLeaf());
                        cnt++;
                    }
                }
            }
        }
        if (info.getZmin().hasBuilding) {
            for (int z = 0; z < info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS; z++) {
                xLoop: for (int x = 0; x < 16; x++) {
                    driver.current(x, height, z);
                    // @todo can be more optimal? Only go down to non air in case random succeeds?
                    while (driver.getBlockDown() == airChar) {
                        driver.decY();
                        if (driver.getY() == 0) {
                            // whoops, it's air all the way down. No leaf blocks here
                            continue xLoop;
                        }
                    }
                    float v = Math.min(.8f, info.profile.CHANCE_OF_RANDOM_LEAFBLOCKS * (info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS + 1 - z));
                    int cnt = 0;
                    while (rand.nextFloat() < v && cnt < 30) {
                        driver.add(getRandomLeaf());
                        cnt++;
                    }
                }
            }
        }
        if (info.getZmax().hasBuilding) {
            for (int z = 15 - info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS; z < 15; z++) {
                xLoop: for (int x = 0; x < 16; x++) {
                    driver.current(x, height, z);
                    // @todo can be more optimal? Only go down to non air in case random succeeds?
                    while (driver.getBlockDown() == airChar) {
                        driver.decY();
                        if (driver.getY() == 0) {
                            // whoops, it's air all the way down. No leaf blocks here
                            continue xLoop;
                        }
                    }
                    float v = info.profile.CHANCE_OF_RANDOM_LEAFBLOCKS * (z - 14 + info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS);
                    int cnt = 0;
                    while (rand.nextFloat() < v && cnt < 30) {
                        driver.add(getRandomLeaf());
                        cnt++;
                    }
                }
            }
        }
    }

    private void generateParkSection(BuildingInfo info, int height, boolean elevated) {
        char b;
        boolean el00 = info.getXmin().getZmin().isElevatedParkSection();
        boolean el10 = info.getZmin().isElevatedParkSection();
        boolean el20 = info.getXmax().getZmin().isElevatedParkSection();
        boolean el01 = info.getXmin().isElevatedParkSection();
        boolean el21 = info.getXmax().isElevatedParkSection();
        boolean el02 = info.getXmin().getZmax().isElevatedParkSection();
        boolean el12 = info.getZmax().isElevatedParkSection();
        boolean el22 = info.getXmax().getZmax().isElevatedParkSection();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (x == 0 || x == 15 || z == 0 || z == 15) {
                    b = street;
                    if (elevated) {
                        if (x == 0 && z == 0) {
                            if (el01 && el00 && el10) {
                                b = grassChar;
                            }
                        } else if (x == 15 && z == 0) {
                            if (el21 && el20 && el10) {
                                b = grassChar;
                            }
                        } else if (x == 0 && z == 15) {
                            if (el01 && el02 && el12) {
                                b = grassChar;
                            }
                        } else if (x == 15 && z == 15) {
                            if (el12 && el22 && el21) {
                                b = grassChar;
                            }
                        } else if (x == 0) {
                            if (el01) {
                                b = grassChar;
                            }
                        } else if (x == 15) {
                            if (el21) {
                                b = grassChar;
                            }
                        } else if (z == 0) {
                            if (el10) {
                                b = grassChar;
                            }
                        } else if (z == 15) {
                            if (el12) {
                                b = grassChar;
                            }
                        }
                    }
                } else {
                    b = grassChar;
                }
                driver.current(x, height, z).block(b);
            }
        }
    }

    private void generateFullStreetSection(int height) {
        char b;
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (isSide(x, z)) {
                    b = street;
                } else {
                    b = street2;
                }
                driver.current(x, height, z).block(b);
            }
        }
    }

    private void generateNormalStreetSection(BuildingInfo info, int height) {
//        char defaultStreet = info.profile.isFloating() ? street2 : streetBase;

        char defaultStreet = streetBase;
        char b;

        // TODO
        Random rand = new Random();
        boolean put0 = rand.nextBoolean();
        boolean put1 = rand.nextBoolean();
        boolean put2 = rand.nextBoolean();
        boolean put3 = rand.nextBoolean();

        if(!(put0 && put1 && put2 && put3)) {
            int v = (int)getRandom(0, 4);
            switch (v) {
                case 0:
                    put0 = true;
                    break;

                case 1:
                    put1 = true;
                    break;

                case 2:
                    put2 = true;
                    break;

                case 3:
                    put3 = true;
                    break;
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                b = defaultStreet;

                if (isStreetBorder(x, z)) {
                    if (x <= streetBorder && z > streetBorder && z < (15 - streetBorder)
                            && (hasRoadConnection(info, info.getXmin(), put0) || (info.getXmin().hasXBridge(provider) != null))) {
                        b = street;
                    } else if (x >= (15 - streetBorder) && z > streetBorder && z < (15 - streetBorder)
                            && (hasRoadConnection(info, info.getXmax(), put1) || (info.getXmax().hasXBridge(provider) != null))) {
                        b = street;
                    } else if (z <= streetBorder && x > streetBorder && x < (15 - streetBorder)
                            && (hasRoadConnection(info, info.getZmin(), put2) || (info.getZmin().hasZBridge(provider) != null))) {
                        b = street;
                    } else if (z >= (15 - streetBorder) && x > streetBorder && x < (15 - streetBorder)
                            && (hasRoadConnection(info, info.getZmax(), put3) || (info.getZmax().hasZBridge(provider) != null))) {
                        b = street;
                    }
                } else {
                    b = street;
                }

                driver.current(x, height, z);

                CubePos pos = driver.getCubePos();
                if(b == defaultStreet) {
                    if(driver.getBlock() == airChar || invalid.contains(driver.getBlockState().getMaterial()))
                        continue;

                    IBlockState filler = worldObj.getBiome(pos.getCenterBlockPos()).fillerBlock;
                    if(filler.getBlock().getUnlocalizedName().contains("dirt"))
                        filler = Blocks.GRASS.getDefaultState();
                    b = (char)Block.BLOCK_STATE_IDS.get(filler);
                }

                fixDriver();
                driver.block(b);
            }
        }
    }

    // TODO; Inplement on more places? (highways, parks...)
    private void fixDriver() {
        // Fix Y values, we need to adapt to the terrain
        char cur = driver.getBlock();
        IBlockState curState = driver.getBlockState();

        char up = driver.getBlockUp();
        IBlockState upState = driver.getBlockStateUp();

        boolean curStateNotValid = !invalid.contains(curState.getMaterial());
        boolean upStateNotValid = !invalid.contains(upState.getMaterial());

        if(cur == airChar) {
            goDownOnDriver();
        }
        else if(curStateNotValid || upStateNotValid) {
            goDownOnDriver();
            // driver.decY();
        }
        else if(up != airChar) {
            // Restore in case we are on a invalid material
            int counter = 0;
            while(counter < 16) {
                driver.incY();
                if(driver.getBlock() == airChar)
                    break;

                ++counter;
            }
        }
    }

    private void goDownOnDriver() {
        int counter = 0;
        while(counter < 16) {
            driver.decY();
            if(driver.getBlock() != airChar && !invalid.contains(driver.getBlockState().getMaterial()))
                break;

            ++counter;
        }
    }

    private static List<Material> invalid;
    {
        invalid = new ArrayList<>();
        invalid.add(Material.WOOD);
        invalid.add(Material.WATER);
        invalid.add(Material.CACTUS);
        invalid.add(Material.GOURD);
        invalid.add(Material.LEAVES);
        invalid.add(Material.PLANTS);
    };

    private boolean hasRoadConnection(BuildingInfo i1, BuildingInfo i2, boolean put) {
        if (!i1.doesRoadExtendTo()) {
            return false;
        }
        if (!i2.doesRoadExtendTo()) {
            return false;
        }

        return put;
    }

    public void generateHighways(int chunkX, int chunkZ, BuildingInfo info) {
        int levelX = Highway.getXHighwayLevel(chunkX, chunkZ, provider, info.profile);
        int levelZ = Highway.getZHighwayLevel(chunkX, chunkZ, provider, info.profile);
        if (levelX == levelZ && levelX >= 0) {
            // Crossing
            generateHighwayPart(info, levelX, Transform.ROTATE_NONE, info.getXmax(), info.getZmax(), "_bi");
        } else if (levelX >= 0 && levelZ >= 0) {
            // There are two highways on different level. Make sure the lowest one is done first because it
            // will clear out what is above it
            if (levelX == 0) {
                generateHighwayPart(info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), "");
                generateHighwayPart(info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), "");
            } else {
                generateHighwayPart(info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), "");
                generateHighwayPart(info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), "");
            }
        } else {
            if (levelX >= 0) {
                generateHighwayPart(info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), "");
            } else if (levelZ >= 0) {
                generateHighwayPart(info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), "");
            }
        }
    }

    public static void generateBorders(BuildingInfo info, boolean canDoParks, int y) {
        if(!generateBorders)
            return;

        Character borderBlock = info.getCityStyle().getBorderBlock();

        switch (info.profile.LANDSCAPE_TYPE) {
            case DEFAULT:
                fillToBedrockStreetBlock(info);
                break;
            case FLOATING:
                fillMainStreetBlock(info, borderBlock, 3, y);
                break;
            case CAVERN:
                fillMainStreetBlock(info, borderBlock, 2, y);
                break;
            case SPACE:
                fillToGroundStreetBlock(info, y);
                break;
        }

        // TODO
        if (doBorder(info, Direction.XMIN)) {
            int x = 0;
            for (int z = 0; z < 16; z++) {
                generateBorder(info, canDoParks, x, y, z, Direction.XMIN.get(info));
            }
        }
        if (doBorder(info, Direction.XMAX)) {
            int x = 15;
            for (int z = 0; z < 16; z++) {
                generateBorder(info, canDoParks, x, y, z, Direction.XMAX.get(info));
            }
        }
        if (doBorder(info, Direction.ZMIN)) {
            int z = 0;
            for (int x = 0; x < 16; x++) {
                generateBorder(info, canDoParks, x, y, z, Direction.ZMIN.get(info));
            }
        }
        if (doBorder(info, Direction.ZMAX)) {
            int z = 15;
            for (int x = 0; x < 16; x++) {
                generateBorder(info, canDoParks, x, y, z, Direction.ZMAX.get(info));
            }
        }
    }

    /**
     * Fill base blocks under streets to bedrock
     */
    private static void fillToBedrockStreetBlock(BuildingInfo info) {
        /* // TODO
        // Base blocks below streets
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                driver.setBlockRange(x, info.profile.BEDROCK_LAYER, z, info.getCityGroundLevel(), baseChar);
            }
        }
        */
    }

    /**
     * Fill from a certain lowest level with base blocks until non air is hit
     */
    private static void fillToGroundStreetBlock(BuildingInfo info, int lowestLevel) {
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                int y = lowestLevel - 1;
                driver.current(x, y, z);
                while (y > 1 && driver.getBlock() == airChar) {
                    driver.block(baseChar).decY();
                    y--;
                }
            }
        }
    }

    /**
     * Fill a main street block with base blocks and border blocks at the bottom
     */
    private static void fillMainStreetBlock(BuildingInfo info, Character borderBlock, int offset, int y) {
        char border = info.getCompiledPalette().get(borderBlock);
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                driver.setBlockRange(x, y - (offset - 1), z, y, baseChar);
                driver.current(x, y - offset, z).block(border);
            }
        }
    }

    /**
     * Generate a single border column for one side of a street block
     */
    private static void generateBorder(BuildingInfo info, boolean canDoParks, int x, int y, int z, BuildingInfo adjacent) {
        Character borderBlock = info.getCityStyle().getBorderBlock();
        Character wallBlock = info.getCityStyle().getWallBlock();
        char wall = info.getCompiledPalette().get(wallBlock);

        switch (info.profile.LANDSCAPE_TYPE) {
            case DEFAULT:
                // We do the ocean border 6 lower then groundlevel
                setBlocksFromPalette(x, info.groundLevel - 6, z, y + 1, info.getCompiledPalette(), borderBlock);
                break;
            case SPACE: {
                int adjacentY = y - 8;
                if (adjacent.isCity) {
                    adjacentY = Math.min(adjacentY, adjacent.getCityGroundLevel(!USE_HEIGHT_CACHE));
                } else {
                    ICommonHeightmap adjacentHeightmap = provider.getHeightmap(info.chunkX, info.chunkZ);
                    int minimumHeight = adjacentHeightmap.getMinimumHeight();
                    adjacentY = Math.min(adjacentY, minimumHeight-2);
                }

                if (adjacentY > 5) {
                    setBlocksFromPalette(x, adjacentY, z, y + 1, info.getCompiledPalette(), borderBlock);
                }
                break;
            }
            case FLOATING:
                setBlocksFromPalette(x, y - 3, z, y + 1, info.getCompiledPalette(), borderBlock);
                if (isCorner(x, z)) {
                    generateBorderSupport(info, wall, x, y, z, 3);
                }
                break;
            case CAVERN:
                setBlocksFromPalette(x, y - 2, z, y + 1, info.getCompiledPalette(), borderBlock);
                if (isCorner(x, z)) {
                    generateBorderSupport(info, wall, x, y, z, 2);
                }
                break;
        }
        if (canDoParks) {
            if (!borderNeedsConnectionToAdjacentChunk(info, x, z)) {
                driver.current(x, y + 1, z).block(wall);
            } else {
                driver.current(x, y + 1, z).block(airChar);
            }
        }
    }

    /**
     * Generate a column of wall blocks (and stone below that in water)
     */
    private static void generateBorderSupport(BuildingInfo info, char wall, int x, int oy, int z, int offset) {
        ICommonHeightmap heightmap = provider.getHeightmap(info.chunkX, info.chunkZ);
        int height = heightmap.getHeight(x, z);
        if (height > 1) {
            // None void
            int y = oy - offset - 1;
            driver.current(x, y, z);
            while (y > 1 && driver.getBlock() == airChar) {
                driver.block(wall).decY();
                y--;
            }
            while (y > 1 && driver.getBlock() == liquidChar) {
                driver.block(baseChar).decY();
                y--;
            }
        }
    }
}
