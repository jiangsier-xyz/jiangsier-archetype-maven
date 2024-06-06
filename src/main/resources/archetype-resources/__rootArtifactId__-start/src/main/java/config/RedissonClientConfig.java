#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.config;

import com.esotericsoftware.kryo.Kryo;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ${package}.config.util.UnmodifiableCollectionsSerializer;

@Configuration
@SuppressWarnings("unused")
public class RedissonClientConfig {
    @Value("${symbol_dollar}{redis.mode:cluster}")
    private String mode;
    @Value("${symbol_dollar}{redis.datasource.url}")
    private String nodeAddress;
    @Value("${symbol_dollar}{redis.datasource.password}")
    private String password;
    @Value("${symbol_dollar}{redis.datasource.timeout}")
    private Integer timeout;

    @Bean(destroyMethod="shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config().setCodec(codec());

        if ("single".equalsIgnoreCase(mode)) {
            config.useSingleServer()
                    .setAddress(nodeAddress)
                    .setPassword(password)
                    .setTimeout(timeout);
        } else {
            config.useClusterServers()
                    .addNodeAddress(nodeAddress)
                    .setPassword(password)
                    .setTimeout(timeout);
        }
        return Redisson.create(config);
    }

    private Codec codec() {
        return new Kryo5Codec() {
            protected Kryo createKryo(ClassLoader classLoader) {
                Kryo kryo = super.createKryo(classLoader);
                UnmodifiableCollectionsSerializer.registerSerializers(kryo);
                return kryo;
            }
        };
    }
}
