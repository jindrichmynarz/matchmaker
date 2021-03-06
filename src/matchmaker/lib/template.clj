(ns matchmaker.lib.template
  (:require [taoensso.timbre :as timbre]
            [matchmaker.lib.util :refer [join-file-path]]
            [clojure.java.io :as io]
            [stencil.core :refer [render-file]]))

; Private functions

(defn- get-template-path
  "Returns resource path for @template-path vector"
  [template-path]
  {:pre [(vector? template-path)]
   :post [((complement nil?) (io/resource %))]}
  (str (apply join-file-path (into ["templates"] template-path)) ".mustache"))

; Public functions

(defn render-template
  "Pass in a vector with @template-path (without its .mustache
  filename extension) and optionally key-value pairs of data for the template.

  Use: (render-template [path to template-file-name] :data {:key1 value1 :key2 value2})"
  [template-path & {:keys [data]}]
  (render-file (get-template-path template-path) data))

(defn render-sparql
  "Render SPARQL @template-path using @data with named graphs from config added automatically" 
  [config template-path & {:keys [data]}]
  (let [source-graph (get-in config [:data :source-graph])
        sample-graph (get-in config [:benchmark :sample :graph])
        merged-data (merge {:source-graph source-graph
                            :sample-graph sample-graph} data)]
    (render-template template-path
                     :data merged-data)))
