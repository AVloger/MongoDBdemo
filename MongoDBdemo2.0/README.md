

# **2.0升级版内容**

# 1 后台管理板块

![image-20211031151250422](http://avloger.oss-cn-beijing.aliyuncs.com/img/image-20211031151250422.png)

## 登录流程设计

首先，在数据库中查询用户记录，伪代码如下：

```
select * from xxx_user where account_number = 'xxxx';
```

如果不存在这条记录则表示身份验证失败，登录流程终止；如果存在这条记录，则表示身份验证成功，接下来则需要进行登录状态的存储和验证了。

用户登录成功后我们将用户信息放到 session 对象中，之后再实现一个拦截器（后面一篇文章会介绍拦截器相关的知识并给出代码），在访问项目时判断 session 中是否有用户信息，有则放行请求，没有就跳转到登录页面。

## 登录页面实现

在 templates/admin 目录中新建 login.html 模板页面，模板引擎我们选择的是 Thymeleaf。该页面是直接修改的 AdminLTE3 模板的登录页，将页面中的原本的文案修改为中文并微调了一下页面布局，同时增加了验证码的设计，最终的页面效果如上图所示。

用户在输入账号、密码和验证码后，点击登录按钮后将会向后端发送登录请求，请求地址为 admin/login，请求类型为 post，在 form 表单中已经定义了登陆的请求路径：

```
<form th:action="@{/admin/login}" method="post">
```

通过 form 表单中的字段我们可以看出，该登录请求会带着 3 个请求参数，分别是：

- userName
- password
- verifyCode

之后我们在编写后端代码时需要接收这三个字段，并根据这三个字段进行相应的业务处理。

## 后端功能实现

通过main函数中的@MapperScan("com.mongodemo.dao")查找对应的接口文件，接口文件在AdminMapper.xml中通过用户名和密码查询用户记录。

首先对参数进行校验，参数中包括登陆信息和验证码，验证码的显示和校验的逻辑在之前的文章中已经有介绍，这里就直接整合到了登陆功能中，拿参数与存储在 session 中的验证码值进行比较，之后调用 adminUserService 业务层代码查询用户对象，最后根据验证结果来跳转页面，如果登陆成功则跳转到管理系统的首页，失败的话则带上错误信息返回到登录页，登录页中会显示出登陆的错误信息。

# 2 整合Mybatis

## 添加依赖

如果要将其整合到当前项目中，首先需要将其依赖配置增加到 pom.xml 文件中，我们选择的 mybatis-springboot-starter 版本为 1.3.2，需要 Spring Boot 版本达到 1.5 或者以上版本，同时，我们需要将数据源依赖和 jdbc 依赖也添加到配置文件中(如果不添加的话，将会使用默认数据源和 jdbc 配置)。

## application.properties 配置

我们只配置 mapper-locations 即可，最终的 application.properties 文件如下：

```
# datasource config
spring.datasource.name=newbee-mall-datasource
spring.datasource.url=jdbc:mysql://localhost:3306/test_db?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=123456

mybatis.mapper-locations=classpath:mapper/*Dao.xml
```

## 启动类增加 Mapper 扫描

在启动类中添加对 Mapper 包扫描 @MapperScan，Spring Boot 启动的时候会自动加载包路径下的 Mapper 接口。当然也可以直接在每个 Mapper 接口上面添加 @Mapper 注解，但是如果 Mapper 接口数量较多，在每个 Mapper 加注解是挺繁琐的，建议使用扫描注解。

# 3 登录拦截器

我们登陆的目的是为了能够访问后台页面（上文中后台路径为 `/index`），但即使我们不进行登陆操作也可以访问该页面。不仅仅是 index 请求，后台管理系统中还有其他多个功能模块的页面和接口请求，与此时的 index 请求一样，如果不做权限处理的话都可以在未登录的状态下访问。也就是说我们并没有在用户访问后台管理系统的资源时进行身份认证和访问拦截，没有进行身份认证和访问权限拦截的话，我们的系统依然是完全暴露出去的。

定义一个 Interceptor 非常简单方式也有几种，这里简单列举两种：

1. 新建类要实现 Spring 的 HandlerInterceptor 接口

2. 新建类继承实现了 HandlerInterceptor 接口的实现类，例如已经提供的实现了 HandlerInterceptor 接口的抽象类 HandlerInterceptorAdapter

- **preHandle**：在业务处理器处理请求之前被调用。预处理，可以进行编码、安全控制、权限校验等处理；
- **postHandle**：在业务处理器处理请求执行完成后，生成视图之前执行。
- **afterCompletion**：在DispatcherServlet完全处理完请求后被调用，可用于清理资源等，返回处理（已经渲染了页面）；

## 定义拦截器

新建 interceptor 包，在包中新建 AdminLoginInterceptor 类，该类需要实现 HandlerInterceptor 接口.

我们只需要完善 preHandle 方法即可，同时在类声明上方添加 @Component 注解使其注册到 IOC 容器中。

通过上面代码可以看出，在请求的预处理过程中读取当前 session 中是否存在 loginUser 对象，如果不存在则返回 false 并跳转至登录页面，如果已经存在则返回 true，继续做后续处理流程，我在代码中也添加了几条打印语句，可以在测试的时候判断拦截器是否正确的产生作用。

## 配置拦截器

在实现拦截器的相关方法之后，我们需要对该拦截器进行配置以使其生效，在 Spring Boot 1.x 版本中我们通常会继承 WebMvcConfigurerAdapter 类，但是在 Spring Boot 2.x 版本中，WebMvcConfigurerAdapter 被 @deprecated 注解标注说明已过时，WebMvcConfigurer 接口现在已经有了默认的空白方法，所以在Spring Boot 2.x 版本下更好的做法还是实现 WebMvcConfigurer 接口。

在该配置类中，我们添加刚刚新增的 AdminLoginInterceptor 登陆拦截器，并对该拦截器所拦截的路径进行配置，由于后端管理系统的所有请求路径都以 `/admin` 开头，所以拦截的路径为 `/admin/**` ，但是登陆页面以及部分静态资源文件也是以 `/admin` 开头，所以需要将这些路径排除，配置如上，大家注意一下 `addPathPatterns()` 方法和 `excludePathPatterns()` 两个方法，它们分别是添加路径拦截规则和排除路径拦截规则。

# 4 整合kaptcha实现验证码

所选择的方案是Google 的 kaptcha 框架，后续如果有更好的方案可以进行切换更新。

这样可以有效防止恶意用户对网站进行破坏，实际上是用验证码是现在很多网站通行的方式，虽然会使得某些操作变得麻烦一点，但是对大部分的功能场景来说这个功能还是很有必要，也很重要。

## 添加依赖

Spring Boot 整合 kaptcha 的第一步就是增加依赖，首先我们需要将其依赖配置增加到 pom.xml 文件中。

```
        <dependency>
            <groupId>com.github.penggle</groupId>
            <artifactId>kaptcha</artifactId>
            <version>2.3.2</version>
        </dependency>
```

## 配置

注册 DefaultKaptcha 到 IOC 容器中，新建 config 包，之后新建 KaptchaConfig 类，内容如下：

```
package ltd.newbee.mall.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import com.google.code.kaptcha.util.Config;
import java.util.Properties;

@Component
public class KaptchaConfig {
    @Bean
    public DefaultKaptcha getDefaultKaptcha(){
        com.google.code.kaptcha.impl.DefaultKaptcha defaultKaptcha = new com.google.code.kaptcha.impl.DefaultKaptcha();
        Properties properties = new Properties();
        // 图片边框
        properties.put("kaptcha.border", "no");
        // 字体颜色
        properties.put("kaptcha.textproducer.font.color", "black");
        // 图片宽
        properties.put("kaptcha.image.width", "160");
        // 图片高
        properties.put("kaptcha.image.height", "40");
        // 字体大小
        properties.put("kaptcha.textproducer.font.size", "30");
        // 验证码长度
        properties.put("kaptcha.textproducer.char.space", "5");
        // 字体
        properties.setProperty("kaptcha.textproducer.font.names", "宋体,楷体,微软雅黑");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
```

## 后端处理

在 controller 包中新建 KaptchaController，之后注入刚刚配置好的 DefaultKaptcha 类，然后就可以新建一个方法，在方法里可以生成验证码对象，并以图片流的方式写到前端以供显示。

## 前端处理

新建 kaptcha.html，在该页面中显示验证码，代码如下：

```
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>验证码显示</title>
</head>
<body>
<img src="/kaptcha"
     onclick="this.src='/kaptcha?d='+new Date()*1">
</body>
</html>
```

访问后端验证码路径 /kaptcha，并将其返回显示在 img 标签中，之后定义了 onclick 方法，在点击该 img 标签时可以动态的切换显示一个新的验证码，点击时访问的路径为 '/kaptcha?d='+new Date()*1，即原来的验证码路径后面带上一个时间戳参数，时间戳是会变化的，所以每次点击都会是一个与之前不同的请求，如果不这样处理的话，由于浏览器的机制可能并不会重新发送请求，无法实现刷新验证码的功能交互。

# 5 遇到的问题

1. maven clean项目。在初次使用thymeleaf的时候，因为前后端不分离的原因，每次对于前端页面进行修改之后，都要进行一次maven clean的操作。如果没有清除缓存，会出现前端页面没有更新的情况。
2. MapperScan，出现找不到Mapper的错误，先去查看主类中是否注释了Mapper的位置，然后再去看properties中是否注明。
3. properties和Yaml的写法构造是不一样的。
