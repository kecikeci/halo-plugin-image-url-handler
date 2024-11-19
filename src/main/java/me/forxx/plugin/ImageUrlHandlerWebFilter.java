package me.forxx.plugin;

import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.MediaTypeServerWebExchangeMatcher;
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
@Slf4j
@Component
public class ImageUrlHandlerWebFilter implements AdditionalWebFilter {

    private final ServerWebExchangeMatcher exchangeMatcher;
    private SettingConfig thisSettingConfig;

    public ImageUrlHandlerWebFilter(ReactiveSettingFetcher reactiveSettingFetcher) {
        Mono<SettingConfig> settingConfigMono =
            reactiveSettingFetcher.fetch("setting", SettingConfig.class)
                .defaultIfEmpty(new SettingConfig());
        this.thisSettingConfig = settingConfigMono.block();

        var htmlMatcher = new MediaTypeServerWebExchangeMatcher(MediaType.TEXT_HTML);
        htmlMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        var pathMatcher = ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/**");

        // 设置匹配器
        ServerWebExchangeMatcher settingMatcher = exchange -> {
            String suffix = this.thisSettingConfig.getSuffix();
            if (suffix == null || suffix.isEmpty()) {
                return ServerWebExchangeMatcher.MatchResult.notMatch();
            }
            String path = exchange.getRequest().getPath().toString();
            log.debug("当前页面路径: {}", path);
            if (isExcludedPath(path, this.thisSettingConfig.getExcludedPaths())) { // 自定义逻辑判断是否排除
                return ServerWebExchangeMatcher.MatchResult.notMatch(); // 跳过处理，不进行下一步
            }
            return ServerWebExchangeMatcher.MatchResult.match();
        };

        this.exchangeMatcher = new AndServerWebExchangeMatcher(
            htmlMatcher, pathMatcher, settingMatcher
        );
        log.info("------------------ImageUrlHandlerWebFilter 初始化完成");
    }

    @Override
    @NonNull
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return this.exchangeMatcher.matches(exchange)
            .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
            .switchIfEmpty(Mono.defer(() -> {
                log.debug("跳过页面路径 {}",
                    exchange.getRequest().getPath().toString());
                return chain.filter(exchange).then(Mono.empty());
            }))
            .flatMap(matchResult -> {
                var decoratedExchange = exchange.mutate()
                    .response(new ResponseDecorator(exchange, this.thisSettingConfig.getSuffix()))
                    .build();
                return chain.filter(decoratedExchange);
            });
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.LAST.getOrder();
    }

    private boolean isExcludedPath(String path, String other) {
        // 添加你的排除链接逻辑，例如：
        String defaultExcludedPaths = "/console,/api,/apis,/actuator,/plugins,/upload,/uc";
        String excludedPaths;
        if (other == null || other.isEmpty()) {
            excludedPaths = defaultExcludedPaths;
        } else {
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
