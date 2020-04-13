package mcjty.lostcities.cubic.world;

import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.cubic.world.generators.*;
import mcjty.lostcities.dimensions.world.WorldTypeTools;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import mcjty.lostcities.dimensions.world.lost.cityassets.WorldStyle;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.init.Blocks;
import org.spongepowered.noise.module.source.Perlin;

import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.worldObj;

public class CubicCityUtils {
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

    // Generators

    public static BuildingGenerator buildingGenerator;
    public static PartGenerator partGenerator;
    public static RailsGenerator railsGenerator;
    public static StreetGenerator streetGenerator;
    public static RubbleGenerator rubbleGenerator;
    public static DebrisGenerator debrisGenerator;
    public static RuinsGenerator ruinsGenerator;
    public static DoorsGenerator doorsGenerator;

    public static WorldStyle worldStyle;

    public static Perlin perlin;

    public static void init(long seed) {
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
        buildingGenerator = new BuildingGenerator();
        railsGenerator = new RailsGenerator();
        streetGenerator = new StreetGenerator();
        rubbleGenerator = new RubbleGenerator();
        debrisGenerator = new DebrisGenerator();
        ruinsGenerator = new RuinsGenerator();
        doorsGenerator = new DoorsGenerator();
    }
}
