package com.dsh.channelcompat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通道过滤：让 Forge 客户端表现得像 Fabric ——
 * 只向服务器宣告「双方都有且版本相同」的网络通道，多余的模组通道直接不广告。
 * 这样服务端不会检测到差异，也就不会拒绝连接。
 */
public final class ChannelFilter {

    /** 服务端在 S2CModList 里告知的通道表：id -> 版本（未收到时为 null） */
    private static volatile Map<String, String> serverChannels = null;

    private ChannelFilter() {
    }

    /** 收到服务端的通道表时调用（handleServerModListOnClient 里） */
    public static void rememberServerChannels(Map<?, ?> channels) {
        Map<String, String> map = new LinkedHashMap<>();
        if (channels != null) {
            for (Map.Entry<?, ?> e : channels.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                map.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        serverChannels = map;
        ChannelCompat.LOGGER.info("[ChannelCompat] 已记录服务端通道 {} 个", map.size());
    }

    /** Forge 自己的通道永远保留（避免破坏握手本身） */
    private static boolean isForgeOwn(String id) {
        return id.startsWith("fml:") || id.startsWith("fml|") || id.startsWith("minecraft:")
                || id.startsWith("forge:") || id.equals("fml:handshake") || id.startsWith("fml:");
    }

    /**
     * 过滤客户端自己的通道表：
     * 保留 fml/forge/minecraft 自己的通道 + 服务端也有且版本相同的通道。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map filterOwnChannels(Map<?, ?> own) {
        Map<String, String> srv = serverChannels;
        if (own == null) {
            return null;
        }
        if (srv == null) {
            // 还没收到服务端列表（例如本地/单人），不干预
            return (Map) own;
        }
        Map out = new LinkedHashMap();
        int skipped = 0;
        for (Map.Entry<?, ?> e : own.entrySet()) {
            Object key = e.getKey();
            Object val = e.getValue();
            if (key == null) {
                continue;
            }
            String id = String.valueOf(key);
            if (isForgeOwn(id)) {
                out.put(key, val);
                continue;
            }
            String srvVer = srv.get(id);
            if (srvVer != null && srvVer.equals(String.valueOf(val))) {
                out.put(key, val);
            } else {
                skipped++;
                ChannelCompat.LOGGER.info("[ChannelCompat] 不宣告通道 {}（本地版本 {}，服务端 {}）",
                        id, val, srvVer == null ? "无此模组" : srvVer);
            }
        }
        if (skipped > 0) {
            ChannelCompat.LOGGER.info("[ChannelCompat] 共隐藏 {} 个多余/版本不符的通道，连接不会被拒绝", skipped);
        }
        return out;
    }
}
