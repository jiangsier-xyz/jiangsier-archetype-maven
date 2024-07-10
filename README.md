## What is jiangsier-archetype-maven
jiangsier-archetype-maven is an archetype project used to reduce non-business-related development time in the process of application creation.

## Why do jiangsier-archetype-maven
Modern frameworks, such as Spring Boot, have greatly reduced the non-business-related development workload in the application creation process. But as a "framework", they need to take into account flexibility and scalability, which brings a lot of complex configuration work. Some configuration items are difficult to understand, and you even have to read the source code of the framework project to set them correctly.

For routine applications with uncomplicated backgrounds, many configurations can be preset. In addition, for a small amount of customization of different application parts, it may be more efficient to directly provide the source code for different business parties to modify the customization than to set obscure (and possibly overlapping) configuration items for this purpose. This is also an important reason for the birth of jiangsier-archetype-maven. Applications created based on jiangsier-archetype-maven can be installed and run almost directly, and have already implemented a lot of business-independent features required by a distributed Web system.

## How to use jiangsier-archetype-maven
### Customize your awesome app
jiangsier-archetype-maven is a standard maven archetype project, you can directly select it in the IDE to create a new project. You can also create a new project through the `mvn archetype:generate` command line. It is recommended to use the tool script [gen-proj.sh](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/bin/gen-proj.sh) to create your project.
```shell
gen-proj.sh --group-id xyz.jiangsier \
   --artifact-id jiangsier-archetype-demo\
   --image-repository jiangsier/jiangsier-archetype-demo
```
This script may not suitable for you, feel free to modify it.

Let's call your application "awesome-app".

### Build the data access layer
The default database table design of awesome-app is only a semi-finished product, which only contains basic user related information. Please customize your database, update the table schema information in [schema.sql](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-dal/src/main/resources/sql/schema.sql), modify [generatorConfig.xml](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-dal/src/main/resources/mybatis-generator/generatorConfig.xml), and then use [mgb.sh](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/bin/mgb.sh) to generate your data access layer.

mgb.sh first uses `docker run` to run a MySQL instance on your local machine, then runs schema.sql through MGB (MyBatis Generator), and automatically generates the MyBatis data access layer according to the created table and generatorConfig.xml configuration. When all done, stop the docker container. During this process, the MySQL data files will not be persisted.
The above process means that you need to install the Docker runtime environment locally.

### Building the application
You can use [build.sh](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/bin/build.sh) to build your project. It does the following in order:
1. Compile and package your project using Maven.
2. Use `docker buildx` to generate amd64 and arm64 docker images at the same time, and push to the Docker warehouse (the default is hub.docker.com, please configure your own private warehouse).
3. Pull the dependent charts declared in the Helm configuration, currently relying on bitnami/mysql and bitnami/redis.

### Install, upgrade and uninstall the application
You can use [install.sh](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/bin/install.sh), [upgrade.sh](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/bin/upgrade.sh), [uninstall.sh](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/bin/uninstall.sh) to install, upgrade, uninstall your application and Its dependencies (MySQL & Redis). Note that information such as database URL, password, etc. will be mounted to the container as a Secret resource through a Spring configuration file (application-private.yml) generated during installation, and loaded by the Spring-boot application. For details, please refer to [_spring.tpl](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/configs/helm/templates/_spring.tpl) and [deployment.yaml](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/configs/helm/templates/deployment.yaml).

### Debugging the application
#### Local debugging
By default, awesome-app uses part of the configuration in helm to generate the Spring configuration needed at runtime, and try to avoid maintaining same parameters in multiple places and systems (such as MySQL URL). The rendering template is [_spring.tpl](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/configs/helm/templates/_spring.tpl). The rendering result will be accessed by the application as a Secret resource named "awesome-app-spring-properties", and the corresponding key/file name is "application-private.yml".

