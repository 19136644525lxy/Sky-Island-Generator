package com.skyisland.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * 下界空岛区块生成器
 * 5x5x3下界岩平台(顶层+2层支撑)
 */
public class NetherSkyIslandGenerator extends AbstractSkyIslandChunkGenerator {

    public static final Codec<NetherSkyIslandGenerator> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.getBiomeSource()),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.generatorSettings()),
            Codec.INT.fieldOf("platform_x").forGetter(g -> g.platformX),
            Codec.INT.fieldOf("platform_y").forGetter(g -> g.platformY),
            Codec.INT.fieldOf("platform_z").forGetter(g -> g.platformZ),
            Codec.INT.fieldOf("platform_size").forGetter(g -> g.platformSize)
        ).apply(instance, instance.stable(NetherSkyIslandGenerator::new))
    );

    public NetherSkyIslandGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings,
                                    int platformX, int platformY, int platformZ, int platformSize) {
        super(biomeSource, settings, platformX, platformY, platformZ, platformSize, 3);  // 3层: 顶层+2层支撑
    }

    @Override
    protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public BlockState getPlatformBlock() {
        return Blocks.NETHERRACK.defaultBlockState();
    }

    @Override
    public BlockState getSupportBlock() {
        return Blocks.NETHERRACK.defaultBlockState();
    }

    @Override
    public boolean shouldGeneratePlatform() {
        return true;
    }
}
