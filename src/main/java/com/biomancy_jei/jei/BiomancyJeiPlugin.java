package com.biomancy_jei.jei;

import com.biomancy_jei.BiomancyJei;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.neoforged.fml.ModList;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// 注册 JEI 分类并读取 Biomancy 自带的血肉镰刀掉落表。
@JeiPlugin
public final class BiomancyJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(BiomancyJei.MOD_ID, "plugin");
    private static final Identifier SICKLE_ID = Identifier.fromNamespaceAndPath("biomancy", "despoil_sickle");
    private static final Identifier DESPOIL_ENCHANTMENT_ID = Identifier.fromNamespaceAndPath("biomancy", "despoil");
    private static volatile List<DespoilRecipe> sharedRecipes;
    private static volatile CradleRecipe cradleRecipe;

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new DespoilRecipeCategory(registration.getJeiHelpers().getGuiHelper(), getSickleStack(), getDespoilBook()));
        registration.addRecipeCategories(new CradleRecipeCategory(registration.getJeiHelpers().getGuiHelper(), getCradleStack()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(DespoilRecipeCategory.RECIPE_TYPE, getSickleStack(), getDespoilBook());
        registration.addRecipeCatalysts(CradleRecipeCategory.RECIPE_TYPE, getCradleStack());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(DespoilRecipeCategory.RECIPE_TYPE, getSharedRecipes());
        registration.addRecipes(CradleRecipeCategory.RECIPE_TYPE, List.of(getCradleRecipe()));
    }

    // 读取并缓存 Biomancy 的血肉镰刀战利品表，供 JEI 与 EMI 共用。
    public static List<DespoilRecipe> getSharedRecipes() {
        List<DespoilRecipe> cached = sharedRecipes;
        if (cached != null) {
            return cached;
        }
        synchronized (BiomancyJeiPlugin.class) {
            cached = sharedRecipes;
            if (cached == null) {
                cached = loadRecipes();
                sharedRecipes = cached;
            }
            return cached;
        }
    }

    // 按掉落物聚合所有可获取它的生物刷怪蛋，每种掉落物只生成一条配方。
    private static List<DespoilRecipe> loadRecipes() {
        var contents = ModList.get().getModFileById("biomancy").getFile().getContents();
        List<String> paths = new ArrayList<>();
        contents.visitContent("data/", (name, resource) -> {
            if (name.endsWith(".json")) {
                paths.add(name);
            }
        });
        Map<Item, LinkedHashSet<Item>> targetsByDrop = new LinkedHashMap<>();
        for (String path : paths.stream().sorted().toList()) {
            String[] relativePath = path.substring("data/".length()).split("/");
            if (relativePath.length < 5 || !relativePath[1].equals("loot_table")
                    || !relativePath[2].equals("biomancy") || !relativePath[3].equals("despoil")) {
                continue;
            }
            String entityPath = String.join("/", java.util.Arrays.copyOfRange(relativePath, 4, relativePath.length));
            Identifier entityId = Identifier.fromNamespaceAndPath(relativePath[0], entityPath.substring(0, entityPath.length() - ".json".length()));
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
            Item spawnEgg = entityType == null ? null : SpawnEggItem.byId(entityType).map(net.minecraft.core.Holder::value).orElse(null);
            if (spawnEgg == null) {
                continue;
            }
            Set<Item> drops = new LinkedHashSet<>();
            try (Reader reader = contents.get(path).bufferedReader(StandardCharsets.UTF_8)) {
                JsonObject lootTable = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray pools = lootTable.getAsJsonArray("pools");
                if (pools == null) {
                    continue;
                }
                for (JsonElement poolElement : pools) {
                    if (!poolElement.isJsonObject()) {
                        continue;
                    }
                    JsonArray entries = poolElement.getAsJsonObject().getAsJsonArray("entries");
                    if (entries == null) {
                        continue;
                    }
                    for (JsonElement entry : entries) {
                        collectDrops(entry, drops);
                    }
                }
            } catch (IOException | IllegalStateException exception) {
                continue;
            }
            for (Item drop : drops) {
                targetsByDrop.computeIfAbsent(drop, ignored -> new LinkedHashSet<>()).add(spawnEgg);
            }
        }

        List<DespoilRecipe> recipes = new ArrayList<>();
        for (Map.Entry<Item, LinkedHashSet<Item>> entry : targetsByDrop.entrySet()) {
            Identifier dropId = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (dropId == null) {
                continue;
            }
            List<ItemStack> targetStacks = entry.getValue().stream().map(Item::getDefaultInstance).toList();
            Identifier recipeId = Identifier.fromNamespaceAndPath(
                    BiomancyJei.MOD_ID,
                    "despoil/" + dropId.getNamespace() + "/" + dropId.getPath()
            );
            recipes.add(new DespoilRecipe(recipeId, entry.getKey().getDefaultInstance(), targetStacks));
        }
        return List.copyOf(recipes);
    }

    // 递归读取战利品池中的物品与复合条目。
    private static void collectDrops(JsonElement entry, Set<Item> drops) {
        if (!entry.isJsonObject()) {
            return;
        }

        JsonObject entryObject = entry.getAsJsonObject();
        JsonElement itemName = entryObject.get("name");
        if (itemName != null && itemName.isJsonPrimitive() && "minecraft:item".equals(entryObject.get("type").getAsString())) {
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName.getAsString())).orElse(null);
            if (item != null) {
                drops.add(item);
            }
        }

        JsonArray children = entryObject.getAsJsonArray("children");
        if (children != null) {
            for (JsonElement child : children) {
                collectDrops(child, drops);
            }
        }
    }

    // 统一取得作为 JEI 催化剂和输入的血肉镰刀。
    public static ItemStack getSickleStack() {
        return BuiltInRegistries.ITEM.getValue(SICKLE_ID).getDefaultInstance();
    }

    // 创建带有 Biomancy 剥取附魔的附魔书，用于表达镰刀之外的获取方式。
    public static ItemStack getDespoilBook() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            throw new IllegalStateException("Despoil enchantment requires client registry access");
        }
        var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, DESPOIL_ENCHANTMENT_ID));
        ItemStack book = Items.ENCHANTED_BOOK.getDefaultInstance();
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(enchantment, 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        return book;
    }

    // 统一取得用于 JEI 和 EMI 页面入口的原初摇篮方块物品。
    public static ItemStack getCradleStack() {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("biomancy", "primordial_cradle"));
        return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    // 构建并缓存原初摇篮的动态投喂数据，避免 JEI 和 EMI 分别扫描注册表。
    public static CradleRecipe getCradleRecipe() {
        CradleRecipe cached = cradleRecipe;
        if (cached != null) {
            return cached;
        }
        synchronized (BiomancyJeiPlugin.class) {
            cached = cradleRecipe;
            if (cached == null) {
                cached = CradleRecipe.create();
                cradleRecipe = cached;
            }
            return cached;
        }
    }
}