If you want to debug locally, you generally don't run helm rendering, and the connection addresses of many services are usually not the service addresses automatically deployed in k8s. You need to solve the problem of dependent services (such as MySQL, Redis) by yourself, and manually maintain an [application-local.yml](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/resources/application-local.yml), and load it in the debug option of the IDE, then you can debug your application normally.
> [local-deps.sh](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/scripts/local-deps.sh) can help you run/stop a local MySQL and Redis, hope they can help your debugging.

#### Remote debugging
Sometimes local debugging can't reproduce the problem on the server, or you can't find the provider of the services that the application depends on, so you want to directly debug the pods in the k8s cluster remotely. awesome-app has considered this aspect, you follow the steps below:
1. Set helm value "debug.jpda.enabled" to true
```yaml
debug:
   jpda:
     enabled: true
     port: 5005
```
Then when we render the Deployment, the corresponding debugging port and environment variables will be added.

2. Use kubectl port-forward to map your local port to the pod's debug port
```shell
kubectl --kubeconfig=<your config> port-forward pod/<awesome-app-5454655984-n8kf8> 5005:5005 -n <namespace>
```
You can also use the tools script [port-forward.sh](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/bin/port-forward.sh) to simplify the process. It randomly selects one of the application's pods to forward (note, this may not be what you want), and can automatically reconnect.

3. Configure remote debugging in the IDE, and set the "Host" to localhost. Normally, your IDE can be attached to the JVM process in the specified pod.

## What awesome-app has
### Distributed Cache
awesome-app implements Spring Cache based on Redisson, refer to [RedissonCacheConfig.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/java/config/RedissonCacheConfig.java). In addition, [FullNameKeyGenerator.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-service/src/main/java/cache/FullNameKeyGenerator.java) is customized to generate the cache key by class name, method name and parameter value, so as to support the use of the preset cache in the whole system. The preset cache includes the following:
- `@ShortPeriodCache`: short-term cache, expires in 2 seconds, suitable for high-frequency access, imprecise data.
- `@MiddlePeriodCache`: Medium-term cache, expired in 5 minutes, suitable for most scenarios that do not require high real-time results.
- `@LongPeriodCache`: long-term cache, 1-hour expiration, suitable for basically unchanged information, such as authentication tokens, binding relationships of users on different platforms, etc.

