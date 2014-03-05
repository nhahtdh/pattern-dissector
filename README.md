PatternDissector
=================

## Motivation

Explore finer details of Java regex syntax and see how the regex is actually interpreted by the engine in the `Pattern` class.

Instead of *inferring* the meaning of a regex via the documentation (by contract), this allows us to directly *verify* how the regex is interpreted by the engine.

Since the start of this project (Feb 2014), I have found and filed 2 bug reports using this tool:

- https://bugs.openjdk.java.net/browse/JDK-8035076
- https://bugs.openjdk.java.net/browse/JDK-8033663

## Supported implementations

Currently, the dissector only works if the Java Virtual Machine (JVM) is configured to use the reference implementation of [Java Class Library](http://en.wikipedia.org/wiki/Java_Class_Library) (JCL), i.e. [OpenJDK](http://en.wikipedia.org/wiki/OpenJDK) JCL.

For the other 2 known JCLs:

- [GNU Classpath](http://en.wikipedia.org/wiki/GNU_Classpath) is currently **not supported**, but it may be supported in the future.
- [Apache Harmony](http://en.wikipedia.org/wiki/Apache_Harmony) is currently **not supported**, and there is no plan to support it, since the project has been retired since 2011.

To know what JCL is used or can be configured to use by your JVM, check out [Comparison of Java virtual machines](http://en.wikipedia.org/wiki/Comparison_of_Java_virtual_machines#Technical_information) on Wikipedia.
