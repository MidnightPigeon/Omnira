# 联动开发

面向Minecraft 1.21.1 / NeoForge。优先使用资源ID、标签、数据包配方与标准capability；不要求加载Omnira客户端类，不使用反射访问机器私有字段。以下是当前扩展契约，不承诺所有内部Java类跨版本二进制兼容。

## 配方与KubeJS

所有下列配方都通过标准RecipeManager加载，支持数据包覆盖、删除及重载。KubeJS不是必需依赖；安装时可用`ServerEvents.recipes`中的`event.custom`，无须额外Java插件。参考[KubeJS官方配方说明](https://kubejs.com/wiki/tutorials/recipes/)。

| 类型ID | 主要字段 |
| --- | --- |
| `omnira:condensation` | `mana_cost`、`result`；高阶输入另加`ingredient`、`count` |
| `omnira:analysis` | `ingredient`、`count`、`result`；可选副产物及原料耐久，见对应codec |
| `omnira:assembly` | 七个`slots`、`result`；可选`outer_shapeless`、`operation`与`return_remainders` |
| `omnira:advanced_assembly_table` | 九个`slots`、`result`；可选`operation` |
| `omnira:reaction` | `items`、`fluid`、`additional_fluids`、`support`、`results`、`guaranteed_results`、`fluid_result`、`duration` |
| `omnira:catalysis` | `substrate`、`seed`、`fluid`、`result`、`stages` |
| `omnira:starlight_conversion` | `ingredient`、`result`，整组一对一转化 |
| `omnira:vessel_conversion` | `ingredient`、`result` |
| `omnira:sword_shaping` | 四个`offerings`、`result`，沿用仪式规则 |
| `omnira:world_development` | `from`、`to`、`materials`、`duration`；可选`series`将多阶段及替代投入归为同一演化配方 |

世界演化的`series`为资源ID，省略时使用`omnira:myriad_confluence`。同系列步骤作为一项JEI配方，并共享初始提示框；不同系列的后续步骤不会混合匹配，最多三种可启动系列。系列显示名为`miniature.<namespace>.series.<path>`语言键（路径斜线替换为点）；原料及数量仍由每个步骤定义。此字段负责流程分组，不扩展现有场景、跨维度切换和最终产物机制；新增世界内容需另行实现对应场景与结果。

已实现系列为`omnira:myriad_confluence`和`omnira:world_probability`。后者使用WORKSHOP、DREAM_PORTAL、DREAM_EMPTY、DREAM_LAYERS、SPACETIME_EMPTY、SPACETIME_LAYERS、PROBABILITY_EXPLOSION场景；DREAM_PORTAL手动跨越，进入SPACETIME_EMPTY自动清空余料，PROBABILITY_EXPLOSION耗尽余料并在演出完成后交付概率云。终产物由终末场景决定，不能仅通过`series`字段替换；两系列均在取出终产物后重置。

制作结果使用1.21格式`{ id: 'minecraft:stick', count: 1 }`，不是旧版本的`item`字段；Ingredient仍使用`{ item: '...' }`或`{ tag: '...' }`。装配空位写`[]`，不能写`{}`。普通装配前六位为台面、第七位为中央；高阶前六位为台面，后三位为入口视角左、中、右。普通装配的结果数量限制与特殊操作保留既有规则，不将它视为任意批量合成器。

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
  // 删除用配方ID，不是配方类型ID：
  // event.remove({ id: 'omnira:reaction/example' });
});
```

示例仅供整合包作者自定义，不作为Omnira新配方发放。`duration`单位tick，反应默认2000tick；`results`为等概率择一，`guaranteed_results`为每次固定产出，最多四种物品及四种具体原液。`support`为催化方块的物品Ingredient，省略表示无限制。流体输入当前使用具体FluidStack（包含组件），**不支持将流体ID字段直接换成标签**；需要标签流体时为对应流体分别生成配方。催化室每次固定100mB，四次完成一轮（40秒/400mB）；催化展示统一四阶段，`stages`字段为旧数据兼容保留，不改变实际催化次数或展示阶段。固定配方无液体不进入自然生长模式。

天然作物、树木与矿物存在基于方块行为、标签和战利品的回退规则，不是全部都能用删除JSON配方关闭。催化室显式配方优先，原料及产出不绕过容量与自动化筛选。高级`operation`是既有专用行为，不是开放的脚本函数名。

高阶装配支持`operation: "grid_upgrade"`：六个台面原料无序匹配，三个节点按左/中/右（索引6/7/8）固定匹配，中间节点必须为单件水晶格栅，产物必须为容量更大的格栅。只保留该格栅的组件及法术库存，其他原料仍遵守非空容器限制；沿用普通装配的格栅升级实现。

已有机器正在执行的任务遵循各自保存与结算规则，重载不承诺立刻取消或替换进行中的任务。联动应查询当前RecipeManager而不是永久缓存RecipeHolder；自建缓存需在数据重载/服务器停止时失效。

## Java与自动化

- 注册类保留现有公开holder与ID；`omnira:advanced_forge`保留注册别名，新配方使用`omnira:advanced_assembly_table`。不批量移动既有公开类的包名。
- 使用NeoForge `Capabilities.ItemHandler.BLOCK`、`FluidHandler.BLOCK`和`EnergyStorage.BLOCK`，通过Level查询指定位置、指定面的能力。先模拟、后提交，遵守返回值及标准能力失效，不直接写机器库存/NBT。
- 无能力返回值表示该面不开放，不等于另一面也不可用。多方块输出、液体端口、升级筛选、共鸣输入有各自限制，不绕过它们强行访问控制器。
- FE是标准接口而非“电力模组存在”的证据。供能按机器接收上限，不能以一次无限量灌入替代每秒限额。
- 时空端口只代理远端相邻面，由原方块实际传输；没有主动电力搬运服务。联动不依赖白/黑端颜色推断电力流向。
- 配方记录中的ItemStack/FluidStack访问器仍视为只读模板；取得结果使用`assemble`/`getResultItem`，修改访问器返回值前自行复制。构造配方及结果展示已做防御性复制，但并未将所有Minecraft可变对象变成不可变对象。

## 属性与伤害接口

属性ID与公开holder维持 `registry.ModAttributes`；点数与百分比使用NeoForge原生格式，同类百分比基础乘区加算，不因物品来源追加独立倍率。

`combat.OmniraDamage.TRUE_DAMAGE` 表示普通真实伤害；`OVERDRAW` 是独立的透支死亡类型。`trueDamage(target, amount, originalSource)` 保留攻击者和范围语义并精确扣生命；`payHealth(target, amount, reason)` 支付自身生命，不足时走 `overdraw`，不能用图腾豁免。`execute` 是致死量的普通真伤，仍允许原版不死图腾复生。真实伤害不经过普通增减伤链，但会发送标准 `LivingDamageEvent.Post` 和死亡事件，记录实际生命损失、玩家归属及战斗统计。联动不应仅构造真实伤害来源后调用 `LivingEntity.hurt`，也不要自行 `setHealth`。普通伤害保留原版护甲流程；受法术强度影响的攻击先乘法强，再叠加全体伤害平面值，同类百分比在同一基础乘区加算。

## 可选依赖与客户端边界

终末世界法术的专项兼容入口为 `spell.world.WorldRotationHooks.registerMachine` 和 `registerEntity`。适配器只结算一次当前工作，保留原料/产出空间检查，不应启动无限循环或制造空原料产物；施法者可能为空。FE 能力会按所有面去重，标准 EnergyStorage 补满，自定义拒收或按 tick 限流的能源需要专项适配。实体分类可用 `world_magic_immune`、`reload_neutral`、`reload_hostile`、`reload_friendly` 标签扩展。已覆盖机器以 `WorldRotationHooks` 注册的适配器为准，没有通用反射强改未知机器计时器。

Create、Sable与Simulated的专属Mixin由`OptionalIntegrationMixins`在加载早期按模组ID启用；不存在时不解析第三方类型，存在时不静默跳过失效注入。基础机器能力独立于可选模组。JEI实现集中在`compat/jei`；渲染器、屏幕及音乐客户端类仅由客户端入口加载，不要在服务端联动入口调用它们。

Sable整机选取复用`BottleMultiblock`完整部件集合，微缩框架另按实际边长校验；静默移除标签仅用于原生快照搬运，不改变普通破坏行为。方块质量0不变，`ServerSubLevel.getMassTracker()`仅在总质量恰为0时提供0.001计算质量及有限质心/惯性，`getSelfMassTracker()`仍返回真实方块质量；正常混合结构不替换质量数据。时空端口NBT新增`PortAnchor`以修正同结构内的搬运偏移，不建立跨物理结构与普通世界的连接。

当前KubeJS支持范围为数据配方及既有标准能力，尚无专用`event.recipes.omnira.*`构建器或自定义脚本事件。不能把JSON兼容描述为所有Java反射访问均受支持。

## 回归入口

原生能源实现位于`pulse/`，`PulseNetworks.power(level,pos)`提供连接、停机与实际机器分配；`allocated`和`occupied`均为不含线损的工作量。`PulseTransport`统一结算最短链、分流、线性损耗与节点配额；无损和单源路径复用稀疏`PulseAllocation`，多源不同衰减使用压缩链的线性约束。两者不引用Minecraft或第三方类。Create转换仅位于`compat/create/PulseKinetics`；Jade由可选插件`compat/jade/PulseJadePlugin`发现，公共入口不解析Jade类型。两种晶球的方案1普通装配配方已注册，支持既有数据配方与KubeJS方式。

晶球未用配额用`PulseNetworks.available(orb)`，工作流量用`occupied(orb)`；蓝色配额不在相连晶球重复预留，串联节点的金色工作流量可包含同一股传输。连线信号用`input(wire)`，节点衰减用`loss(entity)`（连线为本格损耗，晶球为到达节点的上游链路损耗）；源侧实际原值占用用`sourceOccupied(level,pos,face)`。它们不是可提取电池储量。所有读数来自同一缓存结算，源只输出、机器只接收；不要再次单独回溯全网产能作为节点可用值。

```powershell
./scripts/gradle-local.ps1 runGameTestServer -PdreamTestData -PkubejsRecipeTest '-PgameTestNamespaces=omnira_recipe_integration,omnira_kubejs_recipes,omnira_recipe_audit' --offline
```

测试脚本仅复制到无界面开发实例，结束时清理，不写玩家整合包、不进入发布JAR。覆盖真实KubeJS自定义配方加载、标签及空槽匹配、已加载Omnira配方JSON/网络往返、结果模板保护。未安装第三方的隔离运行可另选`omnira_recipe_integration`及`-PwithoutExternalMods -PwithoutCreate -PwithoutSable -PwithoutJei`，不带KubeJS夹具开关。

普通装配的 `return_remainders` 默认为 `true`，沿用物品的容器/工具返还。显式设为 `false` 时消耗整份材料、不返还；原始法术核心的解析晶体装配采用此设置。

