package com.biomancy_jei.jade;

import com.github.elenterius.biomancy.block.cradle.PrimordialCradleBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public final class PrimordialCradleJadeDataProvider implements IServerDataProvider<BlockAccessor> {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("biomancy_jei", "primordial_cradle");

    @Override
    public Identifier getUid() {
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

}
