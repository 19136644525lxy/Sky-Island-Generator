package com.skyisland.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * 末地空岛区块生成器
 * 不生成平台: 末地黑曜石平台由原版 ServerLevel.makeObsidianPlatform 在玩家进入时自动生成(100,50,0)
 * 龙池位置由 MixinEndDragonFight 移到 (0,48,0)
 */
public class EndSkyIslandGenerator extends AbstractSkyIslandChunkGenerator {

    public static final Codec<EndSkyIslandGenerator> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.getBiomeSource()),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.generatorSettings()),
            Codec.INT.fieldOf("platform_x").forGetter(g -> g.platformX),
            Codec.INT.fieldOf("platform_y").forGetter(g -> g.platformY),
            Codec.INT.fieldOf("platform_z").forGetter(g -> g.platformZ),
            Codec.INT.fieldOf("platform_size").forGetter(g -> g.platformSize)
        ).apply(instance, instance.stable(EndSkyIslandGenerator::new))
    );

    public EndSkyIslandGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings,
                                 int platformX, int platformY, int platformZ, int platformSize) {
        super(biomeSource, settings, platformX, platformY, platformZ, platformSize, 0);  // 0层: 不生成平台
    }

    @Override
    protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public BlockState getPlatformBlock() {
        return Blocks.END_STONE.defaultBlockState();
    }

    @Override
    public BlockState getSupportBlock() {
        return Blocks.END_STONE.defaultBlockState();
    }

    @Override
    public boolean shouldGeneratePlatform() {
        return false;  // 末地用原版黑曜石平台
    }
}
