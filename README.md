此项目由DeepSeek v4.1构建

# ChannelCompat

**让 Forge 客户端像 Fabric 一样忽略"多余的模组网络通道"。**

在 Forge 上，客户端只要装了服务端没有的模组，联机时就会在握手阶段被拒绝：

```
无法连接至服务器
Connection closed - mismatched mod channel list
服务器缺少以下模组，请从您的客户端中移除这些模组以加入此服务器：
    MezzConfig            0.6.8
    What Are They Up To   1.20.1-1.2.3
    ...
```

而 Fabric 不会这样：通道不认识就直接忽略，多余的模组只是**不生效**，不会把人挡在门外。
本模组把 Forge 的行为改成和 Fabric 一致 —— **装多少客户端模组都不影响联机**。

## 原理

Forge 1.20.1 的握手流程（`net.minecraftforge.network`）里有两个关键点，本模组各挂一个 Mixin：

| 挂点 | Forge 原行为 | ChannelCompat 的行为 |
|---|---|---|
| `HandshakeHandler.handleServerModListOnClient` | 调用 `NetworkRegistry.validateClientChannels(服务端通道表)`，返回"客户端有、服务端不接受"的通道；**非空时客户端自己断开** | 顺手记录服务端通道表，然后返回空表 → **不再自我断开** |
| `HandshakeMessages$C2SModListReply.<init>` | 把客户端**全部**已注册通道塞进回复包发给服务端；服务端发现多出来的通道就拒绝 | 构造结束时把通道表替换为**双方都有且版本一致**的子集（`fml:*`、`forge:*`、`minecraft:*` 等 Forge 自身通道始终保留） |

结果：服务端**看不到任何差异**，握手正常完成；多余的模组通道在服务端不存在，自然不会生效。

## 特性

- ✅ **零配置**：不需要像同类工具那样一个个列出要隐藏的模组
- ✅ **自动取交集**：按服务端实际返回的通道表过滤，服务器换模组也不用改配置
- ✅ **纯客户端**：服务端不需要安装，也不要求别人安装
- ✅ **对单人/LAN 无影响**：没收到服务端通道表时不干预
- ✅ **失败安全**：mixin 配置为 `required: false`，目标不匹配时只记日志，不会崩游戏

## 限制（请务必了解）

| 情况 | 结果 |
|---|---|
| 客户端有、服务端没有的模组 | ✅ 正常进入，该模组不生效 |
| 两边都有但**版本不一致** | ✅ 能进（该通道会被隐藏），代价是这个模组的联机功能失效 |
| **服务端有、客户端没有**的模组 | ❌ 无效 —— 那是真的缺内容，只能装模组（Fabric 也一样） |
| 数据包 / 注册表（registry）校验 | ➖ 未改动，保留 Forge 原行为（避免真正的不一致导致崩溃） |
| 服务端主动硬踢 | ❌ 无法阻止（Forge 默认是"通知客户端自己断开"，所以本模组才拦得住） |

> ⚠️ 隐藏通道的意义是"让不兼容的模组安静地不生效"，**不是**绕过服务端的反作弊。请遵守服务器规则。

## 安装

1. 需要 **Minecraft 1.20.1 + Forge 47.x**
2. 把 `channelcompat-1.0.0.jar` 放进客户端 `.minecraft/mods/`
3. 启动游戏即可，无需配置

日志里出现下面两行说明在工作：

```
[ChannelCompat] 已加载：联机时将忽略服务端不存在的模组通道（Fabric 式行为）
[ChannelCompat] 已记录服务端通道 N 个
[ChannelCompat] 不宣告通道 xxx（本地版本 1，服务端 无此模组）
```

## 构建

需要 JDK 17+ 与本地 Minecraft/Forge 安装（提供依赖 jar 路径即可）：

```powershell
# 用法： .\build.ps1 -MinecraftDir "D:\path\to\.minecraft" -ForgeVersion "1.20.1-47.4.26"
.\build.ps1
```

脚本会从 `.minecraft/libraries` 里自动取 `mixin`、`forge-universal`、`javafmllanguage`、`slf4j-api`，编译并打包到 `build/channelcompat-1.0.0.jar`。

## 许可

MIT License，详见 [LICENSE](LICENSE)。