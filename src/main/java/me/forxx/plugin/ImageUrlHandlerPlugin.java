package me.forxx.plugin;

import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;

/**
 * 返回数据处理插件
 * @return
 * @exception
 * @author GMQ
 * @date 2024/5/23 下午5:13
 **/
@Component
public class ImageUrlHandlerPlugin extends BasePlugin {


    public ImageUrlHandlerPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
