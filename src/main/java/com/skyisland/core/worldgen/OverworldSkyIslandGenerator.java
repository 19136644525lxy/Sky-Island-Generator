package com.skyisland.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.biome.BiomeSource;

/**
 * 主世界空岛区块生成器
 * 5x5x1草方块平台(单层), 中间种一棵橡树
 */
public class OverworldSkyIslandGenerator extends AbstractSkyIslandChunkGenerator {

    public static final Codec<OverworldSkyIslandGenerator> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.getBiomeSource()),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.generatorSettings()),
            Codec.INT.fieldOf("platform_x").forGetter(g -> g.platformX),
            Codec.INT.fieldOf("platform_y").forGetter(g -> g.platformY),
            Codec.INT.fieldOf("platform_z").forGetter(g -> g.platformZ),
            Codec.INT.fieldOf("platform_size").forGetter(g -> g.platformSize)
        ).apply(instance, instance.stable(OverworldSkyIslandGenerator::new))
    );

    public OverworldSkyIslandGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings,
                                       int platformX, int platformY, int platformZ, int platformSize) {
        super(biomeSource, settings, platformX, platformY, platformZ, platformSize, 1);
    }

    @Override
    protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public BlockState getPlatformBlock() {
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    @Override
    public BlockState getSupportBlock() {
        return Blocks.DIRT.defaultBlockState();
    }

    @Override
    public boolean shouldGeneratePlatform() {
        return true;
    }

    /**
     * 重写 buildSurface: 在平台中心区块手动放置一棵标准橡树
     * 原理: Features类是data-gen类运行时不存在, 手动放置方块更可靠
     */
    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // 只在平台中心区块生成橡树
        if (chunk.getPos().x == (platformX >> 4) && chunk.getPos().z == (platformZ >> 4)) {
            placeOakTree(region, platformX, platformY + 1, platformZ);
        }
    }

    /**
     * 手动放置标准小橡树
     * 结构: 4格高树干 + 4层树叶
     */
    private void placeOakTree(WorldGenRegion region, int x, int y, int z) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // 树干 Y到Y+3 (4格高)
        for (int dy = 0; dy < 4; dy++) {
            region.setBlock(pos.set(x, y + dy, z), log, 2);
        }

        // 树叶层1 (Y+2): 5x5去四角
        placeLeavesLayer(region, x, y + 2, z, leaves, 2, true);
        // 树叶层2 (Y+3): 5x5去四角 (中心是树干, 跳过)
        placeLeavesLayer(region, x, y + 3, z, leaves, 2, true);
        // 树叶层3 (Y+4): 3x3
        placeLeavesLayer(region, x, y + 4, z, leaves, 1, false);
        // 树叶层4 (Y+5): 1x1
        region.setBlock(pos.set(x, y + 5, z), leaves, 2);
    }

    /**
     * 放置一层树叶
     * @param radius 半径 (2=5x5, 1=3x3)
     * @param skipCorners 是否去掉四角
     */
    private void placeLeavesLayer(WorldGenRegion region, int cx, int cy, int cz, BlockState leaves, int radius, boolean skipCorners) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;  // 中心位置跳过(树干或已放置)
                if (skipCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) continue;  // 去四角
                region.setBlock(pos.set(cx + dx, cy, cz + dz), leaves, 2);
            }
        }
    }
}
