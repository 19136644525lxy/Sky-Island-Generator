package com.skyisland.core.registry;

import com.skyisland.SkyIslandMod;
import com.skyisland.core.worldgen.EndSkyIslandGenerator;
import com.skyisland.core.worldgen.NetherSkyIslandGenerator;
import com.skyisland.core.worldgen.OverworldSkyIslandGenerator;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;

/**
 * 模组注册表
 *
 * 注册 ChunkGenerator 的 Codec 到 BuiltInRegistries.CHUNK_GENERATOR
 * 原理: 1.20.1 中 ChunkGenerator Codec 通过 Registry.register 直接注册
 *        Forge 的 RegisterEvent 用于触发注册时机
 */
public class SkyRegistries {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SkyRegistries::registerCodecs);
    }

    /**
     * 注册三个维度的 ChunkGenerator Codec
     * 注册名: skyisland:overworld_sky_island, skyisland:nether_sky_island, skyisland:end_sky_island
     */
    private static void registerCodecs(RegisterEvent event) {
        registerCodec("overworld_sky_island", OverworldSkyIslandGenerator.CODEC);
        registerCodec("nether_sky_island", NetherSkyIslandGenerator.CODEC);
        registerCodec("end_sky_island", EndSkyIslandGenerator.CODEC);
    }

    /**
     * 通用 Codec 注册方法
     * 显式指定泛型参数避免编译器无法推断类型
     */
    private static <T> void registerCodec(String name, Codec<T> codec) {
        ResourceLocation location = new ResourceLocation(SkyIslandMod.MOD_ID, name);
        ResourceKey<Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> key =
            ResourceKey.create(Registries.CHUNK_GENERATOR, location);
        Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR,
            key,
            (Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator>) codec
        );
    }
}
