package mcjty.lostcities.cubic.world.driver;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import mcjty.lostcities.cubic.CubicCityWorldProcessor;
import mcjty.lostcities.dimensions.world.driver.IIndex;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class CubeDriver implements ICubeDriver {
    private CubePrimer primer;
    private ICube cube;
    private int currentX;
    private int currentY;
    private int currentZ;

    private void checkForCube() {
        if(cube == null) cube = CubicCityWorldProcessor.cachedCubes.get(CubePos.fromBlockCoords(currentX, currentY, currentZ));
    }

    private void checkForCube(int x, int y, int z) {
        if(cube == null) cube = CubicCityWorldProcessor.cachedCubes.get(CubePos.fromBlockCoords(x, y, z));
    }


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
        return new CubeDriver.Index(currentX, currentY, currentZ);
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
        return currentX;
    }

    @Override
    public int getY() {
        return currentY;
    }

    @Override
    public int getZ() {
        return currentZ;
    }

    @Override
    public void setBlockRange(int x, int y, int z, int y2, char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);

        while (y < y2) {
            checkForCube(x, y, z);
            cube.setBlockState(new BlockPos(x, y, z), state);
            y++;
        }

    }

    @Override
    public void setBlockRangeSafe(int x, int y, int z, int y2, char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);

        while (y < y2) {
            checkForCube(x, y, z);
            cube.setBlockState(new BlockPos(x, y, z), state);
            y++;
        }
    }

    @Override
    public ICubeDriver block(char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);

        checkForCube();
        cube.setBlockState(new BlockPos(currentX, currentY, currentZ), state);
        return this;
    }

    @Override
    public ICubeDriver block(IBlockState c) {
        checkForCube();
        cube.setBlockState(new BlockPos(currentX, currentY, currentZ), c);
        return this;

    }

    @Override
    public ICubeDriver add(char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);

        checkForCube();
        cube.setBlockState(new BlockPos(currentX, currentY, currentZ), state);
        return this;
    }

    @Override
    public char getBlock() {
        checkForCube();
        return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY, currentZ));
    }

    public IBlockState getBlockState() {
        checkForCube();
        return cube.getBlockState(currentX, currentY, currentZ);
    }

    public IBlockState getBlockState(int x, int y, int z) {
        checkForCube(x, y, z);
        return cube.getBlockState(x, y, z);
    }

    @Override
    public char getBlockDown() {
        checkForCube(currentX, currentY - 1, currentZ);
        return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY - 1, currentZ));
    }

    @Override
    public char getBlockEast() {
        checkForCube(currentX, currentY + 1, currentZ);
        return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY + 1, currentZ));
    }

    @Override
    public char getBlockWest() {
        checkForCube(currentX - 1, currentY, currentZ);
        return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX - 1, currentY, currentZ));
    }

    @Override
    public char getBlockSouth() {
        checkForCube(currentX, currentY, currentZ + 1);
        return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY, currentZ + 1));
    }

    @Override
    public char getBlockNorth() {
        checkForCube(currentX, currentY, currentZ - 1);
        return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(currentX, currentY, currentZ - 1));
    }


    @Override
    public char getBlock(int x, int y, int z) {
        checkForCube(x, y, z);
        return (char) Block.BLOCK_STATE_IDS.get(cube.getBlockState(x, y, z));
    }

    @Override
    public IIndex getIndex(int x, int y, int z) {
        checkForCube(x, y, z);
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
        return driver;
    }
}
