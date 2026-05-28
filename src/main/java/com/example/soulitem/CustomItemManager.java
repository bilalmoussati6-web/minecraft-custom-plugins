package com.example.soulitem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the real custom item and identifies it via PersistentDataContainer.
 *
 * HOW THIS IS A REAL CUSTOM ITEM (not a texture pack):
 * - Every item created by createItem() has a PersistentDataContainer tag:
 *     key = plugin's NamespacedKey, value = "fire_pickaxe"
 * - isCustomItem() checks for this NBT tag. If the tag is present, it's OUR
 *   custom item — not a regular NETHERITE_PICKAXE.
 * - Without the NBT tag, a NETHERITE_PICKAXE is just vanilla. With the tag,
 *   the plugin treats it as "fire_pickaxe" and fires all abilities.
 * - CustomModelData is OPTIONAL — it only changes the visual texture when
 *   paired with the resource pack in resourcepack/. The plugin works 100%
 *   without any resource pack (item will just look like NETHERITE_PICKAXE).
 *
 * Uses reflective lookups for Attribute and Enchantment names so the same
 * plugin works across multiple Spigot/Paper versions where enum names change.
 */
public class CustomItemManager {

    private final firepickaxePlugin plugin;

    public CustomItemManager(firepickaxePlugin plugin) {
        this.plugin = plugin;
    }

    // The base64-encoded Mojang texture value for the custom head texture.
    // When non-empty, the item becomes a PLAYER_HEAD with this unique texture.
    // Obtained from mineskin.org or minecraft-heads.com.
    private static final String CUSTOM_HEAD_TEXTURE_VALUE = "";

    public ItemStack createItem() {
        ItemStack item;
        ItemMeta meta;

        if (!CUSTOM_HEAD_TEXTURE_VALUE.isEmpty()) {
            // === CUSTOM HEAD TEXTURE MODE ===
            // This item becomes a PLAYER_HEAD with a unique custom skin texture
            // baked into the NBT via PlayerProfile/ProfileProperty API.
            // The texture is unique per-item — it won't look like any vanilla item,
            // even without a resource pack.
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta == null) {
                return new ItemStack(Material.STONE);
            }
            applyCustomHeadTexture(skullMeta, CUSTOM_HEAD_TEXTURE_VALUE);
            meta = skullMeta;
        } else {
            // === BASE ITEM MODE ===
            // The base material is the visual fallback. The REAL identity of
            // this item is the PersistentDataContainer tag added below.
            Material base = matchMaterial("NETHERITE_PICKAXE", Material.DIAMOND_SWORD);
            item = new ItemStack(base);
            meta = item.getItemMeta();
            if (meta == null) return item;
        }

        meta.setDisplayName("fire pickaxe");

        List<String> loreList = new ArrayList<>();
        loreList.add("smelt ores");
        if (!loreList.isEmpty()) meta.setLore(loreList);

        // Custom model data is OPTIONAL — only used by the resource pack
        // to swap the vanilla NETHERITE_PICKAXE texture with a custom one.
        // The plugin works 100% without it (item will look like NETHERITE_PICKAXE).
        try { meta.setCustomModelData(1001); } catch (Throwable ignored) {}

        meta.setUnbreakable(true);
        
        

        // Attributes
        addAttr(meta, "GENERIC_ATTACK_DAMAGE", 10.000d, "HAND");
        addAttr(meta, "GENERIC_ATTACK_SPEED", -2.400d, "HAND");

        // Enchantments
        // no enchantments

