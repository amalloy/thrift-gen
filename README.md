thrift-gen
==========

Generators for arbitrary instances of Thrift objects.

Create a thrift generator using just its Class object, as follows:

```java
Generator<MyThrift> g = new Generator<>(MyThrift.class);
List<? extends MyThrift> samples = g.buildSamples(10;
```

Now that you have 10 different valid objects that comply to your Thrift spec,
you can use them as test inputs, or just a convenient source of dummy inputs
to push around.
