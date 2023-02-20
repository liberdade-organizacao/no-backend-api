(defproject br.bsb.liberdade.baas.api "0.1.3"
  :description "Backend as a Service API"
  :url "https://www.liberdade.bsb.br/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [compojure "1.6.1"]
                 [http-kit "2.3.0"]
                 [ring/ring-defaults "0.3.2"]
                 [org.clojure/data.json "0.2.6"]
                 [selmer "1.12.40"]
                 [buddy/buddy-sign "3.4.1"]
                 [org.clojars.liberdade/strint "0.0.1"]
                 [jumblerg/ring-cors "2.0.0"]
                 [com.github.seancorfield/next.jdbc "1.2.753"]
                 [org.postgresql/postgresql "42.2.10"]
		 [middlesphere/clj-compress "0.1.0"]
		 [clj-http "3.12.3"]]
  :min-lein-version "2.9.8"
  :main ^:skip-aot br.bsb.liberdade.baas.api
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :uberjar-name "br.bsb.liberdade.baas.api.jar"
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})

