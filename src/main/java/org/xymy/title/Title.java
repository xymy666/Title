package org.xymy.title;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Title extends JavaPlugin implements CommandExecutor {

    private LPHook lpHook;

    @Override
    public void onEnable() {
        // 确保 LuckPerms 已安装
        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().severe("未找到 LuckPerms，插件已禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.lpHook = new LPHook();
        if (getCommand("title") != null) {
            getCommand("title").setExecutor(this);
        }
        getLogger().info("Title 插件 (LuckPerms版) 已启动！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该指令只能由玩家执行。");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 1) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "get":
                    String prefix = lpHook.getPrefix(player);
                    if (prefix == null || prefix.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "你当前没有任何称号。");
                    } else {
                        player.sendMessage(formatText("&b你的当前称号是: &f").append(formatText(prefix)));
                    }
                    return true;

                case "num":
                    int maxLen = lpHook.getMaxAllowedLength(player);
                    player.sendMessage(ChatColor.GREEN + "你当前允许设置的最大称号长度为: " + ChatColor.YELLOW + maxLen);
                    return true;

                case "clear":
                    lpHook.clearPrefix(player);
                    player.sendMessage(ChatColor.YELLOW + "已成功清除你的称号。");
                    return true;
            }
        }

        // /title set <称号>
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String titleInput = args[1];
            if (!titleInput.matches("[\\u4e00-\\u9fa5]+")) {
                player.sendMessage(ChatColor.RED + "设置失败：称号必须全部为汉字！");
                return true;
            }
            if (titleInput.length() > lpHook.getMaxAllowedLength(player)) {
                player.sendMessage(ChatColor.RED + "设置失败：字数上限不足。");
                return true;
            }

            String formattedTitle = formatChineseTitle(titleInput);
            lpHook.setPrefix(player, "&f『" + formattedTitle + "&f』");
            player.sendMessage(ChatColor.GREEN + "称号设置成功！");
            return true;
        }

        // /title color <位置> <颜色代码>
        if (args.length == 3 && args[0].equalsIgnoreCase("color")) {
            String prefix = lpHook.getPrefix(player);
            if (prefix == null || !prefix.contains("『")) {
                player.sendMessage(ChatColor.RED + "你还没有设置称号。");
                return true;
            }

            int index;
            try {
                index = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "位置必须是数字。");
                return true;
            }

            String colorCode = args[2];
            if (!colorCode.matches("(?i)&[0-9a-f]") && !colorCode.matches("(?i)&#[0-9a-f]{6}")) {
                player.sendMessage(ChatColor.RED + "颜色格式错误！示例: &6 或 &#FF0000");
                return true;
            }

            // 提取纯汉字部分
            String pureTitle = prefix.substring(prefix.indexOf("『") + 1, prefix.lastIndexOf("』"))
                    .replaceAll("(?i)&#[0-9a-f]{6}|&[0-9a-fA-Fk-orK-ORxX]", "");

            if (index < 0 || index >= pureTitle.length()) {
                player.sendMessage(ChatColor.RED + "位置超出范围！");
                return true;
            }

            StringBuilder newFormatted = new StringBuilder();
            char[] chars = pureTitle.toCharArray();
            String[] colors = getExistingColors(prefix, chars.length);

            for (int i = 0; i < chars.length; i++) {
                if (i == index) {
                    newFormatted.append(colorCode).append(chars[i]);
                } else {
                    newFormatted.append(colors[i]).append(chars[i]);
                }
            }

            lpHook.setPrefix(player, "&f『" + newFormatted.toString() + "&f』");
            player.sendMessage(ChatColor.GREEN + "颜色修改成功！");
            return true;
        }

        // 帮助信息
        sender.sendMessage(ChatColor.GRAY + "====== " + ChatColor.AQUA + "Title 帮助" + ChatColor.GRAY + " ======");
        sender.sendMessage(ChatColor.WHITE + "/title get " + ChatColor.GRAY + "- 查看当前称号");
        sender.sendMessage(ChatColor.WHITE + "/title num " + ChatColor.GRAY + "- 查看字数限制");
        sender.sendMessage(ChatColor.WHITE + "/title clear " + ChatColor.GRAY + "- 清除当前称号");
        sender.sendMessage(ChatColor.WHITE + "/title set <称号> " + ChatColor.GRAY + "- 设置纯汉字称号");
        sender.sendMessage(ChatColor.WHITE + "/title color <位置> <颜色代码(例如&a或者&#8EFFFF)> " + ChatColor.GRAY + "- 修改指定字颜色");
        return true;
    }

    // 优化的颜色解析逻辑
    private String[] getExistingColors(String prefix, int length) {
        String[] colors = new String[length];
        java.util.Arrays.fill(colors, "&a");

        String content = prefix.substring(prefix.indexOf("『") + 1, prefix.lastIndexOf("』"));
        // 简单的正则表达式拆分，寻找颜色代码后紧跟的汉字
        // 修正后的正则：添加了 A-F
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(&[0-9a-fA-F]|&#[0-9a-fA-F]{6})([\\p{IsHan}])").matcher(content);

        int i = 0;
        while (matcher.find() && i < length) {
            colors[i] = matcher.group(1);
            i++;
        }
        return colors;
    }

    private String formatChineseTitle(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append("&a").append(c);
        }
        return sb.toString();
    }

    private Component formatText(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}