(defproject thrift-gen "0.1.0-SNAPSHOT"
  :description "quickcheck generators for producing instances of Thrift schemas"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/test.check "0.5.9"]
                 [org.flatland/useful "0.11.1"]
                 [org.apache.thrift/libthrift "0.6.1"]])
