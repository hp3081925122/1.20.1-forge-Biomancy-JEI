package com.biomancy_jei.jei;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// 保存单个掉落物及其一页可获取该掉落物的生物刷怪蛋。
public record DespoilRecipe(Identifier id, ItemStack drop, List<ItemStack> targets) {
    public DespoilRecipe {
        targets = List.copyOf(targets);
    }
}
