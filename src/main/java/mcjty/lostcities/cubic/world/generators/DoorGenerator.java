package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.cubic.world.ICommonGeneratorProvider;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import static mcjty.lostcities.cubic.world.CubicCityUtils.airChar;
import static mcjty.lostcities.cubic.world.CubicCityUtils.getRandom;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.driver;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.worldObj;

public class DoorGenerator {
    public static void generateDoors(BuildingInfo info, ICommonGeneratorProvider provider) {
        char filler = info.getCompiledPalette().get(info.getBuilding().getFillerBlock());

        for (BuildingGenerator.DoorModel door : info.getDoorTodo()) {
            driver.setLocalBlock(door.getCoord().getChunkX(), 0, door.getCoord().getChunkZ());
            driver.current(0, door.getHeight(), 0);

            boolean eastFacing = worldObj.rand.nextBoolean();
            boolean westFacing = worldObj.rand.nextBoolean();
            boolean northFacing = worldObj.rand.nextBoolean();
            boolean southFacing = worldObj.rand.nextBoolean();

            if (!(eastFacing && westFacing && northFacing && southFacing)) {
                // int v = getFacingDirection();
                int v = (int) getRandom(0, 4);

                switch (v) {
                    case 0:
                        eastFacing = true;
                        break;

                    case 1:
                        westFacing = true;
                        break;

                    case 2:
                        northFacing = true;
                        break;

                    case 3:
                        southFacing = true;
                        break;
                }
            }

            // North, south: x...
            int height = door.getHeight() + 1;
            // int height = door.getHeight();

            int pos = 7;
            int upper = 15;
            int down = 0;

            if (northFacing) {
                boolean isAdjacentBuilding = isAdjacentBuilding(EnumFacing.NORTH, provider);
                generateDoors(info, door, pos, down, EnumFacing.NORTH, isAdjacentBuilding, filler);
            } else if (southFacing) {
                boolean isAdjacentBuilding = isAdjacentBuilding(EnumFacing.SOUTH, provider);
                generateDoors(info, door, pos, upper, EnumFacing.SOUTH, isAdjacentBuilding, filler);
            } else if (westFacing) {
                boolean isAdjacentBuilding = isAdjacentBuilding(EnumFacing.WEST, provider);
                generateDoors(info, door, down, pos, EnumFacing.WEST, isAdjacentBuilding, filler);
            } else if (eastFacing) {
                boolean isAdjacentBuilding = isAdjacentBuilding(EnumFacing.EAST, provider);
                generateDoors(info, door, upper, pos, EnumFacing.EAST, isAdjacentBuilding, filler);
            }
        }

        info.clearDoorTodo();
    }

    private static void generateDoors(BuildingInfo info, BuildingGenerator.DoorModel door, int x, int z, EnumFacing facing, boolean isAdjacentBuilding, char filler) {
        int height = door.getHeight();
        int floor = door.getFloor();

        // height--;       // Start generating doors one below for the filler

        for (int i = 0; i <= 1; ++i) {
            // 7, 8
            boolean isX = x == 7;

            // 6, 9
            int sx = x + (isX ? (i == 0 ? -1 : 2) : 0);
            int sz = z + (!isX ? (i == 0 ? -1 : 2) : 0);

            driver.setBlockRange(sx, height, sz, height + 4, filler);
            driver.setBlockRange(sz, height, sz, height + 4, filler);

            if (isAdjacentBuilding) {
                // Create connection between buildings

                driver.setBlockRange(sx, height, sz, height + 4, filler);

                sx = x + (isX ? i : 0);
                sz = z + (!isX ? i : 0);
                driver.current(sx, height, sz)
                        .add(filler)
                        .add(airChar)
                        .add(airChar)
                        .add(filler);
                return;
            }

            if (floor != 0)
                return;

            sx = x + (isX ? i : 0);
            sz = z + (!isX ? i : 0);

            driver.current(sx, height, sz)
                    .add(filler)
                    .add(getDoor(info.doorBlock, false, false, facing))
                    .add(getDoor(info.doorBlock, true, false, facing))
                    .add(filler);
            driver.current(sx, height, sz)
                    .add(filler)
                    .add(getDoor(info.doorBlock, false, true, facing))
                    .add(getDoor(info.doorBlock, true, true, facing))
                    .add(filler);
        }
    }

    private static char getDoor(Block door, boolean upper, boolean left, EnumFacing facing) {
        IBlockState bs = door.getDefaultState()
                .withProperty(BlockDoor.HALF, upper ? BlockDoor.EnumDoorHalf.UPPER : BlockDoor.EnumDoorHalf.LOWER)
                .withProperty(BlockDoor.HINGE, left ? BlockDoor.EnumHingePosition.LEFT : BlockDoor.EnumHingePosition.RIGHT)
                .withProperty(BlockDoor.FACING, facing);

        return (char) Block.BLOCK_STATE_IDS.get(bs);
    }

    private static boolean isAdjacentBuilding(EnumFacing facing, ICommonGeneratorProvider provider) {
        BuildingInfo adjacent = getAdjacent(facing, provider);
        return adjacent.hasBuilding;
    }

    private static BuildingInfo getAdjacent(EnumFacing facing, ICommonGeneratorProvider provider) {
        int x = driver.getLocalX();
        int z = driver.getLocalZ();

        if (facing == EnumFacing.NORTH) {
            z += 1;
        } else if (facing == EnumFacing.SOUTH) {
            z -= 1;
        } else if (facing == EnumFacing.WEST) {
            x += 1;
        } else if (facing == EnumFacing.EAST) {
            x -= 1;
        }

        return BuildingInfo.getBuildingInfo(x, z, provider);
    }

}
