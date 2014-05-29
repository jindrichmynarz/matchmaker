(ns matchmaker.lib.rdf
  (:require [clojure.set :refer [union]])
  (:import [com.hp.hpl.jena.rdf.model ModelFactory]
           [com.github.jsonldjava.jena JenaJSONLD]))

(JenaJSONLD/init) ; Initialization of the JSON-LD library

; Private vars

(defonce ^{:private true}
  rdf-syntax-names-mappings
  {"TURTLE" #{"n3" "ttl" "turtle"}
   "JSON-LD" #{"jsonld" "json-ld"}})

; Public functions

(defn canonicalize-rdf-syntax-name
  "Converts name of @rdf-syntax into its canonical form.
  Raises an exception when provided with unavailable RDF syntax."
  [rdf-syntax]
  (let [normalized-syntax-name (-> rdf-syntax
                                   clojure.string/trim
                                   clojure.string/lower-case)
        canonical-syntax-name (for [[syntax aliases] rdf-syntax-names-mappings
                                    :when (aliases normalized-syntax-name)]
                                syntax)]
    (if-not (empty? canonical-syntax-name)
      (first canonical-syntax-name)
      (throw (IllegalArgumentException. (format "Invalid RDF syntax: %s" rdf-syntax))))))

(defn convert-syntax
  "Convert RDF @string from @input-syntax to @output-syntax."
  [string & {:keys [input-syntax output-syntax]
             :or {input-syntax "TURTLE"
                  output-syntax "TURTLE"}}]
  (let [canonical-input-syntax (canonicalize-rdf-syntax-name input-syntax)
        canonical-output-syntax (canonicalize-rdf-syntax-name output-syntax)]
    (if (= canonical-input-syntax canonical-output-syntax)
        string
        (-> string
            (string->graph :rdf-syntax canonical-input-syntax)
            (graph->string :rdf-syntax canonical-output-syntax)))))

(defn graph->string
  "Write RDF @graph to string serialized in @rdf-syntax (defaults to Turtle)."
  [graph & {:keys [rdf-syntax]
            :or {rdf-syntax "TURTLE"}}]
  (let [canonical-rdf-syntax-name (canonicalize-rdf-syntax-name rdf-syntax)
        output (java.io.ByteArrayOutputStream.)
        _ (.write graph output canonical-rdf-syntax-name)]
    (.toString output)))

(defn string->graph
  "Read @string containing RDF serialized in @rdf-syntax (defaults to Turtle) into RDF graph."
  [string & {:keys [rdf-syntax]
             :or {rdf-syntax "TURTLE"}}]
  ; TODO: Is adding "UTF-8" to .getBytes needed?
  (let [canonical-rdf-syntax-name (canonicalize-rdf-syntax-name rdf-syntax)
        input-stream (java.io.ByteArrayInputStream. (.getBytes string))]
    (.read (ModelFactory/createDefaultModel) input-stream nil canonical-rdf-syntax-name)))
