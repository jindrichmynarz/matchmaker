(ns matchmaker.core.sparql
  (:require [matchmaker.lib.sparql :as sparql]))

; Private functions

(defn- match-resource
  "Matches resource using SPARQL query generated from @template-path by using @data."
  [config template-path & {:keys [data]
                           :or {data {}}}]
  (let [additional-object-inhibition (-> config :matchmaker :sparql :additional-object-inhibition)
        limit (-> config :matchmaker :limit)
        additional-data {:additional-object-inhibition additional-object-inhibition
                         :limit limit}]
    (sparql/select-query config
                         template-path
                         :data (merge data additional-data))))

; Public functions

(defn match-contract-exact-cpv
  "Match @contract using exact CPV matches SPARQL query."
  [config contract]
  (match-resource config
                  ["matchmaker" "sparql" "contract" "to" "business_entity" "exact_cpv"]
                  :data {:contract contract}))

(defn match-contract-expand-to-narrower-cpv
  "Match @contract using CPV codes unidirectionally expanded to narrower concepts."
  [config contract]
  (match-resource config
                  ["matchmaker" "sparql" "contract" "to" "business_entity" "expand_to_narrower_cpv"]
                  :data {:contract contract}))

(defn match-business-entity-exact-cpv
  "Match @business-entity to relevant public contracts using exact CPV matches SPARQL query."
  [config business-entity]
  (match-resource config
                  ["matchmaker" "sparql" "business_entity" "to" "contract" "exact_cpv"]
                  :data {:business-entity business-entity}))

(defn match-business-entity-expand-to-narrower-cpv
  "Match @business-entity to relevant public contracts using matches through CPV codes
  expanded to more specific concepts."
  [config business-entity]
  (match-resource config
                  ["matchmaker" "sparql" "business_entity" "to" "contract" "expand_to_narrower_cpv"]
                  :data {:business-entity business-entity}))
