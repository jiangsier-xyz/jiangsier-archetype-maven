#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PojoUtilsTest {
    @SuppressWarnings("unused")
    static class Entry {
        private final String v;
        private Demo host;
        private Entry next;

        public Entry(String v) {
            this.v = v;
        }

        public Entry setHost(Demo host) {
            this.host = host;
            return this;
        }

        public Entry setNext(Entry next) {
            this.next = next;
            return this;
        }
    }

    @SuppressWarnings("unused")
    static class Demo {
        private final List<Entry> l = new LinkedList<>();

        public Demo() {
            l.add(new Entry("1").setHost(this)
                    .setNext(new Entry("12")
                            .setNext(new Entry("123")
                                    .setNext(new Entry("1234")
                                            .setNext(new Entry("12345")
                                                    .setNext(new Entry("123456")))))));
        }

        public List<Entry> getL() {
            return l;
        }
    }

    private String toPattern(String orig) {
        return orig.replaceAll("${symbol_escape}${symbol_escape}{", "${symbol_escape}${symbol_escape}${symbol_escape}${symbol_escape}{")
                .replaceAll("${symbol_escape}${symbol_escape}}", "${symbol_escape}${symbol_escape}${symbol_escape}${symbol_escape}}")
                .replaceAll("${symbol_escape}${symbol_escape}[", "${symbol_escape}${symbol_escape}${symbol_escape}${symbol_escape}[")
                .replaceAll("${symbol_escape}${symbol_escape}]", "${symbol_escape}${symbol_escape}${symbol_escape}${symbol_escape}]");
    }

    @Test
    public void testCircleAndDepth() {
        Demo demo = new Demo();
        String objInfo = PojoUtils.object2JsonString(demo, false);
        Pattern p = Pattern.compile(toPattern("{${symbol_escape}"l${symbol_escape}":[{${symbol_escape}"v${symbol_escape}":${symbol_escape}"1${symbol_escape}",${symbol_escape}"host${symbol_escape}":${symbol_escape}"Demo@${symbol_escape}${symbol_escape}w+${symbol_escape}",${symbol_escape}"next${symbol_escape}":{${symbol_escape}"v${symbol_escape}":${symbol_escape}"12${symbol_escape}",${symbol_escape}"host${symbol_escape}":null,${symbol_escape}"next${symbol_escape}":{${symbol_escape}"v${symbol_escape}":${symbol_escape}"123${symbol_escape}",${symbol_escape}"host${symbol_escape}":null,${symbol_escape}"next${symbol_escape}":{${symbol_escape}"v${symbol_escape}":${symbol_escape}"1234${symbol_escape}",${symbol_escape}"host${symbol_escape}":null,${symbol_escape}"next${symbol_escape}":{${symbol_escape}"v${symbol_escape}":${symbol_escape}"12345${symbol_escape}",${symbol_escape}"host${symbol_escape}":null,${symbol_escape}"next${symbol_escape}":${symbol_escape}"Entry@${symbol_escape}${symbol_escape}w+${symbol_escape}"}}}}}]}"));
        Matcher m = p.matcher(objInfo);
        Assert.assertTrue(m.matches());
    }
}
