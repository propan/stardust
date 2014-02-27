(defproject stardust "0.1.0-SNAPSHOT"
  :description "a multiplayer variation of Asteroids"
  :url "http://github.com/propan/stardust"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {:builds {:dev
                       {:source-paths ["src"]
                        :compiler {:output-to     "out/stardust.js"
                                   :output-dir    "out"
                                   :optimizations :none
                                   :source-map    true}}
                       :prod
                       {:source-paths ["src"]
                        :compiler {:output-to     "out/stardust.min.js"
                                   :optimizations :advanced
                                   :pretty-print  false}}}})
