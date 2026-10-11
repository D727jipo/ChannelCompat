package com.dsh.channelcompat.mixin;

import com.dsh.channelcompat.ChannelFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/**
 * 挂点 1：客户端收到服务端模组列表后的校验。
 *
 * Forge 原逻辑：
 *   NetworkRegistry.validateClientChannels(serverChannels) 返回「客户端有、服务端不接受」的通道表，
 *   非空时客户端会自己断开并显示 "Connection closed - mismatched mod channel list"。
 *
 * 这里改为：
 *   - 顺手把服务端的通道表记下来（给挂点 2 用）；
 *   - 返回空表 → 客户端不再自我断开。
 */
@Mixin(targets = "net.minecraftforge.network.HandshakeHandler")
public class MixinHandshakeHandler {

    @Redirect(
            method = "handleServerModListOnClient",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/network/NetworkRegistry;validateClientChannels(Ljava/util/Map;)Ljava/util/Map;"
            ),
            require = 0
    )
    private Map channelcompat$ignoreClientSideMismatch(Map serverChannels) {
        ChannelFilter.rememberServerChannels(serverChannels);
        // 空表 = 没有任何不匹配 → 不会触发客户端的自我断开
        return new HashMap();
    }
}
