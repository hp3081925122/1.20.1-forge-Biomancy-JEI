package com.biomancy_jei.jade;

import com.github.elenterius.biomancy.block.cradle.PrimordialCradleBlock;
import com.github.elenterius.biomancy.block.cradle.PrimordialCradleBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class BiomancyJadePlugin implements IWailaPlugin {
    private static final PrimordialCradleJadeDataProvider CRADLE_DATA_PROVIDER = new PrimordialCradleJadeDataProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 向 Jade 注册原初摇篮服务端数据同步。
        org.slf4j.LoggerFactory.getLogger(BiomancyJadePlugin.class).debug("Registering separate cradle server data provider");
        registration.registerBlockDataProvider(CRADLE_DATA_PROVIDER, PrimordialCradleBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 向 Jade 注册原初摇篮客户端提示组件。
        registration.registerBlockComponent(new PrimordialCradleJadeProvider(), PrimordialCradleBlock.class);
    }
}