        // THIS IS WHAT MAKES IT A REAL CUSTOM ITEM:
        // We stamp it with a plugin-owned PersistentDataContainer tag. This tag
        // is the item's true identity — it's what the plugin uses to recognize
        // "fire_pickaxe" among all the other NETHERITE_PICKAXEs in the world.
        // Without this tag, a NETHERITE_PICKAXE is just vanilla. With it, it's
        // our custom item with abilities, attributes, and recipes.
        meta.getPersistentDataContainer().set(plugin.getItemKey(), PersistentDataType.STRING, "fire_pickaxe");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns true if this ItemStack is the real "fire_pickaxe" custom item.
     * The check uses the PersistentDataContainer NBT tag, NOT CustomModelData
     * or display name. This is what makes this a real plugin, not a texture pack.
     */
    public boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        // Check for the plugin's custom NBT tag.
        String v = meta.getPersistentDataContainer().get(plugin.getItemKey(), PersistentDataType.STRING);
        return "fire_pickaxe".equals(v);
    }

    // ---- helpers ----

    private static Material matchMaterial(String name, Material fallback) {
        Material m = Material.matchMaterial(name);
        return m != null ? m : fallback;
    }

    private static void addAttr(ItemMeta meta, String attrName, double amount, String slotName) {
        try {
            Attribute attr;
            try {
                attr = Attribute.valueOf(attrName);
            } catch (IllegalArgumentException ex) {
                // 1.21+ uses unprefixed names (ATTACK_DAMAGE) or registry keys.
                String stripped = attrName.replace("GENERIC_", "");
                try {
                    attr = Attribute.valueOf(stripped);
                } catch (IllegalArgumentException ex2) {
                    return;
                }
            }
            EquipmentSlot slot;
            try { slot = EquipmentSlot.valueOf(slotName); } catch (Throwable t) { slot = EquipmentSlot.HAND; }
            AttributeModifier mod;
            try {
                // Newer constructor (1.21+) takes NamespacedKey.
                NamespacedKey key = new NamespacedKey(firepickaxePlugin.getInstance(), attrName.toLowerCase());
                mod = AttributeModifier.class
                        .getConstructor(NamespacedKey.class, double.class, AttributeModifier.Operation.class, EquipmentSlot.class)
                        .newInstance(key, amount, AttributeModifier.Operation.ADD_NUMBER, slot);
            } catch (Throwable t) {
                // Legacy constructor with UUID.
                mod = new AttributeModifier(UUID.randomUUID(), attrName.toLowerCase(), amount,
                        AttributeModifier.Operation.ADD_NUMBER, slot);
            }
            meta.addAttributeModifier(attr, mod);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("Could not add attribute " + attrName + ": " + t.getMessage());
        }
    }

    private static void addEnchant(ItemMeta meta, String keyName, int level) {
        Enchantment ench = null;
        try {
            NamespacedKey k = NamespacedKey.minecraft(keyName);
            ench = Enchantment.getByKey(k);
        } catch (Throwable ignored) {}
        if (ench == null) {
            try { ench = Enchantment.getByName(keyName.toUpperCase()); } catch (Throwable ignored) {}
        }
        if (ench != null) {
            try { meta.addEnchant(ench, level, true); } catch (Throwable ignored) {}
        }
    }

    /**
     * Applies a custom skin texture to a player head (SkullMeta) using the
     * texture value from Mojang (base64-encoded JSON containing the texture URL).
     *
     * This is what makes the item TRULY custom — without a resource pack, the
     * item shows as a player head with a unique custom skin texture, not as any
     * vanilla item.
     *
     * FIX: Uses 100% reflection for all code paths to ensure the plugin
     * compiles successfully even when building against older Spigot APIs.
     */
    private static void applyCustomHeadTexture(SkullMeta skullMeta, String base64TextureValue) {
        if (base64TextureValue == null || base64TextureValue.isEmpty()) return;
        try {
            // --- Modern path (1.20.5+): PlayerProfile API via Reflection ---
            try {
                java.util.UUID uuid = java.util.UUID.randomUUID();
                // profile = Bukkit.createProfile(uuid, "custom_item")
                java.lang.reflect.Method createProfile = Bukkit.class.getMethod("createProfile", java.util.UUID.class, String.class);
                Object profile = createProfile.invoke(null, uuid, "custom_item");

                // prop = new ProfileProperty("textures", value)
                Class<?> propClass = Class.forName("org.bukkit.profile.ProfileProperty");
                Object prop = propClass.getConstructor(String.class, String.class).newInstance("textures", base64TextureValue);

                // profile.setProperty(prop)
                java.lang.reflect.Method setProperty = profile.getClass().getMethod("setProperty", propClass);
                setProperty.invoke(profile, prop);

                // skullMeta.setPlayerProfile(profile)
                java.lang.reflect.Method setPlayerProfile = skullMeta.getClass().getMethod("setPlayerProfile", Class.forName("org.bukkit.profile.PlayerProfile"));
                setPlayerProfile.invoke(skullMeta, profile);
                return;
            } catch (Throwable ignored) {}

            // --- 1.18-1.20.4: SkullMeta.setProfile(GameProfile) ---
            try {
                java.lang.reflect.Method setProfileMethod = skullMeta.getClass().getDeclaredMethod("setProfile", Class.forName("com.mojang.authlib.GameProfile"));
                setProfileMethod.setAccessible(true);
                Object profile = makeGameProfile(base64TextureValue);
                setProfileMethod.invoke(skullMeta, profile);
                return;
            } catch (Throwable ignored) {}

            // --- 1.13-1.17: reflection on SkullMeta.profile field ---
            try {
                java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                Object profile = makeGameProfile(base64TextureValue);
                profileField.set(skullMeta, profile);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    /**
     * Creates a GameProfile with a custom texture property.
     * Uses reflection to avoid compile-time dependency on Mojang authlib.
     */
    private static Object makeGameProfile(String base64TextureValue) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            java.util.UUID uuid = java.util.UUID.randomUUID();
            Object profile = gameProfileClass.getConstructor(java.util.UUID.class, String.class)
                    .newInstance(uuid, "custom_item");
            // Get the properties map
            java.lang.reflect.Method getProperties = gameProfileClass.getMethod("getProperties");
            Object properties = getProperties.invoke(profile);
            // properties.put("textures", new Property("textures", base64Value))
            java.lang.reflect.Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", base64TextureValue);
            putMethod.invoke(properties, "textures", property);
            return profile;
        } catch (Throwable t) {
            return null;
        }
    }
}
