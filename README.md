## halo-plugin-image-url-handler

Halo 2.0 插件-图片链接处理

### 插件说明

本插件主要为对象存储、CDN、nginx配置重定向等可设置图片处理参数的服务提供图片处理功能
参考[https://github.com/halo-sigs/plugin-page-cache](https://github.com/halo-sigs/plugin-page-cache)项目部分逻辑，感谢！

### 功能逻辑

拦截所有html页面(默认排除后台页面、系统页面等，也可自定义排除路径)，解析出html页面中的图片链接(含`img`标签和`background-image`属性)，并在图片链接后拼上配置的图片处理后缀

### 预览地址

[https://4xx.me](https://4xx.me)

### 主题推荐

- [4xx-first](https://4xx.me/archives/4xx-first-tutorial)

### 插件效果图

图片处理前
![before-time.png](image%2Fbefore-time.png)
图片处理后
![after-time.png](image%2Fafter-time.png)

### 插件配置界面

![config.png](image%2Fconfig.png)
