(defproject br.bsb.liberdade.baas.api "0.3.0"
  :description "Backend as a Service API"
  :url "https://www.liberdade.bsb.br/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [compojure "1.7.1"]
                 [http-kit "2.8.0"]
                 [ring/ring-defaults "0.5.0"]
                 [org.clojure/data.json "2.5.0"]
                 [selmer "1.12.61"]
                 [buddy/buddy-sign "3.6.1-359"]
                 [org.clojars.liberdade/strint "0.0.1"]
                 [jumblerg/ring-cors "3.0.0"]
                 [com.github.seancorfield/next.jdbc "1.3.939"]
                 [org.xerial/sqlite-jdbc "3.46.1.0"]
                 [middlesphere/clj-compress "0.1.0"]
                 [clj-http "3.13.0"]]
  :min-lein-version "2.9.8"
  :main ^:skip-aot br.bsb.liberdade.baas.api
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :uberjar-name "br.bsb.liberdade.baas.api.jar"
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :plugins [[lein-ancient "1.0.0-RC3"]])

