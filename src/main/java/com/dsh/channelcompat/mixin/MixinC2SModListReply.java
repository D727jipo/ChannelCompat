package com.dsh.channelcompat.mixin;

import com.dsh.channelcompat.ChannelFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * 挂点 2：客户端回复给服务端的模组/通道列表。
 *
 * Forge 原逻辑：C2SModListReply 构造时用 NetworkRegistry.buildChannelVersions()
 *   把「本客户端注册的全部通道」存进 channels 字段，然后原样发给服务端；
 *   服务端发现多出来的通道就拒绝连接（"mismatched mod channel list"）。
 *
 * 这里改为：构造完成时把 channels 字段替换成「双方都有且版本一致」的子集
 * （Forge 自身通道始终保留）。服务端因此看不到任何差异 ——
 * 多余的模组只是不生效，正是 Fabric 的行为。
 */
@Mixin(targets = "net.minecraftforge.network.HandshakeMessages$C2SModListReply")
public class MixinC2SModListReply {

    @Shadow
    private Map channels;

    @Inject(method = "<init>()V", at = @At("RETURN"), require = 0)
    private void channelcompat$filterOwnChannels(CallbackInfo ci) {
        Map filtered = ChannelFilter.filterOwnChannels(this.channels);
        if (filtered != null) {
            this.channels = filtered;
        }
    }
}
