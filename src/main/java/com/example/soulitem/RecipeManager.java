package com.example.soulitem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeManager {

    private final firepickaxePlugin plugin;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public RecipeManager(firepickaxePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        try {
        NamespacedKey key = new NamespacedKey(plugin, "fire_pickaxe_recipe");
        FurnaceRecipe recipe = new FurnaceRecipe(key, plugin.getItemManager().createItem(),
                Material.matchMaterial("OBSIDIAN"), 5.7f, 101);
        Bukkit.addRecipe(recipe);
        registeredKeys.add(key);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register recipe: " + t.getMessage());
        }
    }

    public void unregisterRecipes() {
        for (NamespacedKey k : registeredKeys) {
            try { Bukkit.removeRecipe(k); } catch (Throwable ignored) {}
        }
        registeredKeys.clear();
    }
}
