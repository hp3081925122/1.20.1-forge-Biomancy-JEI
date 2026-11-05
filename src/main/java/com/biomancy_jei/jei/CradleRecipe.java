package com.biomancy_jei.jei;

import com.biomancy_jei.BiomancyJei;
import com.github.elenterius.biomancy.api.tribute.Tribute;
import com.github.elenterius.biomancy.api.tribute.Tributes;
import com.github.elenterius.biomancy.crafting.recipe.PotionSerumRecipes;
import com.github.elenterius.biomancy.item.PotionSerumItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.Holder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 保存原初摇篮的方块物品、可能生成的生物和全部有效投喂物品。
public record CradleRecipe(Identifier id, ItemStack cradle, List<CradleMob> mobs, List<CradleTribute> tributes) {
    private static final Identifier CRADLE_ID = Identifier.fromNamespaceAndPath("biomancy", "primordial_cradle");
    private static final List<CradleMobDefinition> MOB_DEFINITIONS = List.of(
            new CradleMobDefinition("flesh_blob", MobCondition.FLESH),
            new CradleMobDefinition("hungry_flesh_blob", MobCondition.HUNGRY),
            new CradleMobDefinition("primordial_flesh_blob", MobCondition.PRIMORDIAL),
            new CradleMobDefinition("primordial_hungry_flesh_blob", MobCondition.PRIMORDIAL_HUNGRY),
            new CradleMobDefinition("legacy_flesh_blob", MobCondition.LEGACY)
    );

    public CradleRecipe {
        cradle = cradle.copy();
        mobs = List.copyOf(mobs);
        tributes = List.copyOf(tributes);
    }

    // 从当前 Biomancy 版本的 Tribute API 构建原初摇篮页面数据。
    public static CradleRecipe create() {
        Item cradleItem = BuiltInRegistries.ITEM.getValue(CRADLE_ID);
        List<CradleMob> mobs = new ArrayList<>();
        for (CradleMobDefinition definition : MOB_DEFINITIONS) {
            Identifier entityId = Identifier.fromNamespaceAndPath("biomancy", definition.entityPath());
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
            if (entityType == null) {
                continue;
            }
            Item spawnEgg = SpawnEggItem.byId(entityType).map(Holder::value).orElse(null);
            if (spawnEgg != null) {
                mobs.add(new CradleMob(spawnEgg.getDefaultInstance(), definition.condition()));
            }
        }

        List<CradleTribute> tributes = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            addTribute(tributes, item.getDefaultInstance());
        }
        for (Holder.Reference<Potion> potion : BuiltInRegistries.POTION.listElements().toList()) {
            // 为三种药水容器分别保留效果 NBT，展示其真实的摇篮数值。
            addTribute(tributes, PotionContents.createItemStack(Items.POTION, potion));
            addTribute(tributes, PotionContents.createItemStack(Items.SPLASH_POTION, potion));
            addTribute(tributes, PotionContents.createItemStack(Items.LINGERING_POTION, potion));
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof PotionSerumItem serum) {
                // 药水血清只展示 Biomancy 配方实际可制作的药水变体。
                for (Holder<Potion> potion : PotionSerumRecipes.getPotions(Minecraft.getInstance().level)) {
                    addTribute(tributes, serum.getInstanceFrom(potion));
                }
            }
        }
        tributes.sort(Comparator
                .comparing((CradleTribute entry) -> BuiltInRegistries.ITEM.getKey(entry.item().getItem()).toString())
                .thenComparing(entry -> entry.item().getComponentsPatch().toString()));

        Identifier recipeId = Identifier.fromNamespaceAndPath(BiomancyJei.MOD_ID, "primordial_cradle");
        return new CradleRecipe(recipeId, cradleItem == null ? ItemStack.EMPTY : cradleItem.getDefaultInstance(), mobs, tributes);
    }

    // 使用 Biomancy 的原始判定 API 过滤并保存单个有效投喂物品堆栈。
    private static void addTribute(List<CradleTribute> tributes, ItemStack stack) {
        try {
            Tribute tribute = Tributes.getTribute(stack);
            if (tribute == Tributes.WRONG_ITEM || tribute.isEmpty()) {
                return;
            }
            tributes.add(new CradleTribute(
                    stack,
                    tribute.biomass(),
                    tribute.lifeEnergy(),
                    tribute.successModifier(),
                    tribute.diseaseModifier(),
                    tribute.hostileModifier(),
                    tribute.anomalyModifier()
            ));
        } catch (RuntimeException ignored) {
            // 某些第三方物品可能在默认堆栈上读取额外数据，不能阻止 JEI 注册其他物品。
        }
    }

    // 描述刷怪蛋对应的随机判定分支。
    public record CradleMob(ItemStack egg, MobCondition condition) {
        public CradleMob {
            egg = egg.copy();
        }
    }

    // 保存单个投喂物品对六项摇篮数值的实际修正。
    public record CradleTribute(ItemStack item, int biomass, int lifeEnergy, int success, int disease, int hostile, int anomaly) {
        public CradleTribute {
            item = item.copy();
        }
    }

    // 枚举成功后会进入的生物生成分支。
    public enum MobCondition {
        FLESH("jei.biomancy_jei.cradle.condition.flesh"),
        HUNGRY("jei.biomancy_jei.cradle.condition.hungry"),
        PRIMORDIAL("jei.biomancy_jei.cradle.condition.primordial"),
        PRIMORDIAL_HUNGRY("jei.biomancy_jei.cradle.condition.primordial_hungry"),
        LEGACY("jei.biomancy_jei.cradle.condition.legacy");

        private final String translationKey;

        MobCondition(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    private record CradleMobDefinition(String entityPath, MobCondition condition) {
    }
}
