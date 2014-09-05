package thrift.gen;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import java.util.List;

public class Generator<T> {
    public static final IFn generator;
    public static final IFn sample;
    static {
        Clojure.var("clojure.core", "require").invoke(Clojure.read("thrift.gen"));
        generator = Clojure.var("thrift.gen", "struct-gen");
        sample = Clojure.var("clojure.test.check.generators", "sample");
    }

    public final Object gen;

    public Generator(Class<T> thriftClass) {
        this.gen = generator.invoke(thriftClass);
    }

    @SuppressWarnings("unchecked")
    public List<? extends T> buildSamples(int numSamples) {
        return (List<T>)sample.invoke(gen, numSamples);
    }
}
