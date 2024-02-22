(ns fintraffic.efti.backend.service.consignment
  (:require [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.query :as query-db]
            [fintraffic.efti.schema.consignment :as consignment-schema]))

(db/require-queries 'consignment)

(defn save-consignment! [db _whoami uil consignment]
  (consignment-db/upsert-consignment! db (merge uil consignment)))

(defn find-consignment [db _whoami uil]
  (first (query-db/find-by db :consignment consignment-schema/Consignment uil)))