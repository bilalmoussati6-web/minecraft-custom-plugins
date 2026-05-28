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

    private final SoulItemPlugin plugin;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public RecipeManager(SoulItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        try {
        NamespacedKey key = new NamespacedKey(plugin, "soul_blade_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, plugin.getItemManager().createItem());
        recipe.shape("AA ", "AB ", " C ");
        recipe.setIngredient('A', Material.matchMaterial("DIAMOND"));
        recipe.setIngredient('B', Material.matchMaterial("BLAZE_ROD"));
        recipe.setIngredient('C', Material.matchMaterial("STICK"));
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
