(ns matchmaker.data-synchronization.sparql-extractor
  (:gen-class)
  (:require [taoensso.timbre :as timbre]
            [matchmaker.lib.sparql :as sparql]
            [matchmaker.lib.rdf :as rdf]
            [matchmaker.lib.util :as util]
            [matchmaker.common.config :refer [->Config]]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as component])
  (:import [com.github.jsonldjava.utils JsonUtils]))

(declare get-contract-description save-json-ld)

; ----- Private functions -----

(defn- etl-contract
  "Extract @contract from SPARQL endpoint using @sparql-extractor,
  transform to framed JSON-LD and save it to filesystem to @dir-path."
  [sparql-extractor dir-path contract]
  (let [contract-uri (:contract contract)
        sha1-hash (util/sha1 contract-uri)
        file-path (util/join-file-path dir-path (str sha1-hash ".jsonld"))]
    (when-not (.exists (clojure.java.io/as-file file-path))
      (some->> contract-uri
               (get-contract-description sparql-extractor)
               (spit file-path)))))

(defn- get-contract-description
  "Get framed JSON-LD representation of @contract."
  [sparql-extractor contract]
  (let [turtle (sparql/construct-query (:sparql-endpoint sparql-extractor)
                                       ["data_synchronization" "resource_description"]
                                       :data {:resource contract})
        model (rdf/string->graph turtle)
        context (get-in sparql-extractor [:sparql-extractor :contract-context])
        frame (get-in sparql-extractor [:sparql-extractor :contract-frame])]
    (when-not (zero? (.size model))
      (let [json-ld (JsonUtils/fromString (rdf/graph->string model :rdf-syntax "JSONLD"))
            framed-json-ld (rdf/frame-json-ld json-ld frame)
            triples (.get framed-json-ld "@graph")]
        (when (seq triples)
          (JsonUtils/toPrettyString (first triples)))))))

; ----- Records -----

(defrecord SparqlExtractor []
  component/Lifecycle
  (start [sparql-extractor]
    (let [contract-frame (util/parse-json-resource "public/jsonld_contexts/contract_frame.jsonld")
          contract-context (util/parse-json-resource "public/jsonld_contexts/contract.jsonld")]
      (assoc sparql-extractor
             :contract-frame contract-frame
             :contract-context contract-context)))
  (stop [sparql-extractor] sparql-extractor))
  
; ----- Public functions -----

(defn load-contracts
  "Retrieve lazy sequence of contracts."
  [sparql-extractor]
  (sparql/select-query-unlimited (:sparql-endpoint sparql-extractor)
                                 ["data_synchronization" "get_contract_uris"])) 

(defn load-extractor
  "Start a SPARQL extractor system."
  []
  (component/start
    (component/system-map :config (->Config (:matchmaker-config env))
                          :sparql-endpoint (component/using (sparql/->SparqlEndpoint) [:config])
                          :sparql-extractor (component/using (->SparqlExtractor) [:sparql-endpoint]))))

(defn bulk-download
  "Bulk download of contracts in JSON-LD using @sparql-extractor.
  The downloaded files are saved to @dir-path."
  [sparql-extractor dir-path]
  {:pre [(.exists (clojure.java.io/as-file dir-path))]}
  (dorun (map (partial etl-contract sparql-extractor dir-path)
              (load-contracts sparql-extractor))))

(defn -main
  [& args]
  (let [extractor (load-extractor)]
    (bulk-download extractor (first args))))
