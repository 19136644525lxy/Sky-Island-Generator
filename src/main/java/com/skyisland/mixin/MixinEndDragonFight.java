package com.skyisland.mixin;

import com.skyisland.core.worldgen.AbstractSkyIslandChunkGenerator;
import com.skyisland.SkyIslandMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Mixin: 修改末地龙池(出口传送门)位置
 *
 * 原理: 参考 CarpetSkyAdditions 的 EnderDragonFightMixin
 *       原版 EndDragonFight.spawnExitPortal 在 portalLocation==null 时
 *       调用 getHeightmapPos(MOTION_BLOCKING_NO_LEAVES, getLocation(origin)).below()
 *       空岛末地无地形, heightmap 返回 minBuildHeight(0), 龙池生成在虚空 (0,-1,0)
 *
 * 修复: 注入 spawnExitPortal HEAD, 若 portalLocation 为 null
 *       调用 getBaseHeightInEquivalentNoiseWorld 获取"等效噪声地形"高度
 *       即计算若有地形时该位置的高度, 让龙池生成在合理高度
 *       不强制固定 Y 值, 由末地噪声地形自然决定
 */
@Mixin(EndDragonFight.class)
public class MixinEndDragonFight {

    // aliases 指定 SRG 名, 生产环境无 refmap 时通过别名定位字段
    @Shadow(aliases = {"f_64061_"})
    @Final
    private ServerLevel level;

    @Shadow(aliases = {"f_64072_"})
    private @Nullable BlockPos portalLocation;

    /**
     * 注入 spawnExitPortal 方法 HEAD: 在原版 heightmap 查找前设置 portalLocation
     * 原理: 原版只在 portalLocation==null 时查找, 我们提前设置后原版查找被跳过
     *       getBaseHeightInEquivalentNoiseWorld 调用父类计算真实噪声地形高度
     *
     * 注意: remap=false 并同时指定 Mojang名和SRG名
     *       原因: Forge 1.20.1 生产环境用 SRG mappings(方法名混淆为 m_64093_)
     *       开发环境用 Mojang mappings(方法名 spawnExitPortal)
     *       因项目未生成 refmap, 需手动指定两个名字确保两种环境都能注入
     */
    @Inject(method = {"spawnExitPortal", "m_64093_"}, at = @At("HEAD"), remap = false)
    private void skyIsland$setExitPortalLocation(boolean previouslyKilled, CallbackInfo ci) {
        // 只对空岛生成器生效
        if (level.getChunkSource().getGenerator() instanceof AbstractSkyIslandChunkGenerator chunkGenerator) {
            if (portalLocation == null) {
                // 获取等效噪声地形高度, -1 因为龙池放在地表下一格
                int y = chunkGenerator.getBaseHeightInEquivalentNoiseWorld(
                        0, 0, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level) - 1;
                SkyIslandMod.LOGGER.debug("[SkyIsland] End dragon pool set to Y={}", y);
                portalLocation = BlockPos.ZERO.atY(y);
            }
        }
    }
}