These annotations support keyBy parameters to generate cache keys themselves. keyBy supports SpEL (Spring Expression Language) syntax, note that the result of the expression must be a string. See [Appendix](#todo) for details.

The cache configuration is in [cache-config.yml](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/resources/cache-config.yml), these caches can be passed through the corresponding `@XxxPeriodCacheEvict` annotation to clear, you can also use regular cache annotations to clear (you may need to use their names "shortPeriod", "middlePeriod", "longPeriod").

### Distributed Session
awesome-app implements Spring Session based on Redisson, and sets the Session expiration time to one hour, refer to [RedissonSessionConfig.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/java/config/RedissonCacheConfig.java). As long as a server in the cluster has a Session set, the entire cluster will know.

Note that the implementation of RedissonConnectionFactory is related to the version of spring-session-data-redis. The second-party package currently used is redisson-spring-data-27 (because spring-session-data-redis uses 2.7.0). For specific correspondence, see [GitHub](https://github.com/redisson/redisson/tree/master/redisson-spring-data#usage).

### Distributed Scheduling
TODO

### Authentication and Authorization
Authentication is the process of identifying visitor identity information (and granting certain roles/permissions).  
Authorization is the process of judging whether the visitor is allowed to access the service/resource after the authentication is completed.
> Objectively speaking, authorization may only require an access credential (such as a token), not necessarily complete identity information.  
> When using the Spring Security framework, just pre-set the permissions required by the services/resources in the configuration, and set the visitor permissions/roles when the authentication is successful. The authorization process will automatically completed by the framework.  
> If the authentication fails, it is usually redirected to a specific page (such as a login page). If the authorization fails, it usually returns HTTP 403.

#### User System
Almost all systems need to build a user system. awesome-app has made the a basic design, including user table, permission table and binding table (binding refers to the mapping between users of this site and users of other sites). The field of user information largely refers to the [Standard Claims](https://openid.net/specs/openid-connect-core-1_0.html#Claims) of OpenID Connect (OIDC), which can basically meet the general requirements.

#### Portal Authentication
Portal authentication refers to relying on the user name and password passed in from a login page to match the user table in the database to complete the authentication. awesome-app does not modify the default settings of spring-security, the login page accesses "/login" via GET, and the path of login processing accesses "/login" via POST. Typically, these pages should be customized.

#### OAuth2 Authentication
The OAuth2 authentication process of most websites is designed with additional request parameters. For example, Google Cloud OAuth2 authorization parameters refer to [here](https://developers.google.com/identity/protocols/oauth2/web-server#creatingclient). In order to properly set these parameters, this system designs [OAuth2AuthorizationRequestCustomizer.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/java/auth/customizer/OAuth2AuthorizationRequestCustomizer.java) to customize the request content before the actual jump. Since the default OAuth2AuthorizationRequestResolver implementation of the Spring Security framework only supports setting a Customizer bean, considering scalability (supporting OAuth2 authentication for several websites), this class is not directly implemented according to Google's protocol, but according to the name of the OAuth2 authentication service provider to dynamically find the bean object that can handle the request. Protocol extension for Google, implemented in [GoogleOAuth2AuthorizationRequestCustomizer.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/java/auth/customizer/GoogleOAuth2AuthorizationRequestCustomizer.java).

In addition, awesome-app also supports Alibaba Cloud's OAuth2 authentication.

**Binding Mode**<br>
In general, OAuth2 is used for "authentication". That is, after the login of the external site is completed through redirection, the current user has a new login state; on the contrary, if the login fails at the external site, the current user will be in the "logout" state.

According to the actual business scenario, what you probably need is a "binding" logic: the user identity is always your own system user, and the authentication on the external site is only used for account-account binding. Then you may allow to obtain the user's external access token and refresh token of the website (as a resource server), in order to call the APIs of the external site to access the user's resources.

In this case, no matter whether the user's authentication on the external site is successful or not, it should not affect the current user's login status on this site, but only affects whether the account binding is successful or not. You can follow this flow by passing the parameter "bind=1" when accessing the OAuth2 authorization URL ("/oauth2/authorization/{registrationId}" by default).

#### API Token Authentication
awesome-app supports the APIs under specified path (default "/api/\*\*") to use token string for authentication. There are two ways to obtain token information:
- Get the token from the parameter, the default parameter name is "\_token", which is configurable.
- Get the token from the request header, the default key name is "X-API-TOKEN", which is configurable.
- If non of them is configured, use Bearer Token for authentication.
Priority is obtained from the parameters. If multiple \_token parameters are passed in, the first valid token shall prevail. Multiple tokens can also be passed in the request header, separated by "," and the first valid token from the left shall prevail.

Logged-in users can view, create, delete, and disable tokens through the "/token/\*\*" series of APIs, see [AuthController.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/java/controller/AuthController.java). The validity period is specified in seconds when the token is created. Defaults to 1 day if not specified. Each user can create up to 5 tokens.

In the design of database tables, tokens can support policy/authority scope, but the current implementation only supports "full scope", which means that holding a valid token can have all the interface permissions of the corresponding user.

**OpenAPI Specification**<br>
awesome-app supports the [OpenAPI Specification](https://swagger.io/specification/) by the [spring-doc](https://springdoc.org/) framework.

### Performance Tracing
#### Bean Tracing
You can add `@Trace` annotation to the method of bean implementation class to print performance log, refer to [TraceAspect.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/java/interceptor/TraceAspect.java), the format is as follows:
```
traceId|userId|className::methodName|status(S/F/B)|elapseTime(ms)|args|return|errorMessage|extInfo
```
Log location: logs/common-perf.log

status parameter description:
- S: Successful, indicating that the call is successful.
- F: Failed, indicating that the call failed.
- B: Bad request, indicating that the call is abnormal due to an illegal request. In this case, although the call fails, it should not be counted as a service failure, and the failure rate of the API is not raised.

`@Trace` can also be added to the class, which means to trace and print all the proxied methods under this class.
`@Trace`(ignoreArgs=true) ignores method parameters. For parameters containing many or very complex objects (such as large-sized lists), this can avoid the performance overhead of parsing the content of parameters.
`@Trace`(ignoreReturn=true) ignores the return value, the same as above.
`@Trace`(extInfo="'Hello ' + #username") prints additional information, supports SpEL (Spring Expression Language) syntax to provide a flexible way to access runtime information, note that the result of the expression must be a string. See [Appendix](#todo) for details.

**Information Confidentiality**<br>
If the parameter value of the method is sensitive, and you do not want to print the specific content, you can achieve it by annotating `@Secret` on the parameter, such as
```java
@Trace
public boolean login(String username, @Secret String password) {...}
```
Then the log print content is
```
ac11000216560387254571001d0093|-|c.a.t.e.c.c.TestComponent::login|S|19|Alice,*|true|-|-
```

#### HTTP Tracing
All HTTP API calls are uniformly traced, and the related implementation is in [TraceInterceptor.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-start/src/main/java/interceptor/TraceInterceptor.java).

## What awesome-app depends on
As a cloud-native application, the services that awesome-app depends on are all pulled from the helm repository and deployed to your cluster without requiring you to purchase separate cloud services.

But, from the perspective of operation and maintenance, you may prefer to purchase cloud services guaranteed by SLA (Service Level Agreement), then you only need to set [Helm Configurations](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/configs/helm/values.yaml) parameters as
```yaml
mysql:
  deployment:
    enabled: false
   url: <your mysql url>
   auth:
     rootPassword: <your mysql root password>
     username: <your mysql username>
     password: <your mysql password for the username>

redis:
  deployment:
    enabled: false
   url: <your redis url>
   auth:
     password: <your redis password>
```
Then awesome-app will not automatically install MySQL and Redis, and directly use the configuration information to connect. This information will be automatically injected into the Spring configuration file when installing, and then retrieved by the application.

### MySQL
Almost all applications are inseparable from the database, and awesome-app is the same. Considering the popularity of MySQL, awesome-app uses MySQL as its own database to store authentication-related information, and uses standard SQL sentences as much as possible to avoid hardcoding the MySQL dialect.

awesome-app deploys [bitnami/mysql](https://artifacthub.io/packages/helm/bitnami/mysql) to the same namespace by default. And the default MySQL url is `jdbc:mysql://awesome-app-mysql:3306/accounts?useUnicode=true&characterEncoding=utf-8`

You can also specify other MySQL instances.

### Redis
As mentioned above, awesome-app uses Redis as the backend to achieve most of its distributed capabilities. Redis is a must-have component of awesome-app.

awesome-app deploys [bitnami/redis](https://artifacthub.io/packages/helm/bitnami/redis) to the same namespace by default. And the default Reids url is `redis://awesome-app-redis-master:6379`

You can also specify other Redis instances.

## Appendix
Some annotations of this system support SpEL (Spring Expression Language) syntax and provide built-in objects, which can directly obtain the following information in expressions:

name | description | example
---- | ---- | ----
p{n} | n+1th parameter | #p1<br/>Indicates the second parameter of the current method call.
a{n} | same as p{n} | #a1
parameterName | Parameter Reference by parameterName | #username<br/>Indicates the parameter named username in the current method. Note: The parameter name is not reserved by default when Java is compiled, and this way may get nothing. If you explicitly specify the "-parameters" compilation switch, you can use this notation, otherwise it is recommended to use #p{n} to refer to parameter values.
methodName | current method name | #methodName
method | current method | #method.name<br/>Access the name property of the Method object corresponding to the current method
target | the currently called object | #target
targetClass | The type of the currently called object | #targetClass<br/>Access the Class object of the current instance
args | array of current method arguments | #args[0]

If you need to expand more information, you can modify [SpELUtils.java](https://github.com/jiangsier-xyz/jiangsier-archetype-maven/blob/main/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/util/SpELUtils.java).

For more powerful capabilities of SpEL, please refer to its [documentation](https://www.tutorialspoint.com/spring_expression_language/index.htm).
