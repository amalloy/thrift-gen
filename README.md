thrift-gen
==========

Generators for arbitrary instances of Thrift objects, with a Java API for getting sample objects, or a Clojure
function creating a [test.check][] generator.

Using from Clojure
==========

Create a thrift generator from a Thrift Class object, and then get samples from it using the functions in test.check:

```clojure
(ns my.ns 
  (:require [thrift.gen :as t]
            [clojure.test.check.generators :as gen]))
(def generator (t/struct-gen MyThrift))
(gen/sample generator 10) ;; returns 10 instances of MyThrift
```

## Customization
Sometimes your Thrift schema doesn't capture all the domain rules that are required to make your
objects "valid": for example, maybe you have an i32 representing a number of CPU cores. Thrift is
perfectly happy to encode -9 cores, or 38123 cores, but your tests are not willing to accept such
absurd values. In that case, you can customize the thrift generators in a post-processing step,
specific to any particular thrift class, by passing in an additional map to the `struct-gen` function:
the keys in this map are Thrift classes, and the values are functions for mutating objects of that
class to make them fit your requirements. For example:

```clojure
(ns my.ns 
  (:require [thrift.gen :as t]
            [clojure.test.check.generators :as gen]))
(def generator (t/struct-gen MyThrift {CPUSettings (fn [settings]
                                                     (let [cores (.getCores settings)]
                                                       (.setCores settings
                                                                  (rem (if (neg? cores) (- cores) cores)
                                                                       64))))}))
(gen/sample generator 10) ;; returns 10 instances of MyThrift, with CPU cores between 0 and 63
```

Using from Java
=========
Create a thrift generator using just its Class object, as follows:

```java
Generator<MyThrift> g = new Generator<>(MyThrift.class);
List<? extends MyThrift> samples = g.buildSamples(10);
```

Now that you have 10 different valid objects that comply to your Thrift spec,
you can use them as test inputs, or just a convenient source of dummy inputs
to push around.

## Customization

Sometimes your Thrift schema doesn't capture all the domain rules that are required to make your
objects "valid": for example, maybe you have an i32 representing a number of CPU cores. Thrift is
perfectly happy to encode -9 cores, or 38123 cores, but your tests are not willing to accept such
absurd values. In that case, you can customize the thrift generators in a post-processing step,
specific to any particular thrift class:

```java
Generator<MyThrift> g = new Generator.Builder<>(MyThrift.class)
                          .customize(CPUSettings.class, new Generator.Customizer<>() {
                            public void customize(CPUSettings s) {
                              int cores = s.getCores()
                              if (cores < 0) cores = -cores;
                              s.setCores(cores % 64);
                            }
                           })
                          .customize(BuildConfig.Class, new Generator.Customizer<>() {
                            public void customize(BuildConfig conf) {
                              conf.setBuildName("TestBuild");
                            }
                           }
                          .build();
```

[test.check]: https://github.com/clojure/test.check
