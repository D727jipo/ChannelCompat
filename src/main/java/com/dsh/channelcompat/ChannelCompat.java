package com.dsh.channelcompat;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChannelCompat —— 让 Forge 客户端像 Fabric 一样忽略多余的模组网络通道。
 *
 * 现象（原版 Forge 行为）：客户端装了服务端没有的模组时，握手阶段会报
 *   "Connection closed - mismatched mod channel list"
 * 并列出“服务器缺少以下模组”，直接拒绝进入。
 *
 * 本模组做两件事（两个 Mixin 挂点）：
 *   1. 收到服务端的通道表后，客户端不再因为“自己有、服务端没有”而自我断开；
 *   2. 回复给服务端的通道表里，只保留双方都有且版本一致的通道（Forge 自身通道除外）。
 * 于是服务端检测不到任何差异 —— 多余的模组只是不生效，和 Fabric 行为一致。
 *
 * 纯客户端模组，服务端不需要安装。
 */
@Mod(ChannelCompat.MODID)
public class ChannelCompat {

    public static final String MODID = "channelcompat";
    public static final Logger LOGGER = LoggerFactory.getLogger("ChannelCompat");

    public ChannelCompat() {
        LOGGER.info("[ChannelCompat] 已加载：联机时将忽略服务端不存在的模组通道（Fabric 式行为）");
    }
}
