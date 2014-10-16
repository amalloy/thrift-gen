package thrift.gen;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.AFn;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
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

    public Generator(Class<T> thriftClass, IPersistentMap customizations) {
        this.gen = generator.invoke(thriftClass, customizations);
    }

    @SuppressWarnings("unchecked")
    public List<? extends T> buildSamples(int numSamples) {
        return (List<T>)sample.invoke(gen, numSamples);
    }

    public static interface Customizer<T> {
        public void customize(T t);
    }

    public static class Builder<T> {
        public final Class<T> thriftClass;
        public final IPersistentMap customizations;

        public Generator<T> build() {
            return new Generator<T>(thriftClass, customizations);
        }

        public Builder(Class<T> thriftClass) {
            this(thriftClass, PersistentHashMap.EMPTY);
        }

        public Builder(Class<T> thriftClass, IPersistentMap customizations) {
            this.thriftClass = thriftClass;
            this.customizations = customizations;
        }

        @SuppressWarnings("unchecked")
        public <C> Builder<T> customize(Class<C> customClass,
                                        final Customizer<? super C> customizer) {
            return new Builder<T>(thriftClass, customizations.assoc(customClass,
              new AFn() {
                  public Object invoke(Object x) {
                      customizer.customize((C)x);
                      return null;
                  }
              }));
        }
    }
}
