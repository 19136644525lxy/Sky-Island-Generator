package com.skyisland;

import com.mojang.logging.LogUtils;
import com.skyisland.config.SkyIslandConfig;
import com.skyisland.core.registry.SkyRegistries;
import com.skyisland.core.worldgen.AbstractSkyIslandChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * 空岛生成器主类
 * 负责模组初始化、注册表挂载、配置加载、出生点事件
 */
@Mod(SkyIslandMod.MOD_ID)
public class SkyIslandMod {
    public static final String MOD_ID = "skyisland";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SkyIslandMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        SkyRegistries.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, SkyIslandConfig.SPEC, "skyisland-common.toml");
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Sky Island Generator mod initialized");
    }

    /**
     * 主世界加载时强制设置出生点
     * 原理: LevelEvent.CreateSpawnPosition 事件在出生点生成前触发
     *       若主世界使用空岛生成器, 将出生点设为平台中心并取消默认搜索
     *       避免原版出生点随机搜索(空岛无方块时会乱找)
     */
    @SubscribeEvent
    public void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        LevelAccessor level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) return;
        if (!(serverLevel.getChunkSource().getGenerator() instanceof AbstractSkyIslandChunkGenerator)) return;

        // 出生点 = 平台中心正上方 (8, 64+1, 8)
        BlockPos spawnPos = new BlockPos(8, 65, 8);
        serverLevel.setDefaultSpawnPos(spawnPos, 0.0F);
        event.setCanceled(true);
    }
}
