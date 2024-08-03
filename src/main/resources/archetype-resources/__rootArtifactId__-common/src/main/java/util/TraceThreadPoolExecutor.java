#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import org.apache.commons.collections4.MapUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@SuppressWarnings("unused")
public class TraceThreadPoolExecutor extends ThreadPoolExecutor {

    public TraceThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit milliseconds, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, milliseconds, workQueue);
    }

    public TraceThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit milliseconds, LinkedBlockingQueue<Runnable> workQueue, ThreadFactory factory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, milliseconds, workQueue, factory);
    }

    @Override
    public void execute(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException();
        }

        String traceId = TraceUtils.getTraceId();
        Map<String, String> traceAttributes = TraceUtils.getTraceAttributes();
        super.execute(() -> {
            TraceUtils.startTrace(traceId);
            if (MapUtils.isNotEmpty(traceAttributes)) {
                traceAttributes.forEach(TraceUtils::putTraceAttribute);
            }
            try {
                runnable.run();
            } finally {
                TraceUtils.endTrace();
            }
        });
    }
}
