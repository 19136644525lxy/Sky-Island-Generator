package com.skyisland.core.worldgen;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 空岛区块生成器抽象基类
 *
 * 关键设计: 继承 NoiseBasedChunkGenerator
 * 原理: ChunkMap.createRandomState 检查 instanceof NoiseBasedChunkGenerator,
 *       若是则用 generatorSettings() 创建正确的 RandomState, MultiNoiseBiomeSource 才能正常工作。
 *
 * 覆写策略:
 * - fillFromNoise: 不调用 super(不生成噪声地形), 只放平台方块
 * - buildSurface / applyCarvers: 空实现(子类可重写 buildSurface 放橡树)
 * - applyBiomeDecoration: 重写, 只生成结构方块, 不生成地物(避免树木/末地石岛等)
 *                         过滤废弃传送门(ruined_portal)
 * - createState: 不重写, 使用原版(保证末地城等结构正常计算)
 */
public abstract class AbstractSkyIslandChunkGenerator extends NoiseBasedChunkGenerator {

    protected final int platformX;
    protected final int platformY;
    protected final int platformZ;
    protected final int platformSize;
    protected final int platformLayers;

    public AbstractSkyIslandChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings,
                                           int platformX, int platformY, int platformZ, int platformSize, int platformLayers) {
        super(biomeSource, settings);
        this.platformX = platformX;
        this.platformY = platformY;
        this.platformZ = platformZ;
        this.platformSize = platformSize;
        this.platformLayers = platformLayers;
    }

    /** 子类返回平台顶层方块 */
    public abstract BlockState getPlatformBlock();

    /** 子类返回平台下方支撑方块 */
    public abstract BlockState getSupportBlock();

    /** 子类返回是否生成平台(末地=false, 用原版黑曜石平台) */
    public abstract boolean shouldGeneratePlatform();

    /**
     * 核心方法: 只放置平台方块, 其余全空
     * 不调用 super.fillFromNoise, 避免生成噪声地形
     */
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                       StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            if (!shouldGeneratePlatform()) {
                return chunk;
            }
            placePlatform(chunk);
            return chunk;
        }, executor);
    }

    /** 在指定区块放置平台方块 */
    protected void placePlatform(ChunkAccess chunk) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();

        int half = (platformSize - 1) / 2;
        int platformMinX = platformX - half;
        int platformMaxX = platformX + half;
        int platformMinZ = platformZ - half;
        int platformMaxZ = platformZ + half;

        boolean intersectsX = platformMaxX >= chunkMinX && platformMinX <= chunkMinX + 15;
        boolean intersectsZ = platformMaxZ >= chunkMinZ && platformMinZ <= chunkMinZ + 15;
        if (!intersectsX || !intersectsZ) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState platformBlock = getPlatformBlock();
        BlockState supportBlock = getSupportBlock();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        int startX = Math.max(0, platformMinX - chunkMinX);
        int endX = Math.min(15, platformMaxX - chunkMinX);
        int startZ = Math.max(0, platformMinZ - chunkMinZ);
        int endZ = Math.min(15, platformMaxZ - chunkMinZ);

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int dy = 0; dy < platformLayers; dy++) {
                    int y = platformY - dy;
                    if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) continue;
                    BlockState state = (dy == 0) ? platformBlock : supportBlock;
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                    oceanFloor.update(x, y, z, state);
                    worldSurface.update(x, y, z, state);
                }
            }
        }
    }

    /**
     * 重写 applyBiomeDecoration: 生成结构 + 特殊放行的地物(黑曜石柱)
     * 原理: 参考 CarpetSkyAdditions 的实现
     *       原版方法会生成结构(placeInChunk) + 地物(placeWithBiomeCheck)
     *       空岛模式只需要结构 + 黑曜石柱
     *       过滤废弃传送门(ruined_portal)
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos chunkPos = chunk.getPos();
        SectionPos sectionPos = SectionPos.of(chunkPos, level.getMinSection());
        BlockPos minChunkPos = sectionPos.origin();

        net.minecraft.core.Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<Integer, List<Structure>> structuresPerStep = structureRegistry.stream()
                .collect(Collectors.groupingBy(structure -> structure.step().ordinal()));

        net.minecraft.core.Registry<net.minecraft.world.level.levelgen.placement.PlacedFeature> placedFeatures =
                level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        net.minecraft.world.level.levelgen.WorldgenRandom random = new net.minecraft.world.level.levelgen.WorldgenRandom(
                new net.minecraft.world.level.levelgen.XoroshiroRandomSource(net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed()));
        long decorationSeed = random.setDecorationSeed(level.getSeed(), minChunkPos.getX(), minChunkPos.getZ());

        int numSteps = GenerationStep.Decoration.values().length;
        try {
            for (int genStep = 0; genStep < numSteps; genStep++) {
                if (structureManager.shouldGenerateStructures()) {
                    List<Structure> structuresForStep = structuresPerStep.getOrDefault(genStep, Collections.emptyList());
                    int structureInStep = 0;
                    for (Structure structure : structuresForStep) {
                        random.setFeatureSeed(decorationSeed, structureInStep, genStep);
                        Supplier<String> nameSupplier = () -> structureRegistry.getResourceKey(structure)
                                .map(Object::toString).orElseGet(structure::toString);
                        try {
                            // 过滤废弃传送门
                            if (!isRuinedPortal(structure, structureRegistry)) {
                                level.setCurrentlyGenerating(nameSupplier);
                                structureManager.startsForStructure(sectionPos, structure).forEach(start -> {
                                    start.placeInChunk(level, structureManager, this, random, getWritableArea(chunk), chunkPos);
                                });
                            }
                        } catch (Exception e) {
                            CrashReport crashReport = CrashReport.forThrowable(e, "Feature placement");
                            crashReport.addCategory("Feature").setDetail("Description", nameSupplier::get);
                            throw new ReportedException(crashReport);
                        }
                        structureInStep++;
                    }
                }
                // 黑曜石柱(end_spike): 移到循环结束后统一处理, 避免 lambda 捕获非 final 变量
            }
            level.setCurrentlyGenerating(null);

            // 特殊放行: 黑曜石柱(end_spike)地物 (SURFACE_STRUCTURES 步骤)
            final int surfaceStepOrdinal = GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal();
            placedFeatures.holders().forEach(pfHolder -> {
                pfHolder.unwrapKey().ifPresent(key -> {
                    if (key.location().getPath().equals("end_spike")) {
                        random.setFeatureSeed(decorationSeed, 0, surfaceStepOrdinal);
                        try {
                            level.setCurrentlyGenerating(() -> key.location().toString());
                            pfHolder.value().placeWithBiomeCheck(level, this, random, minChunkPos);
                        } catch (Exception e) {
                            CrashReport crashReport = CrashReport.forThrowable(e, "Feature placement");
                            crashReport.addCategory("Feature").setDetail("Description", () -> key.location().toString());
                            throw new ReportedException(crashReport);
                        }
                    }
                });
            });
        } catch (Exception e) {
            CrashReport crashReport = CrashReport.forThrowable(e, "Biome decoration");
            crashReport.addCategory("Generation")
                    .setDetail("CenterX", chunkPos.x)
                    .setDetail("CenterZ", chunkPos.z)
                    .setDetail("Seed", decorationSeed);
            throw new ReportedException(crashReport);
        }
    }

    /** 判断结构是否为废弃传送门 */
    private boolean isRuinedPortal(Structure structure, net.minecraft.core.Registry<Structure> registry) {
        return registry.getResourceKey(structure)
                .map(key -> key.location().equals(new ResourceLocation("minecraft", "ruined_portal")))
                .orElse(false);
    }

    /** 复制自 ChunkGenerator.getWritableArea (私有方法) */
    private static BoundingBox getWritableArea(ChunkAccess chunk) {
        ChunkPos chunkpos = chunk.getPos();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();
        LevelHeightAccessor levelheightaccessor = chunk.getHeightAccessorForGeneration();
        int k = levelheightaccessor.getMinBuildHeight() + 1;
        int l = levelheightaccessor.getMaxBuildHeight() - 1;
        return new BoundingBox(i, k, j, i + 15, l, j + 15);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // 空岛无地表层(子类可重写放橡树)
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                            StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving) {
        // 空岛无洞穴
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor accessor) {
        return this.platformY;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor accessor, RandomState randomState) {
        if (shouldGeneratePlatform()) {
            int half = (platformSize - 1) / 2;
            if (x >= platformX - half && x <= platformX + half
                    && z >= platformZ - half && z <= platformZ + half) {
                return this.platformY + 1;
            }
        }
        // 返回 platformY(至少63) 作为"地表高度"
        // 原因: 空岛无地形, 若返回 minBuildHeight(-64), 结构会在 Y=-64 生成
        //       末地城要求 Y>=60, 主世界海平面63, 所以至少返回63
        return Math.max(this.platformY, 63);
    }

    /**
     * 获取"等效噪声世界"中的地形高度
     * 原理: 调用父类(NoiseBasedChunkGenerator)的 getBaseHeight, 计算若有地形时的高度
     *       用于末地龙池位置计算, 让龙池生成在"等效地形"高度而非虚空
     *       注意: 不能覆盖 getBaseColumn, 否则噪声列全空, getBaseHeight 返回 minBuildHeight
     * 参考: CarpetSkyAdditions SkyBlockChunkGenerator.getBaseHeightInEquivalentNoiseWorld
     */
    public int getBaseHeightInEquivalentNoiseWorld(int x, int z, Heightmap.Types heightmap, WorldGenLevel level) {
        RandomState randomState = RandomState.create(
                generatorSettings().value(),
                level.registryAccess().registryOrThrow(Registries.NOISE).asLookup(),
                level.getSeed());
        return super.getBaseHeight(x, z, heightmap, level, randomState);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    }
}
