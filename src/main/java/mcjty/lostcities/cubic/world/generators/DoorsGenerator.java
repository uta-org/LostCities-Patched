package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.cubic.world.CubicCityWorldPopulator;
import mcjty.lostcities.cubic.world.ICommonGeneratorProvider;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.varia.ChunkCoord;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static mcjty.lostcities.cubic.world.CubicCityUtils.airChar;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.driver;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.worldObj;

public class DoorsGenerator
{
    public class FacingModel {
        private Set<EnumFacing> buildings = new HashSet<>();
        private EnumFacing facing;
        private EnumFacing currentFacing;
        private BuildingInfo info;
        private BuildingGenerator.DoorModel door;
        private ICommonGeneratorProvider provider;
        private char filler;

        private List<EnumFacing> facings;
        {
            facings = new ArrayList<>();
            facings.add(EnumFacing.EAST);
            facings.add(EnumFacing.WEST);
            facings.add(EnumFacing.NORTH);
            facings.add(EnumFacing.SOUTH);
        };

        private FacingModel() {}

        public FacingModel(BuildingInfo info, BuildingGenerator.DoorModel door,  ICommonGeneratorProvider provider, char filler)
        {
            this(info, door, provider, filler, true);
        }

        public FacingModel(BuildingInfo info, BuildingGenerator.DoorModel door, ICommonGeneratorProvider provider, char filler, boolean init) {
            this.info = info;
            this.provider = provider;
            this.filler = filler;

            if(init)
                init();
        }

        public void add(EnumFacing facing) {
            buildings.add(facing);
        }

        public EnumFacing getFacing() {
            return facing;
        }

        public void setFacing(EnumFacing facing) {
            this.facing = facing;
        }

        public BuildingInfo getInfo() {
            return info;
        }

        public BuildingGenerator.DoorModel getDoor() {
            return door;
        }

        public char getFiller() {
            return filler;
        }

        private void init() {
            EnumFacing facing = null;
            List<EnumFacing> valid = new ArrayList<>();

            for (int i = 0; i < 4; ++i) {
                switch(i) {
                    case 0:
                        facing = EnumFacing.EAST;
                        break;

                    case 1:
                        facing = EnumFacing.WEST;
                        break;

                    case 2:
                        facing = EnumFacing.NORTH;
                        break;

                    case 3:
                        facing = EnumFacing.SOUTH;
                        break;
                }

                if(isAdjacentBuilding(facing, provider)) {
                    add(facing);
                }
                else {
                    valid.add(facing);
                }

                EnumFacing validFacing = valid.get(worldObj.rand.nextInt(valid.size()));
                setFacing(validFacing);
            }
        }

        public void generateDoors() {
            for (EnumFacing facing : facings) {
                currentFacing = facing;
                DoorsGenerator.generateDoors(this);
            }
        }

        public boolean isBuildingFacing() {
            return buildings.contains(currentFacing);
        }

        public boolean isValidFacing() {
            return currentFacing == facing;
        }

        public int getX() {
            if(currentFacing == EnumFacing.NORTH) {
                return 7;
            } else if(currentFacing == EnumFacing.SOUTH) {
                return 7;
            } else if(currentFacing == EnumFacing.WEST) {
                return 0;
            } else if(currentFacing == EnumFacing.EAST) {
                return 15;
            }

            return -1;
        }

        public int getZ() {
            if(currentFacing == EnumFacing.NORTH) {
                return 0;
            } else if(currentFacing == EnumFacing.SOUTH) {
                return 15;
            } else if(currentFacing == EnumFacing.WEST) {
                return 7;
            } else if(currentFacing == EnumFacing.EAST) {
                return 7;
            }

            return -1;
        }

        public boolean isX() {
            return currentFacing == EnumFacing.NORTH || currentFacing == EnumFacing.SOUTH;
        }
    }

    public void generateDoors(BuildingInfo info, ICommonGeneratorProvider provider) {
        char filler = info.getCompiledPalette().get(info.getBuilding().getFillerBlock());

        for (BuildingGenerator.DoorModel door : info.getDoorTodo()) {
            driver.setLocalBlock(door.getCoord().getChunkX(), 0, door.getCoord().getChunkZ());
            driver.current(0, door.getHeight(), 0);

            new FacingModel(info, door, provider, filler).generateDoors();
        }

        info.clearDoorTodo();
    }

    private static void generateDoors(FacingModel facingModel) {
        BuildingInfo info = facingModel.getInfo();
        BuildingGenerator.DoorModel door = facingModel.getDoor();

        int height = door.getHeight();
        int floor = door.getFloor();

        boolean isX = facingModel.isX();

        int x = facingModel.getX();
        int z = facingModel.getZ();

        char filler = facingModel.getFiller();

        // height--;       // Start generating doors one below for the filler

        EnumFacing facing = facingModel.getFacing();

        for (int i = 0; i <= 1; ++i)
        {
            // 6, 9
            int sx = x + (isX ? (i == 0 ? -1 : 2) : 0);
            int sz = z + (!isX ? (i == 0 ? -1 : 2) : 0);

            if (facingModel.isBuildingFacing()) {
                // Create connection between buildings
                System.out.println("Adjacent building at ["+driver.getTp(sx, sz)+"]: "+facing.toString());

                driver.setBlockRange(sx, height, sz, height + 4, filler);

                // 7, 8
                sx = x + (isX ? i : 0);
                sz = z + (!isX ? i : 0);

                driver.current(sx, height, sz)
                        .add(filler)
                        .add(airChar)
                        .add(airChar)
                        .add(filler);
                return;
            }

            if (floor != 0 || !facingModel.isValidFacing())
                return;

            driver.setBlockRange(sx, height, sz, height + 4, filler);

            // 7, 8
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
        ChunkCoord coord = getCoord(facing, provider);
        return adjacent.hasBuilding && CubicCityWorldPopulator.isCityChunk(coord.getChunkX(), coord.getChunkZ());
    }

    private static BuildingInfo getAdjacent(EnumFacing facing, ICommonGeneratorProvider provider) {
        ChunkCoord coord = getCoord(facing, provider);
        return BuildingInfo.getBuildingInfo(coord.getChunkX(), coord.getChunkZ(), provider);
    }

    private static ChunkCoord getCoord(EnumFacing facing, ICommonGeneratorProvider provider) {
        int x = driver.getLocalX();
        int z = driver.getLocalZ();

        if (facing == EnumFacing.NORTH) {
            z -= 1;
        } else if (facing == EnumFacing.SOUTH) {
            z += 1;
        } else if (facing == EnumFacing.WEST) {
            x -= 1;
        } else if (facing == EnumFacing.EAST) {
            x += 1;
        }

        return new ChunkCoord(provider.getDimensionId(), x, z);
    }
}
