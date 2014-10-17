thrift-gen
==========

Generators for arbitrary instances of Thrift objects.

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
