package org.xymy.title;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LPHook {

    private final LuckPerms lp;

    public LPHook() {
        // 直接获取 LP 实例
        this.lp = LuckPermsProvider.get();
    }

    public String getPrefix(Player player) {
        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user == null) return null;
        return user.getCachedData().getMetaData().getPrefix();
    }

    public void setPrefix(Player player, String prefix) {
        lp.getUserManager().modifyUser(player.getUniqueId(), user -> {
            // 先移除旧的所有前缀（如果你只想保留这一个称号）
            user.data().clear(NodeType.PREFIX::matches);

            // 设置新的前缀，优先级可以设高一点，比如 100
            PrefixNode node = PrefixNode.builder(prefix, 100).build();
            user.data().add(node);
        });
    }

    public void clearPrefix(Player player) {
        lp.getUserManager().modifyUser(player.getUniqueId(), user -> {
            user.data().clear(NodeType.PREFIX::matches);
        });
    }

    public int getMaxAllowedLength(Player player) {
        // LuckPerms 推荐直接使用权限检查，不需要遍历 1-100
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("title.num." + i)) {
                return i;
            }
        }
        return 0;
    }
}