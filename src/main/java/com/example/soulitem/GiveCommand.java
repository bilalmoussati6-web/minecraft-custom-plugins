package com.example.soulitem;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GiveCommand implements CommandExecutor {

    private final firepickaxePlugin plugin;

    public GiveCommand(firepickaxePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[0]);
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("§cUsage: /fire <player>");
            return true;
        }

        ItemStack item = plugin.getItemManager().createItem();
        target.getInventory().addItem(item);
        target.sendMessage("§aYou received the real fire_pickaxe custom item (NBT-stamped by the plugin)!");
        if (sender != target) {
            sender.sendMessage("§aGave the real fire_pickaxe to " + target.getName());
        }
        return true;
    }
}
