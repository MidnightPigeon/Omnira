# 联动开发

适用 Minecraft 1.21.1 / NeoForge。优先使用资源 ID、标签、数据配方和标准能力接口；内部 Java 实现不保证跨版本兼容。

## 配方与KubeJS

配方通过 RecipeManager 加载，支持数据包覆盖和重载。KubeJS 为可选依赖，可使用 `ServerEvents.recipes` 与 `event.custom`，目前没有专用配方构建器。

| 类型 ID | 主要字段 |
| --- | --- |
| `omnira:condensation` | `mana_cost`、`result`；高阶输入另加 `ingredient`、`count` |
| `omnira:analysis` | `ingredient`、`count`、`result`；支持副产物与原料耐久 |
| `omnira:assembly` | 七个 `slots`、`result`；可选 `outer_shapeless`、`operation`、`return_remainders` |
| `omnira:advanced_assembly_table` | 九个 `slots`、`result`；可选 `operation` |
| `omnira:reaction` | `items`、`fluid`、`additional_fluids`、`support`、`results`、`guaranteed_results`、`fluid_result`、`duration` |
| `omnira:catalysis` | `substrate`、`seed`、`fluid`、`result` |
| `omnira:starlight_conversion` | `ingredient`、`result`，整组一对一转化 |
| `omnira:vessel_conversion` | `ingredient`、`result` |
| `omnira:sword_shaping` | 四个 `offerings`、`result` |
| `omnira:world_development` | `from`、`to`、`materials`、`duration`；可选 `series` |

完整格式和字段范围以 [现有配方](src/main/resources/data/omnira/recipe) 为准。结果使用 `{ id: 'minecraft:stick', count: 1 }`；Ingredient 使用 `item` 或 `tag`，装配空槽使用 `[]`。

```js
// kubejs/server_scripts/omnira.js
ServerEvents.recipes(event => {
  event.custom({
    type: 'omnira:reaction',
    items: [{ ingredient: { tag: 'minecraft:planks' }, count: 1 }],
    fluid: { id: 'minecraft:water', amount: 1000 },
    results: [{ id: 'minecraft:stick', count: 1 }],
    duration: 2000
  }).id('your_pack:water_and_planks');
});
```

这是自定义示例，不是模组自带配方。删除配方时使用配方 ID，而非类型 ID。

### 必要规则

- **普通装配**：前六槽为外围，第七槽为中央。`return_remainders` 默认 `true`；设为 `false` 时整份消耗，不返还容器或工具。结果数量与特殊操作仍受设备限制。
- **高阶装配**：前六槽为台面，后三槽依次为入口视角的左、中、右。`operation: "grid_upgrade"` 的台面无序，中间节点须为单件格栅，产物须为更大容量格栅；保留其组件与法术库存，其他材料不能夹带内容物。`operation` 只能选择已实现的行为。
- **反应**：`duration` 单位为 tick，默认2000；`results` 等概率择一，`guaranteed_results` 固定产出。最多四种物品原料和四种具体原液。`support` 是催化方块的物品 Ingredient，省略时不限。流体输入是含组件的 FluidStack，不直接接受流体标签；需为各流体分别生成配方。
- **催化**：每阶段消耗100 mB，固定四阶段完成一轮，基础共40秒、400 mB。显式配方优先，无液体时不会转入自然生长。作物、树木与矿物另有行为和标签回退，删除 JSON 不一定禁用它们。
- **世界演化**：`series` 将多阶段及替代材料归为同一流程和 JEI 配方，默认 `omnira:myriad_confluence`。现有系列另有 `omnira:world_probability`、`omnira:myriad_codex`，最多三种可启动系列。名称键为 `miniature.<namespace>.series.<path>`（路径斜线改为点）。该字段仅分组，不能自行增加场景或改变终产物。
- **重载**：进行中的任务按各机器规则结算，不保证立即替换。配方缓存须在数据重载和服务器停止时失效；ItemStack、FluidStack 模板修改前先复制。

