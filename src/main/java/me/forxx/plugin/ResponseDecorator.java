package me.forxx.plugin;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Mono;
import run.halo.app.infra.utils.PathUtils;

/**
 * @desc 响应装饰器（重构响应体）
 */
public class ResponseDecorator extends ServerHttpResponseDecorator {

    private static String suffix;

    public ResponseDecorator(ServerHttpResponse delegate,String path) {
        super(delegate);
        suffix = path;
    }

    /**
     * 匹配img标签添加参数到链接
     *
     * @param html
     * @return java.lang.String
     * @throws
     * @author GMQ
     * @date 2024/5/26 下午7:47
     **/
    public static String addParamToLinksByImg(String html) {
        // 正则表达式匹配<img>标签中的src属性
        String regex =
            "(<img[^>]*?)src\\s*=\\s*(\"|')((http|https)://|//)[^>]*?([^\"']|\\s)*\\2[^>]*>";
        // 使用正则表达式和模式匹配
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        StringBuilder modifiedHtml = new StringBuilder();
        while (matcher.find()) {
            // 获取原始src属性
            String imgDoc = matcher.group();
            Document document = Jsoup.parse(imgDoc);
            document.select("img").forEach(img -> {
                String src = img.attr("src");
                if (PathUtils.isAbsoluteUri(src)) {
                    if (!src.contains("?")) {
                        img.attr("src", src + suffix);
                    }
                }
            });
            // 替换原始的src属性
            matcher.appendReplacement(modifiedHtml, document.html());
        }
        // 完成替换操作
        matcher.appendTail(modifiedHtml);

        return modifiedHtml.toString();
    }

    /**
     * 匹配style中的background-image添加参数到链接
     *
     * @param html
     * @return java.lang.String
     * @throws
     * @author GMQ
     * @date 2024/5/26 下午7:46
     **/
    public static String addParamToLinksByStyle(String html) {
        // 正则表达式匹配<img>标签中的src属性
        String regex = "(?:background-image:\\s*url\\()([^\"']*)\\)";
        // 使用正则表达式和模式匹配
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        StringBuilder modifiedHtml = new StringBuilder();
        while (matcher.find()) {
            // 获取原始src属性
            String src = matcher.group(1);
            if (PathUtils.isAbsoluteUri(src)) {
                if (!src.contains("?")) {
                    // 添加?x=f到src属性
                    String updatedSrc = src + suffix;
                    String replacement = matcher.group().replace(src, updatedSrc);
                    // 替换原始的src属性
                    matcher.appendReplacement(modifiedHtml, replacement);
                }
            }
        }
        // 完成替换操作
        matcher.appendTail(modifiedHtml);

        return modifiedHtml.toString();
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

        if (body instanceof Mono) {
            Mono<DataBuffer> monoBody = (Mono<DataBuffer>) body;
            return super.writeWith(monoBody.map(dataBuffer -> {
                DataBufferUtils.retain(dataBuffer); // 首先保留DataBuffer，避免被自动释放
                byte[] originalContentByte = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(originalContentByte);
                DataBufferUtils.release(dataBuffer);// 释放掉内存

                String originalContent = new String(originalContentByte, StandardCharsets.UTF_8);
                String modifiedContent;
                if (isLikelyHtml(originalContent)) {
                    modifiedContent = addParamToLinksByStyle(addParamToLinksByImg(originalContent));
                } else {
                    modifiedContent = originalContent;
                }
                // 将修改后的内容转换回DataBuffer
                return bufferFactory().wrap(modifiedContent.getBytes(StandardCharsets.UTF_8));
            }));
        }
        return super.writeWith(body);
    }

    /**
     * 判断是否是html
     *
     * @param input
     * @return boolean
     * @throws
     * @author GMQ
     * @date 2024/5/26 下午7:46
     **/
    public boolean isLikelyHtml(String input) {
        return input != null && (input.contains("<") && input.contains(">"));
    }
}