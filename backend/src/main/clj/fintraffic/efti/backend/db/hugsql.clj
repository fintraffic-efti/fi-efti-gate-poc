(ns fintraffic.efti.backend.db.hugsql
  (:require [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.query :as db-query]))

(defn columns
  ([malli-schema] (columns malli-schema {}))
  ([malli-schema {:keys [remove-columns] :as options
                  :or {remove-columns #{}}}]
   (->>
     (db-query/schema->columns malli-schema options)
     (map (:column-fn db/default-opts))
     (map name)
     (remove (set remove-columns)))))
