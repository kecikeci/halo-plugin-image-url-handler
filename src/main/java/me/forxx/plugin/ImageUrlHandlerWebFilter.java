package me.forxx.plugin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.security.AdditionalWebFilter;

/**
 * 返回数据处理
 *
 * @author GMQ
 * @date 2024/5/23 下午6:24
 **/
@RequiredArgsConstructor
@Component
public class ImageUrlHandlerWebFilter implements AdditionalWebFilter {

    final ServerWebExchangeMatcher
        requiresMatcher = ServerWebExchangeMatchers.pathMatchers("/**");

    private final ReactiveSettingFetcher reactiveSettingFetcher;

    Logger log = LoggerFactory.getLogger(ImageUrlHandlerWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Mono<SettingConfig> settingConfigMono = reactiveSettingFetcher.fetch("setting",SettingConfig.class)
            .defaultIfEmpty(new SettingConfig());
        return requiresMatcher.matches(exchange)
            .flatMap(matchResult -> {
                // 使用flatMap操作符，将SettingConfig与matchResult组合
                return settingConfigMono
                    .flatMap(settingConfig -> {
                        String suffix = settingConfig.getSuffix();
                        if (suffix == null || suffix.isEmpty()) {
                            return chain.filter(exchange);
                        }
                        String path = exchange.getRequest().getPath().toString();
                        if (isExcludedPath(path,settingConfig.getExcludedPaths())) { // 自定义逻辑判断是否排除
                            return chain.filter(exchange); // 跳过处理，不进行下一步
                        } else {
                            ResponseDecorator decorator = new ResponseDecorator(exchange.getResponse(), settingConfig.getSuffix());
                            return chain.filter(exchange.mutate().response(decorator).build());
                        }
                    });
            })
            .onErrorResume(ex -> chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.LAST.getOrder();
    }


    private boolean isExcludedPath(String path, String other) {
        // 添加你的排除链接逻辑，例如：
        String defaultExcludedPaths = "/console,/api,/apis,/actuator,/plugins,/upload,/uc";
        String excludedPaths;
        if (other == null || other.isEmpty()){
            excludedPaths = defaultExcludedPaths;
        }else {
            excludedPaths = defaultExcludedPaths + "," + other;
        }

        for (String excludedPath : excludedPaths.split(",")) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }

    @Data
    public static class SettingConfig {
        String suffix;
        String excludedPaths;
    }

}
