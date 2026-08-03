package com.skyisland.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Mixin: 下界要塞只生成烈焰人刷怪笼房间(MonsterThrone)
 *
 * 原理: 拦截 findGenerationPoint, 替换原版的多组件生成逻辑
 *       只添加一个 MonsterThrone 房间(包含烈焰人刷怪笼)
 *       保留堡垒遗迹(Bastion)生成不变(由 JigsawStructure 独立处理)
 */
@Mixin(NetherFortressStructure.class)
public class MixinNetherFortressStructure {

    /**
     * 替换下界要塞生成: 只生成 MonsterThrone 房间
     * MonsterThrone 房间尺寸 7x8x9, 包含烈焰人刷怪笼
     *
     * 注意: remap=false 并同时指定 Mojang名和SRG名
     *       原因: Forge 1.20.1 生产环境用 SRG mappings(方法名混淆为 m_214086_)
     *       开发环境用 Mojang mappings(方法名 findGenerationPoint)
     *       因项目未生成 refmap, 需手动指定两个名字确保两种环境都能注入
     */
    @Inject(method = {"findGenerationPoint", "m_214086_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onlyBlazeSpawnerRoom(Structure.GenerationContext context,
                                       CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        ChunkPos chunkpos = context.chunkPos();
        BlockPos blockpos = new BlockPos(chunkpos.getMinBlockX(), 64, chunkpos.getMinBlockZ());

        cir.setReturnValue(Optional.of(new Structure.GenerationStub(blockpos, (builder) -> {
            // 创建 MonsterThrone 房间
            RandomSource random = context.random();
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            // BoundingBox.orientBox 参数: (minX, minY, minZ, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, direction)
            // MonsterThrone 房间尺寸 7x8x9, 使用 orientBox 创建包围盒
            BoundingBox box = BoundingBox.orientBox(
                chunkpos.getMinBlockX() + 2, 64, chunkpos.getMinBlockZ() + 2,
                -2, 0, 0, 7, 8, 9, direction
            );
            NetherFortressPieces.MonsterThrone throne =
                new NetherFortressPieces.MonsterThrone(0, box, direction);
            builder.addPiece(throne);
        })));
    }
}
