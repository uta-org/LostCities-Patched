package mcjty.lostcities.cubic.world.driver;

import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import mcjty.lostcities.dimensions.world.driver.IIndex;
import net.minecraft.block.state.IBlockState;

public interface ICubeDriver {
    void setPrimer(CubePrimer primer);

    CubePrimer getPrimer();

    ICubeDriver current(int x, int y, int z);

    ICubeDriver current(IIndex index);

    /// Return a copy of the current position
    IIndex getCurrent();

    /// Increment the height of the current position
    void incY();
    /// Increment the height of the current position
    void incY(int amount);
    /// Decrement the height of the current position
    void decY();

    void incX();

    void incZ();

    int getX();

    int getY();

    int getZ();

    IIndex getIndex(int x, int y, int z);

    void setBlockRange(int x, int y, int z, int y2, char c);

    void setBlockRangeSafe(int x, int y, int z, int y2, char c);

    /// Set a block at the current position
    ICubeDriver block(char c);

    /// Set a block at the current position
    ICubeDriver block(IBlockState c);

    /// Set a block at the current position and increase the height with 1
    ICubeDriver add(char c);

    char getBlock();

    char getBlockDown();
    char getBlockEast();
    char getBlockWest();
    char getBlockSouth();
    char getBlockNorth();

    char getBlock(int x, int y, int z);

    ICubeDriver copy();    
}
