package com.skyisland.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 空岛模组配置
 * 提供三个维度的平台位置与尺寸配置
 */
public class SkyIslandConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue OVERWORLD_PLATFORM_X;
    public static final ForgeConfigSpec.IntValue OVERWORLD_PLATFORM_Y;
    public static final ForgeConfigSpec.IntValue OVERWORLD_PLATFORM_Z;
    public static final ForgeConfigSpec.IntValue OVERWORLD_PLATFORM_SIZE;

    public static final ForgeConfigSpec.IntValue NETHER_PLATFORM_X;
    public static final ForgeConfigSpec.IntValue NETHER_PLATFORM_Y;
    public static final ForgeConfigSpec.IntValue NETHER_PLATFORM_Z;
    public static final ForgeConfigSpec.IntValue NETHER_PLATFORM_SIZE;

    // 末地平台由原版 ServerLevel.makeObsidianPlatform 处理 (100, 50, 0)，此处仅控制是否保留
    public static final ForgeConfigSpec.BooleanValue END_PRESERVE_OBSIDIAN_PLATFORM;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Sky Island Generator - 空岛生成器配置").push("general");

        builder.comment("主世界平台设置").push("overworld");
        OVERWORLD_PLATFORM_X = builder.comment("平台中心X坐标").defineInRange("platformX", 0, -30000000, 30000000);
        OVERWORLD_PLATFORM_Y = builder.comment("平台顶层Y坐标").defineInRange("platformY", 64, -64, 320);
        OVERWORLD_PLATFORM_Z = builder.comment("平台中心Z坐标").defineInRange("platformZ", 0, -30000000, 30000000);
        OVERWORLD_PLATFORM_SIZE = builder.comment("平台边长(方块数), 5=5x5").defineInRange("platformSize", 5, 1, 64);
        builder.pop();

        builder.comment("下界平台设置").push("nether");
        NETHER_PLATFORM_X = builder.comment("平台中心X坐标").defineInRange("platformX", 0, -30000000, 30000000);
        NETHER_PLATFORM_Y = builder.comment("平台顶层Y坐标").defineInRange("platformY", 64, -64, 320);
        NETHER_PLATFORM_Z = builder.comment("平台中心Z坐标").defineInRange("platformZ", 0, -30000000, 30000000);
        NETHER_PLATFORM_SIZE = builder.comment("平台边长(方块数)").defineInRange("platformSize", 5, 1, 64);
        builder.pop();

        builder.comment("末地平台设置 (原版黑曜石平台在 100,50,0)").push("end");
        END_PRESERVE_OBSIDIAN_PLATFORM = builder
                .comment("是否保留原版末地黑曜石平台 (玩家进入末地时自动生成)")
                .define("preserveObsidianPlatform", true);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    /** 工具方法: 根据size返回平台半边长(用于遍历) */
    public static int halfSize(int size) {
        return (size - 1) / 2;
    }
}
