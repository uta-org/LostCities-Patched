package mcjty.lostcities.cubic.world.generators;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import java.util.Set;

public class States {
    public static void addStates(Block block, Set<Character> set) {
        for (int m = 0; m < 16; m++) {
            try {
                IBlockState state = block.getStateFromMeta(m);
                set.add((char) Block.BLOCK_STATE_IDS.get(state));
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
