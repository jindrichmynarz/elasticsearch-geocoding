(defproject elasticsearch-geocoding "0.1.0-SNAPSHOT"
  :description "Geocoding using Elasticsearch"
  :url "http://example.com/jindrichmynarz/elasticsearch-geocoding"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojurewerkz/elastisch "3.0.0-beta1"]
                 [prismatic/schema "1.1.3"]
                 [mount "0.1.10"]
                 [slingshot "0.12.2"]
                 [cheshire "5.6.3"]
                 [org.clojure/tools.cli "0.3.5"]
                 [commons-validator/commons-validator "1.5.1"]
                 [stencil "0.5.0"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/data.xml "0.0.8"]
                 [clj-http "3.3.0"]
                 [org.apache.jena/jena-core "3.1.1"]
                 [org.apache.jena/jena-arq "3.1.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [incanter/incanter-core "1.9.1"]
                 [incanter/incanter-charts "1.9.1"]]
  :main elasticsearch-geocoding.core
  :profiles {:uberjar {:aot :all
                       :uberjar-name "elasticsearch_geocoding.jar"}}
  :aliases {"missing" ["run" "-m" "elasticsearch-geocoding.missing"]})
