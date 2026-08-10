package com.biomancy_jei.jade;

import com.github.elenterius.biomancy.block.cradle.PrimordialCradleBlock;
import com.github.elenterius.biomancy.block.cradle.PrimordialCradleBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class BiomancyJadePlugin implements IWailaPlugin {
    private static final PrimordialCradleJadeProvider CRADLE_PROVIDER = new PrimordialCradleJadeProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 向 Jade 注册原初摇篮服务端数据同步。
        registration.registerBlockDataProvider(CRADLE_PROVIDER, PrimordialCradleBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 向 Jade 注册原初摇篮客户端提示组件。
        registration.registerBlockComponent(CRADLE_PROVIDER, PrimordialCradleBlock.class);
    }
}
