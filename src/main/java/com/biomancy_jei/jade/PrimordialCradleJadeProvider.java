package com.biomancy_jei.jade;

import com.github.elenterius.biomancy.block.cradle.PrimordialCradleBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public final class PrimordialCradleJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("biomancy_jei", "primordial_cradle");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        // 从服务端方块实体读取原初摇篮的实时数值，避免客户端自行推断。
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof PrimordialCradleBlockEntity cradle)) {
            return;
        }
        data.putFloat("biomass", cradle.getBiomassPct() * 100.0F);
        data.putFloat("life_energy", cradle.getLifeEnergyPct() * 100.0F);
        data.putFloat("success", cradle.getSuccessChance() * 100.0F);
        data.putFloat("hostile", cradle.getHostileChance() * 100.0F);
        data.putFloat("anomaly", cradle.getAnomalyChance() * 100.0F);
        data.putFloat("disease", cradle.getDiseaseChance() * 100.0F);
        data.putInt("primal_energy", cradle.getPrimalEnergy());
        data.putBoolean("active", cradle.isFull());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // 将服务端同步的数值按 Jade 行提示展示给玩家。
        CompoundTag data = accessor.getServerData();
        if (data.isEmpty()) {
            return;
        }
        if (!accessor.showDetails()) {
            tooltip.add(Component.translatable(data.getBoolean("active")
                    ? "jade.biomancy_jei.active"
                    : "jade.biomancy_jei.inactive"));
            tooltip.add(Component.translatable("jade.biomancy_jei.view_jei_emi"));
            tooltip.add(Component.translatable("jade.biomancy_jei.hold_shift"));
            return;
        }
        tooltip.add(Component.translatable("jade.biomancy_jei.biomass", formatValue(data.getFloat("biomass"))));
        tooltip.add(Component.translatable("jade.biomancy_jei.life_energy", formatValue(data.getFloat("life_energy"))));
        tooltip.add(Component.translatable("jade.biomancy_jei.success", formatPercent(data.getFloat("success"))));
        tooltip.add(Component.translatable("jade.biomancy_jei.hostile", formatPercent(data.getFloat("hostile"))));
        tooltip.add(Component.translatable("jade.biomancy_jei.anomaly", formatPercent(data.getFloat("anomaly"))));
        tooltip.add(Component.translatable("jade.biomancy_jei.disease", formatPercent(data.getFloat("disease"))));
        tooltip.add(Component.translatable("jade.biomancy_jei.primal_energy", data.getInt("primal_energy")));
        addMobChanceTooltip(tooltip, data);
    }

    // 根据 Biomancy 原初摇篮的实际分支判定计算五种肉团的当前出现概率。
    private static void addMobChanceTooltip(ITooltip tooltip, CompoundTag data) {
        float successChance = clampChance(data.getFloat("success") / 100.0F);
        float hostileChance = clampChance(data.getFloat("hostile") / 100.0F);
        float anomalyChance = clampChance(data.getFloat("anomaly") / 100.0F);
        float fleshChance;
        float hungryChance;
        float legacyChance;
        if (data.getFloat("hostile") < -420.0F) {
            fleshChance = 0.0F;
            hungryChance = 0.0F;
            legacyChance = successChance * (1.0F - anomalyChance);
        } else {
            fleshChance = successChance * (1.0F - anomalyChance) * (1.0F - hostileChance);
            hungryChance = successChance * (1.0F - anomalyChance) * hostileChance;
            legacyChance = 0.0F;
        }
        float primordialChance = successChance * anomalyChance * (1.0F - hostileChance);
        float primordialHungryChance = successChance * anomalyChance * hostileChance;
        tooltip.add(Component.translatable("jade.biomancy_jei.mob_chances").withStyle(ChatFormatting.YELLOW));
        // 保持五种肉团概率使用默认颜色，仅突出显示概率分组标题。
        tooltip.add(Component.translatable("jade.biomancy_jei.flesh_blob", formatPercent(fleshChance * 100.0F)));
        tooltip.add(Component.translatable("jade.biomancy_jei.hungry_flesh_blob", formatPercent(hungryChance * 100.0F)));
        tooltip.add(Component.translatable("jade.biomancy_jei.primordial_flesh_blob", formatPercent(primordialChance * 100.0F)));
        tooltip.add(Component.translatable("jade.biomancy_jei.primordial_hungry_flesh_blob", formatPercent(primordialHungryChance * 100.0F)));
        tooltip.add(Component.translatable("jade.biomancy_jei.legacy_flesh_blob", formatPercent(legacyChance * 100.0F)));
    }

    // 将可能超出随机数范围的 Biomancy 参数限制到实际概率范围。
    private static float clampChance(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    // 使用固定小数位显示存储值，避免不同语言环境改变小数分隔符。
    private static String formatValue(float value) {
        return String.format(Locale.ROOT, "%.1f/100", value);
    }

    // 使用百分号显示概率值。
    private static String formatPercent(float value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }
}