## Java与自动化

通过 Level 查询指定面上的 `Capabilities.ItemHandler.BLOCK`、`FluidHandler.BLOCK`、`EnergyStorage.BLOCK`，遵循模拟、提交、返回值和能力失效机制。没有能力表示该面不开放，不能绕过升级筛选、端口方向或多方块控制器限制。

FE 按输入速率折算 MPU，遵守接收限额与持续供能规则。时空端口仅代理远端相邻面，不主动搬运电力，也不以黑白颜色判断电力方向。

能源读数统一从 `PulseNetworks` 获取，不自行重复扫描网络：

| 入口 | 含义 |
| --- | --- |
| `power(level, pos)` | 网络供能与机器分配 |
| `available(orb)` / `occupied(orb)` | 晶球可用配额 / 工作占用 |
| `input(wire)` / `loss(entity)` | 连线输入 / 节点损耗 |
| `sourceOccupied(level, pos, face)` | 源侧实际占用 |

这些数值不是电池储量。`allocated`、`occupied` 不含线损；串联节点可显示同一股工作流量，不能相加当作额外消耗。

## 属性与伤害接口

属性入口为 `registry.ModAttributes`。同类百分比在同一基础乘区加算；受法术强度影响的伤害先乘法强，再加平面伤害。

- `OmniraDamage.trueDamage(target, amount, originalSource)`：绕过普通减伤并保留来源、伤害后事件与原版死亡结算。
- `execute`：致死量普通真伤，允许不死图腾复生。
- `payHealth`：支付自身生命，不足时触发独立的 `overdraw` 死亡，图腾不能豁免。

真伤请调用上述入口，不要仅构造伤害来源后调用 `hurt`，也不要直接 `setHealth`。

## 可选依赖与客户端边界

- **Create / Sable / Simulated**：专属 Mixin 由 `OptionalIntegrationMixins` 按安装情况启用。基础能力不依赖这些模组。
- **物理化结构**：整机搬运使用 `BottleMultiblock` 的完整部件；时空端口只保留同结构内的绑定。零质量结构的计算兜底不改变方块真实质量。
- **世界轮转**：通过 `WorldRotationHooks.registerMachine` / `registerEntity` 适配；每次结算一轮，保留原料与空间检查，施法者可能为空。拒收或限流的自定义能源需要专项适配。
- **重载分类**：可通过 `world_magic_immune`、`reload_neutral`、`reload_hostile`、`reload_friendly` 标签扩展。
- **客户端**：JEI、Jade、渲染、界面和音乐由各自入口加载；服务端不得引用客户端类，可选模组缺失时不得解析其类型。

## Lootr — 个人战利品

仅安装 Lootr 时，战利品水晶球与液晶球启用个人战利品及个人开启外观；沙子不参与。此类晶球不接受升级或远程自动化，界面内手动取放与液体容器操作仍可用。正常潜行破坏掉落空晶球，踩碎不掉落内容物；任何破坏方式都不释放个人库存。

## 回归入口

```powershell
./gradlew.bat runGameTestServer -PdreamTestData -PkubejsRecipeTest '-PgameTestNamespaces=omnira_recipe_integration,omnira_kubejs_recipes,omnira_recipe_audit'
```

测试仅使用无界面开发实例，不修改玩家整合包或打包测试数据。最小依赖测试可使用 `omnira_recipe_integration`，加上 `-PwithoutExternalMods -PwithoutCreate -PwithoutSable -PwithoutJei`，不启用 KubeJS 夹具。

## 已废弃内容

以下仅供识别旧数据，新联动使用正文中的当前接口。

| 旧用法 | 当前用法 |
| --- | --- |
| `omnira:advanced_forge` | 使用 `omnira:advanced_assembly_table`；旧 ID 已不再解析。 |
| 催化配方的 `stages` 字段 | 不再填写；催化固定四阶段，旧字段已移除。 |
