package org.xymy.title;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Title extends JavaPlugin {

    private GMHook gmHook;
    // 匹配 &0-9a-f 或 &#RRGGBB
    private final Pattern colorPattern = Pattern.compile("(?i)&([0-9a-fk-or]|#[0-9a-f]{6})");

    @Override
    public void onEnable() {
        this.gmHook = new GMHook(this);
        if (getCommand("title") != null) {
            getCommand("title").setExecutor(this);
        }
        getLogger().info("Title 插件已启动！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该指令只能由玩家执行。");
            return true;
        }
        Player player = (Player) sender;

        // --- 处理 1 个参数的指令: get, num, clear ---
        if (args.length == 1) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "get":
                    String prefix = gmHook.getPrefix(player);
                    if (prefix == null || prefix.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "你当前没有任何称号。");
                    } else {
                        player.sendMessage(ChatColor.AQUA + "你的当前称号是: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', prefix));
                    }
                    return true;

                case "num":
                    int maxLen = getMaxAllowedLength(player);
                    player.sendMessage(ChatColor.GREEN + "你当前允许设置的最大称号长度为: " + ChatColor.YELLOW + maxLen);
                    return true;

                case "clear":
                    // 执行删除指令: manudelv <玩家> prefix
                    String clearCmd = String.format("manudelv %s prefix", player.getName());
                    getServer().dispatchCommand(getServer().getConsoleSender(), clearCmd);
                    player.sendMessage(ChatColor.YELLOW + "已成功清除你的称号。");
                    return true;
            }
        }

        // --- 逻辑: /title set <称号> ---
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String titleInput = args[1];
            if (!titleInput.matches("[\\u4e00-\\u9fa5]+")) {
                player.sendMessage(ChatColor.RED + "设置失败：称号必须全部为汉字！");
                return true;
            }
            if (!hasLengthPermission(player, titleInput.length())) {
                player.sendMessage(ChatColor.RED + "设置失败：字数上限不足。");
                return true;
            }

            String formattedTitle = formatChineseTitle(titleInput);
            updatePrefix(player, "&f『" + formattedTitle + "&f』");
            player.sendMessage(ChatColor.GREEN + "称号设置成功！");
            return true;
        }

        // --- 逻辑: /title color <位置> <颜色代码> ---
        if (args.length == 3 && args[0].equalsIgnoreCase("color")) {
            String prefix = gmHook.getPrefix(player);
            // 严谨判断：必须包含『且前缀不为空
            if (prefix == null || !prefix.contains("『")) {
                player.sendMessage(ChatColor.RED + "你还没有设置称号，请先使用 /title set 设置。");
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
            // 增加对 &#RRGGBB 和 &c 的格式校验
            if (!colorCode.matches("(?i)&[0-9a-f]") && !colorCode.matches("(?i)&#[0-9a-f]{6}")) {
                player.sendMessage(ChatColor.RED + "颜色格式错误！示例: &6 或 &#FF0000");
                return true;
            }

            // 提取纯汉字部分
            String pureTitle = prefix.substring(prefix.indexOf("『") + 1, prefix.lastIndexOf("』"))
                    .replaceAll("(?i)&#[0-9a-f]{6}|&[0-9a-fA-Fk-orK-ORxX]", "");

            if (index < 0 || index >= pureTitle.length()) {
                player.sendMessage(ChatColor.RED + "位置超出范围！当前称号长度: " + pureTitle.length());
                return true;
            }

            StringBuilder newFormatted = new StringBuilder();
            char[] chars = pureTitle.toCharArray();

            // 提取颜色代码数组
            String[] colors = getExistingColor(prefix);

            for (int i = 0; i < chars.length; i++) {
                if (i == index) {
                    newFormatted.append(colorCode).append(chars[i]);
                } else {
                    newFormatted.append(colors[i]).append(chars[i]);
                }
            }

            updatePrefix(player, "&f『" + newFormatted.toString() + "&f』");
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

    private void updatePrefix(Player player, String finalPrefix) {
        String cmd = String.format("manuaddv %s prefix %s", player.getName(), finalPrefix);
        getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
    }

    private String[] getExistingColor(String prefix) {
        String content = prefix.substring(prefix.indexOf("『") + 1, prefix.lastIndexOf("』"));
        String[] colors = content.split("\\p{IsHan}");
        for (int i = 0; i < colors.length; i++) {
            String color = colors[i];
            if (color == null || color.isEmpty()) {
                colors[i] = "&f";
            }
        }
        return colors;
    }

    private int getMaxAllowedLength(Player player) {
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("title.num." + i)) return i;
        }
        return 0;
    }

    private boolean hasLengthPermission(Player player, int length) {
        return getMaxAllowedLength(player) >= length;
    }

    private String formatChineseTitle(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append("&a").append(c);
        }
        return sb.toString();
    }
}