package mcjty.lostcities.cubic.world.driver;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import mcjty.lostcities.dimensions.world.driver.IIndex;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;

public class CubeDriver implements ICubeDriver {
    private CubePrimer primer;
    private ICube cube;
    private int currentX;
    private int currentY;
    private int currentZ;
    private boolean useWorld;
    private World world;
    private int localX;
    private int localZ;
    private boolean useLocal;

    @Override
    public void setPrimer(CubePrimer primer) {
        this.primer = primer;
    }

    @Override
    public CubePrimer getPrimer() {
        return primer;
    }

    public ICube getCube() {
        return cube;
    }

    public void setCube(ICube cube) {
        this.cube = cube;
    }

    public void setWorld(World world) { useWorld = true; this.world = world; }

    public void useLocal() {
        useLocal = true;
    }

    public int getLocalX() {
        return localX;
    }

    public void setLocalX(int localX) {
        this.localX = localX;
    }

    public int getLocalZ() {
        return localZ;
    }

    public void setLocalZ(int localZ) {
        this.localZ = localZ;
    }

    public void setLocalChunk(int chunkX, int chunkZ) {
        this.localX = chunkX * 16;
        this.localZ = chunkZ * 16;
    }

    public void setLocalBlock(int localX, int localZ) {
        this.localX = localX;
        this.localZ = localZ;
    }

    @Override
    public ICubeDriver current(int x, int y, int z) {
        currentX = x;
        currentY = y;
        currentZ = z;
        return this;
    }

    @Override
    public ICubeDriver current(IIndex index) {
        CubeDriver.Index i = (CubeDriver.Index) index;
        currentX = i.x;
        currentY = i.y;
        currentZ = i.z;
        return this;
    }

    @Override
    public IIndex getCurrent() {
        return new CubeDriver.Index(getX(), currentY, getZ());
    }

    @Override
    public void incY() {
        currentY++;
    }

    @Override
    public void incY(int amount) {
        currentY += amount;
    }

    @Override
    public void decY() {
        currentY--;
    }

    @Override
    public void incX() {
        currentX++;
    }

    @Override
    public void incZ() {
        currentZ++;
    }

    @Override
    public int getX() {
        return (useLocal ? localX : 0) + currentX;
    }

    // (useLocal ? localX : 0) +
    @Override
    public int getY() {
        return currentY;
    }

    @Override
    public int getZ() {
        return (useLocal ? localZ : 0) +  currentZ;
    }

    @Override
    public void setBlockRange(int x, int y, int z, int y2, char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);

        while (y < y2) {
            if(useWorld)
                world.setBlockState(useLocal ? new BlockPos(x + localX, y, z + localZ) : new BlockPos(x, y, z), state);
            else
                cube.setBlockState(new BlockPos(x, y, z), state);
            y++;
        }

    }

    @Override
    public void setBlockRangeSafe(int x, int y, int z, int y2, char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);

        while (y < y2) {
            if(useWorld)
                world.setBlockState(useLocal ? new BlockPos(x + localX, y, z + localZ) : new BlockPos(x, y, z), state);
            else
                cube.setBlockState(new BlockPos(x, y, z), state);
            y++;
        }
    }

    @Override
    public ICubeDriver block(char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);

        if(useWorld)
            world.setBlockState(new BlockPos(getX(), currentY, getZ()), state);
        else
            cube.setBlockState(new BlockPos(currentX, currentY, currentZ), state);
        return this;
    }

    @Override
    public ICubeDriver block(IBlockState c) {
        if(useWorld)
            world.setBlockState(new BlockPos(getX(), currentY, getZ()), c);
        else
            cube.setBlockState(new BlockPos(currentX, currentY, currentZ), c);
        return this;

    }

    @Override
    public ICubeDriver add(char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);

        if(useWorld)
            world.setBlockState(new BlockPos(getX(), currentY, getZ()), state);
        else
            cube.setBlockState(new BlockPos(currentX, currentY, currentZ), state);
        return this;
    }

    @Override
    public char getBlock() {
        if(useWorld)
            return (char) Block.BLOCK_STATE_IDS.get(world.getBlockState(new BlockPos(getX(), currentY, getZ())));
        else
            return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY, currentZ));
    }

    public IBlockState getBlockState() {
        if(useWorld)
            return world.getBlockState(new BlockPos(getX(), currentY, getZ()));
        else
            return cube.getBlockState(currentX, currentY, currentZ);
    }

    public IBlockState getBlockState(int x, int y, int z) {
        if(useWorld)
            return world.getBlockState(useLocal ? new BlockPos(x + localX, y, z + localZ) : new BlockPos(x, y, z));
        else
            return cube.getBlockState(x, y, z);
    }

    @Override
    public char getBlockDown() {
        if(useWorld)
            return (char) Block.BLOCK_STATE_IDS.get(world.getBlockState(new BlockPos(getX(), currentY - 1, getZ())));
        else
            return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY - 1, currentZ));
    }

    @Override
    public char getBlockEast() {
        if(useWorld)
            return (char) Block.BLOCK_STATE_IDS.get(world.getBlockState(new BlockPos(getX(), currentY + 1, getZ())));
        else
            return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY + 1, currentZ));
    }

    @Override
    public char getBlockWest() {
        if(useWorld)
            return (char) Block.BLOCK_STATE_IDS.get(world.getBlockState(new BlockPos(getX() - 1, currentY, getZ())));
        else
            return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX - 1, currentY, currentZ));
    }

    @Override
    public char getBlockSouth() {
        if(useWorld)
            return (char) Block.BLOCK_STATE_IDS.get(world.getBlockState(new BlockPos(getX(), currentY, getZ() + 1)));
        else
            return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY, currentZ + 1));
    }

    @Override
    public char getBlockNorth() {
        if(useWorld)
            return (char) Block.BLOCK_STATE_IDS.get(world.getBlockState(new BlockPos(getX(), currentY, getZ() - 1)));
        else
            return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY, currentZ - 1));
    }


    @Override
    public char getBlock(int x, int y, int z) {
        if(useWorld)
            return (char) Block.BLOCK_STATE_IDS.get(world.getBlockState(useLocal ? new BlockPos(x + localX, y, z + localZ) : new BlockPos(x, y, z)));
        else
        return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(x, y, z));
    }

    @Override
    public IIndex getIndex(int x, int y, int z) {
        // checkForCube(x, y, z);
        return new CubeDriver.Index(x, y, z);
    }


    private class Index implements IIndex {
        private final int x;
        private final int y;
        private final int z;

        Index(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CubeDriver.Index index = (CubeDriver.Index) o;
            return x == index.x &&
                    y == index.y &&
                    z == index.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    @Override
    public ICubeDriver copy() {
        CubeDriver driver = new CubeDriver();

        driver.currentX = currentX;
        driver.currentY = currentY;
        driver.currentZ = currentZ;
        driver.primer = primer;
        driver.cube = cube;
        driver.useWorld = useWorld;
        driver.world = world;
        driver.localX = localX;
        driver.localZ = localZ;

        return driver;
    }
}
