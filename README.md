# Sky Island Generator

[English](./README_EN.md) | 中文

一个 Minecraft 1.20.1 Forge 模组，添加"空岛"世界类型。创建世界时选择该类型后，三个维度均会生成小型起始平台，周围是虚空，同时保留原版特殊建筑（末地传送门、古城传送门、刷怪笼等）和生物群系的正常生成。

## 功能特性

### 主世界
- 在 (8, 64, 8) 处生成 5×5×1 草方块平台
- 平台中心种植一棵橡树
- 出生点固定为平台中心正上方 (8, 65, 8)
- 保留所有原版结构生成（要塞、末地传送门、古城传送门、刷怪笼等）
- 过滤废弃传送门
- 多生物群系正常分布

### 下界
- 在 (8, 64, 8) 处生成 5×5×3 下界岩平台（顶层+2层支撑）
- 下界要塞仅生成烈焰人刷怪笼房间（MonsterThrone）
- 堡垒遗迹保持原版生成不变
- 多生物群系正常分布

### 末地
- 保留原版黑曜石平台（玩家进入末地时由原版自动生成）
- 龙池（出口传送门）位置调整至"等效地形"高度，避免生成在虚空中
- 黑曜石柱（末地水晶柱）正常生成
- 末地城正常生成
- 击杀末影龙后折跃门可正常生成

## 技术原理

### 核心生成器架构
模组核心是 `AbstractSkyIslandChunkGenerator`，继承自 `NoiseBasedChunkGenerator`：

- **继承原因**：`ChunkMap.createRandomState` 检查 `instanceof NoiseBasedChunkGenerator`，只有继承此类才能正确创建 `RandomState`，使 `MultiNoiseBiomeSource` 正常工作，保证多生物群系分布。
- **覆写策略**：
  - `fillFromNoise`：不调用 `super`，只放置平台方块，避免生成噪声地形
  - `buildSurface` / `applyCarvers`：空实现，无地表层和洞穴
  - `applyBiomeDecoration`：重写，只生成结构方块，过滤废弃传送门，特殊放行黑曜石柱
  - `getBaseHeight`：返回 `platformY`（至少 63），避免结构生成在 Y=-64
  - `createState`：不重写，使用原版方法保证末地城等结构正常计算

### 龙池位置修复
通过 Mixin 注入 `EndDragonFight.spawnExitPortal`，在原版 heightmap 查找前设置 `portalLocation`：

- 调用 `getBaseHeightInEquivalentNoiseWorld` 计算"若有地形时"的高度
- 让龙池生成在等效地形高度，而非虚空中的 Y=0
- 参考 CarpetSkyAdditions 的实现思路

### 下界要塞修改
通过 Mixin 拦截 `NetherFortressStructure.findGenerationPoint`，替换原版多组件生成逻辑，只生成包含烈焰人刷怪笼的 `MonsterThrone` 房间。

### 世界类型注册
通过数据驱动的 `world_preset` JSON 文件定义空岛世界预设，并追加到原版 `normal` 与 `extended` 世界类型标签，无需修改创建世界界面代码。

### 出生点设置
监听 `LevelEvent.CreateSpawnPosition` 事件，当主世界使用空岛生成器时，强制设置出生点为平台中心并取消默认搜索。

## Mixin 注意事项

由于 Forge 1.20.1 项目未自动生成 refmap，Mixin 在生产环境（SRG mappings）和开发环境（Mojang mappings）之间需要手动处理映射：

- `@Inject` 的 `method` 同时指定 Mojang 名和 SRG 名
- `@Shadow` 使用 `aliases` 属性指定 SRG 名
- 设置 `remap = false` 禁用自动映射

## 平台坐标配置

平台坐标由 `sky_island.json` 世界预设文件控制，**每个存档独立保存**：

- 世界创建时，生成器参数通过 Codec 序列化到存档的 `level.dat` 文件
- 之后加载世界时直接从 `level.dat` 反序列化，不会重新读取 JSON
- 修改 JSON 只影响**之后新建的世界**，已创建的世界不受影响

如需修改已存在世界的平台位置，需用 NBT 编辑器修改 `level.dat` 中的 `Data⟶WorldGenSettings⟶dimensions⟶minecraft:overworld⟶generator` 下的 `platform_x/y/z` 字段。

## 安装

1. 安装 Minecraft 1.20.1
2. 安装 Forge 47.x.x
3. 将 `skyisland-1.0.0.jar` 放入 `.minecraft/mods` 文件夹
4. 启动游戏，创建新世界时选择"Sky Island"世界类型

## 开发环境

```bash
# 克隆仓库
git clone https://github.com/19136644525lxy/Sky-Island-Generator.git
cd Sky-Island-Generator

# 构建项目
./gradlew build

# 启动测试客户端
./gradlew runClient
```

**环境要求**：
- JDK 17
- Gradle 8.8+

## 项目结构

```
src/main/java/com/skyisland/
├── SkyIslandMod.java                              # 模组主类
├── core/
│   ├── registry/
│   │   └── SkyRegistries.java                     # 注册表
│   └── worldgen/
│       ├── AbstractSkyIslandChunkGenerator.java   # 生成器基类
│       ├── OverworldSkyIslandGenerator.java        # 主世界生成器
│       ├── NetherSkyIslandGenerator.java           # 下界生成器
│       └── EndSkyIslandGenerator.java             # 末地生成器
└── mixin/
    ├── MixinEndDragonFight.java                   # 龙池位置修复
    └── MixinNetherFortressStructure.java           # 下界要塞修改
```

## 致谢

- [CarpetSkyAdditions](https://github.com/jsorrell/CarpetSkyAdditions) — 参考其空岛生成器实现思路

## 许可证

本项目采用 [MIT License](./LICENSE) 许可证。

## 作者

- Yifei
- ZhangXaohan
