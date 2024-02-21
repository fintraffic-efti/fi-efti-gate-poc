(ns fintraffic.efti.backend.service.geo
  (:require [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.query :as db-query]
            [fintraffic.efti.backend.schema.geo :as geo-schema]))

; *** Require sql functions ***
(db/require-queries 'geo)

(defn find-all-countries [db] (geo-db/select-countries db))
(def find-all-languages #(db-query/find-all % :language geo-schema/Language))
