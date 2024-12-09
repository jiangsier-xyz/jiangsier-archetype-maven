#set( $symbol_pound = '#' )
${symbol_pound}${symbol_pound} 什么是 ${artifactId}
${artifactId} 是由 [jiangsier-archetype-maven](https://github.com/jiangsier-xyz/jiangsier-archetype-maven) 生成的一套业务系统。

${symbol_pound}${symbol_pound} ${artifactId} 如何使用
${symbol_pound}${symbol_pound}${symbol_pound} 构建数据访问层
${artifactId} 默认的数据库表设计只是半成品，仅包含基本的用户体系相关信息。请定制你的数据库，将表结构信息更新到 [schema.sql](${scmUrl}/blob/main/${artifactId}-dal/src/main/resources/sql/schema.sql)，修改 [generatorConfig.xml](${scmUrl}/blob/main/${artifactId}-dal/src/main/resources/mybatis-generator/generatorConfig.xml)，再使用 [mgb.sh](${scmUrl}/blob/main/bin/mgb.sh) 来生成你的数据访问层。

mgb.sh 首先在你的机器上使用 `docker run` 运行一个 MySQL 实例，然后通过 MGB(MyBatis Generator) 来运行 schema.sql，并依据创建的表和 generatorConfig.xml 配置来自动生成 MyBatis 数据访问层。做完这一切之后，停止 docker 容器的运行。这个过程中的 MySQL 的数据文件将不会被持久化。
上述过程意味着你本机需要安装 Docker 运行环境。

${symbol_pound}${symbol_pound}${symbol_pound} 构建应用程序
你可以使用 [build.sh](${scmUrl}/blob/main/bin/build.sh) 来构建你的项目。它依次完成下面的工作：
1. 使用 Maven 编译和打包你的项目。
2. 使用 `docker buildx` 来同时生成 amd64 和 arm64 的 docker 镜像，并 push 到 Docker 仓库（默认是 hub.docker.com，请配置你自己的私有仓库）。
3. 拉取 Helm 配置中声明的依赖 charts，目前依赖了 bitnami/mysql 和 bitnami/redis。

${symbol_pound}${symbol_pound}${symbol_pound} 安装、升级和卸载应用程序
你可以分别使用 [install.sh](${scmUrl}/blob/main/bin/install.sh)、[upgrade.sh](${scmUrl}/blob/main/bin/upgrade.sh)、[uninstall.sh](${scmUrl}/blob/main/bin/uninstall.sh) 来安装、升级、卸载你的应用及其依赖（MySQL & Redis）。注意，诸如数据库 URL、密码等信息，会通过安装时生成的一个 Spring 配置文件（application-private.yml） 以 Secret 资源的方式挂载到容器，并被 Spring-boot 应用加载。具体内容可以参考 [_spring.tpl](${scmUrl}/blob/main/configs/helm/templates/backend/_spring.tpl) 和 [deployment.yaml](${scmUrl}/blob/main/configs/helm/templates/backend/deployment.yaml)。

${symbol_pound}${symbol_pound}${symbol_pound} 调试应用程序
${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} 本地调试
默认情况下，${artifactId} 使用 helm 中的部分配置来生成运行时需要的 Spring 配置，尽量避免同一个参数在多个地方、多种系统里维护（比如 MySQL URL）。具体的渲染模版请参考 [_spring.tpl](${scmUrl}/blob/main/configs/helm/templates/backend/_spring.tpl)。渲染结果会以名为“${artifactId}-spring-properties”的 Secret 资源被应用程序访问，对应的键/文件名是“application-private.yml”。

如果想要进行本地调试，一般不会运行 helm 渲染，并且，许多服务的连接地址通常也不是 k8s 中自动部署的服务地址。你需要自行解决依赖服务（如 MySQL、Redis）的问题，并根据实际情况，手工维护一份 [application-local.yml](${scmUrl}/blob/main/${artifactId}-start/src/main/resources/application-local.yml)，再在 IDE 的调试选项中加载它，就可以正常调试你的应用了。
> [local-deps.sh](${scmUrl}/blob/main/scripts/local-deps.sh) 可以帮助你运行/停止一个本地 MySQL 和 Redis，希望能有助于你的调试。

${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} 远程调试
有时候本地调试并不能重现服务器上的问题，或者你找不到应用程序依赖的服务的提供方，因此你期望对 k8s 集群里的 pods 直接进行远程调试。${artifactId} 做了这方面的考虑，你按照以下步骤来进行：
1. 将 helm values 中的 debug.jpda.enabled 设置为 true
```yaml
debug:
  jpda:
    enabled: true
    port: 5005
```
则我们渲染 Deployment 的时候，会增加相应的调试端口和环境变量。

2. 使用 kubectl port-forward 将你的本地端口映射到 pod 的调试端口
```shell
kubectl --kubeconfig=<your config> port-forward pod/<${artifactId}-5454655984-n8kf8> 5005:5005 -n <namespace>
```
你也可以使用工具脚本 [port-forward.sh](${scmUrl}/blob/main/bin/port-forward.sh) 来简化这个过程。它从应用程序的 pods 中随机选择一个进行转发（注意，这未必是你想要的），并可以自动重连。

3. 在 IDE 中配置远程调试，Host 设置为 localhost。正常情况下，你的 IDE 就可以 attach 到指定 pod 中的 JVM 进程上了。

${symbol_pound}${symbol_pound} ${artifactId} 有什么
${symbol_pound}${symbol_pound}${symbol_pound} 分布式缓存
${artifactId} 基于 Redisson 实现了 Spring Cache，参考 [RedissonCacheConfig.java](${scmUrl}/blob/main/${artifactId}-start/src/main/java/${packageDir}/config/RedissonCacheConfig.java)。另外，自定义了 [FullNameKeyGenerator.java](${scmUrl}/blob/main/${artifactId}-service/src/main/java/${packageDir}/cache/FullNameKeyGenerator.java) 来产生包含类名、方法名和参数值的缓存 key，以便支持预置的缓存在系统全范围内使用。预置缓存主要包括以下几个：
- `@ShortPeriodCache`：短期缓存，2 秒过期。适用于高频访问、可接受些微数据延迟的接口。
- `@MiddlePeriodCache`：中期缓存，5 分钟过期，适合于大部分对实时结果要求不高的场景。
- `@LongPeriodCache`：长期缓存，1 小时过期，适合于基本不变的信息，比如认证凭据、不同平台用户的绑定关系等。

这些注解支持 keyBy 参数来自行生成缓存 key。keyBy 支持 SpEL（Spring Expression Language）语法，注意表达式的结果必须是字符串。具体内容见[附录](${symbol_pound}todo)。

缓存配置在 [cache-config.yml](${scmUrl}/blob/main/${artifactId}-start/src/main/resources/cache-config.yml)，这些缓存可以通过对应的 `@XxxPeriodCacheEvict` 注解进行清除，也可以使用常规的缓存注解清理（这时你可能需要用到它们的名字“shortPeriod”、“middlePeriod”、“longPeriod”）。

${symbol_pound}${symbol_pound}${symbol_pound} 分布式会话
${artifactId} 基于 Redisson 实现了 Spring Session，并且设置了 Session 过期时间为一小时，参考 [RedissonSessionConfig.java](${scmUrl}/blob/main/${artifactId}-start/src/main/java/${packageDir}/config/RedissonCacheConfig.java)。只要集群里的一台服务器设置了 Session，则整个集群可见。

注意 RedissonConnectionFactory 的实现，与 spring-session-data-redis 版本有关。具体对应关系见 [GitHub](https://github.com/redisson/redisson/tree/master/redisson-spring-data${symbol_pound}usage)。

${symbol_pound}${symbol_pound}${symbol_pound} 分布式调度
TODO

${symbol_pound}${symbol_pound}${symbol_pound} 认证与鉴权
认证，是识别访问者身份信息（并赋予一定角色/权限）的过程。  
鉴权，是在完成认证的情况下，判断访问者是否被允许访问服务/资源的过程。
> 客观来说，鉴权可能只需要一个访问凭证（如 token），未必需要完整的身份信息。  
> 如果是系统内部鉴权，使用 Spring Security 框架，在配置中预设置服务/资源所需权限，并在认证成功的同时设定好访问者权限/角色，鉴权过程由框架自动完成。  
> 认证失败，一般重定向至特定页面（比如登录页）；鉴权失败，一般返回 HTTP 403。

${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} 用户体系
几乎所有系统都要建设用户体系。${artifactId} 做了基本的设计，包含用户表、权限表和绑定表（绑定指的是将本站用户与外站用户建立映射）。用户信息的字段很大程度上参考了 OpenID Connect (OIDC) 的[标准声明](https://openid.net/specs/openid-connect-core-1_0.html${symbol_pound}Claims)，基本上可以满足一般性需求。

${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} 门户认证
门户认证指的是依赖登录页面中传入的用户名、密码，与数据库中的用户表匹配，完成认证。${artifactId} 没有修改 spring-security 的默认设置，登录页面是 GET 方式访问"/login"，登录处理的路径则是 POST 方式访问"/login"。通常情况下，这些页面是需要定制的。

${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} OAuth2 认证
大部分网站的 OAuth2 认证流程，都设计了额外的请求参数。比如 Google Cloud OAuth2 授权参数参考[这里](https://developers.google.com/identity/protocols/oauth2/web-server${symbol_pound}creatingclient)。为了能适当设置这些参数，本系统设计了 [OAuth2AuthorizationRequestCustomizer.java](${scmUrl}/blob/main/${artifactId}-start/src/main/java/${packageDir}/auth/customizer/OAuth2AuthorizationRequestCustomizer.java) 用于实际跳转前对请求内容进行定制化处理。由于 Spring Security 框架的默认的 OAuth2AuthorizationRequestResolver 实现只支持设置一个 Customizer，考虑到可扩展性（支持更多网站的 OAuth2 认证），此类并没有直接按照 Google 的协议实现，而是根据 OAuth2 认证服务商的名称来动态查找可以处理的 bean 对象。针对 Google 的协议扩展，在 [GoogleOAuth2AuthorizationRequestCustomizer.java](${scmUrl}/blob/main/${artifactId}-start/src/main/java/${packageDir}/auth/customizer/GoogleOAuth2AuthorizationRequestCustomizer.java) 中进行了处理。

另外，${artifactId} 还支持了阿里云的 OAuth2 认证。

**绑定模式**<br>
一般情况下，OAuth2 用于“认证”。即通过重定向完成外站的登录后，当前用户拥有一个新的登录态；相反，如果在外站登录失败，则当前用户将处于“登出”状态。

根据实际的业务场景，你很可能需要的是一个“绑定”逻辑：用户身份永远是你自己的系统用户，在外站的认证只是用来做一个账号关联，并且，你可以以此获取用户在外站（作为资源服务器）的访问凭证和刷新凭证，以便获取他在外站上的资源。

在这种情况下，用户在外站的认证，无论是否成功，都不应该影响当前用户在本站点的登录态，仅仅影响了账号绑定成功与否。你可以通过在访问 OAuth2 授权 URL（默认是“/oauth2/authorization/{registrationId}”）时，传入参数"bind=1"来采用这种流程。

${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} API Token 认证
${artifactId} 支持制定路径下的接口（默认“/api/\*\*”）使用 token 字符串进行认证。获取 token 信息的方式有两种：
- 从参数中获取 token，默认参数名为“\_token”，可配置。
- 从请求头中获取 token，默认键名为“X-API-TOKEN”，可配置。
- 均未配置的情况下，采用 Bearer Token 方式鉴权。
优先从参数中获取。如果配置了多个 \_token 参数，以第一个有效 token 为准。请求头中也可传递多个 token，以","进行分隔，以左数第一个有效 token 为准。

已登录用户可以通过"/token/\*\*"系列接口来查看、创建、删除、禁用 token，详见 [AuthController.java](${scmUrl}/blob/main/${artifactId}-start/src/main/java/${packageDir}/controller/AuthController.java)。token 创建时以秒为单位指定有效期。如果不指定，默认为 1 天。每个用户最多可以创建 5 个token。

在数据库表的设计中，token 可支持策略/权限范围，但目前实现只支持"全部范围"，意味着持有有效 token 即可拥有对应用户的全部接口权限。

**OpenAPI 规范**<br>
${artifactId} 通过 [spring-doc](https://springdoc.org/) 框架支持 [OpenAPI 规范描述](https://swagger.io/specification/)。

${symbol_pound}${symbol_pound}${symbol_pound} 性能追踪
${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} Bean 追踪
可以在 bean 的实现类的方法上添加 `@Trace` 注解，来打印性能日志，参考 [TraceAspect.java](${scmUrl}/blob/main/${artifactId}-start/src/main/java/${packageDir}/interceptor/TraceAspect.java)，格式如下：
```
traceId|userId|className::methodName|status(S/F/B)|elapseTime(ms)|args|return|errorMessage|extInfo
```
日志位置：logs/common-perf.log

status 参数说明：
- S：Successful，表明服务成功。
- F：Failed，表明服务失败。
- B：Bad request，表明因为请求不合法导致服务异常，这种情况下虽然调用失败了，但不应算作服务失败，不统计到接口失败率。

`@Trace` 也可以添加到类上，表示对这个类下的所有被代理的方法追踪打印。
`@Trace`(ignoreArgs=true) 忽略方法参数，对于包含很多或很复杂对象的参数的情况而言（比如大尺寸的列表），这可以避免解析参数内容带来的性能开销。
`@Trace`(ignoreReturn=true) 忽略返回值，作用同上。
`@Trace`(extInfo="'Hello ' + ${symbol_pound}username") 打印额外信息，支持 SpEL（Spring Expression Language）语法以便提供一种灵活的方式来访问运行时信息，注意表达式的结果必须是字符串。具体内容见[附录](${symbol_pound}todo)。

**信息保密**<br>
如果方法的参数值具有敏感性，不希望被性能追踪的切面打印具体内容，可以通过在参数上注解 `@Secret` 实现，如
```java
@Trace
public boolean login(String username, @Secret String password) {...}
```
则日志打印内容为
```
ac11000216560387254571001d0093|-|c.a.t.e.c.c.TestComponent::login|S|19|Alice,*|true|-|-
```

${symbol_pound}${symbol_pound}${symbol_pound}${symbol_pound} HTTP 追踪
所有 HTTP API 的调用被统一追踪，相关实现在 [TraceInterceptor.java](${scmUrl}/blob/main/${artifactId}-start/src/main/java/${packageDir}/interceptor/TraceInterceptor.java)。

${symbol_pound}${symbol_pound} ${artifactId} 依赖什么
作为云原生应用，${artifactId} 所依赖的服务，均通过 helm repository 拉取，部署到您的集群，无需您购买单独的云服务。

当然，从运维的角度，也许您更希望购买有 SLA(Service Level Agreement) 保障的云服务，那么只需要设置 [Helm 配置](${scmUrl}/blob/main/configs/helm/values.yaml)参数为
```yaml
mysql:
  enabled: false
  url: <your mysql url>
  auth:
    rootPassword: <your mysql root password>
    username: <your mysql username>
    password: <your mysql password for the username>

redis:
  enabled: false
  url: <your redis url>
  auth:
    password: <your redis password>
```
则 ${artifactId} 将不会自动安装 MySQL 和 Redis，直接使用配置信息进行连接。这些信息将在安装时自动注入到 Spring 配置文件，再由应用程序获取。

${symbol_pound}${symbol_pound}${symbol_pound} MySQL
几乎所有的应用都离不开数据库，${artifactId} 也一样。考虑到 MySQL 的流行度，${artifactId} 使用 MySQL 来作为自身的数据库，存放认证相关的信息，并且尽量使用标准的 SQL 语法，避免硬编码 MySQL 方言。

${artifactId} 默认部署 [bitnami/mysql](https://artifacthub.io/packages/helm/bitnami/mysql) 到同一命名空间，这时，MySQL url 默认是 `jdbc:mysql://${artifactId}-mysql:3306/accounts?useUnicode=true&characterEncoding=utf-8`

你也可以指定其他的 MySQL 实例。

${symbol_pound}${symbol_pound}${symbol_pound} Redis
如上文所述，${artifactId} 使用 Redis 作为后端实现了大部分分布式能力。Redis 是 ${artifactId} 的必备组件。

${artifactId} 默认部署 [bitnami/redis](https://artifacthub.io/packages/helm/bitnami/redis) 到同一命名空间，这时，Reids url 默认是 `redis://${artifactId}-redis-master:6379`

你也可以指定其他的 Redis 实例。

${symbol_pound}${symbol_pound} 附录
本系统的部分注解，支持 SpEL（Spring Expression Language）语法并提供了内置对象，能在表达式中直接获取以下信息：

名称 | 描述 | 示例
---- | ---- | ----
p{n} | 第 n+1 个参数 | ${symbol_pound}p1<br/>表示当前方法调用的第 2 个参数。
a{n} | 同 p{n} | ${symbol_pound}a1
参数名 | 根据参数名引用参数 | ${symbol_pound}username<br/>表示当前方法中名为 username 的参数。注意：Java 编译时默认不保留参数名称，此时这种写法获取不到任何内容。如果明确指定过“-parameters”编译开关，则可以使用此写法，否则建议采用 ${symbol_pound}p{n} 的写法来引用参数值。
methodName | 当前方法名 | ${symbol_pound}methodName
method | 当前方法 | ${symbol_pound}method.name<br/>访问当前方法对应的 Method 对象的 name 属性
target | 当前被调用的对象 | ${symbol_pound}target
targetClass | 当前被调用的对象的类型 | ${symbol_pound}targetClass<br/>访问当前实例的 Class 对象
args | 当前方法参数组成的数组 | ${symbol_pound}args[0]

如果需要扩展更多的信息，可以修改 [SpELUtils.java](${scmUrl}/blob/main/${artifactId}-common/src/main/java/${packageDir}/util/SpELUtils.java)。

SpEL 更多的强大能力，可以参考其[文档](https://www.tutorialspoint.com/spring_expression_language/index.htm)。
