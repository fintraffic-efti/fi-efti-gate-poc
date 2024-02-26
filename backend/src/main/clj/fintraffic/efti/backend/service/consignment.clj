(ns fintraffic.efti.backend.service.consignment
  (:require [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.query :as query-db]
            [fintraffic.efti.schema.consignment :as consignment-schema]
            [clj-http.client :as http]))

(db/require-queries 'consignment)

(defn save-consignment! [db _whoami uil consignment]
  (consignment-db/upsert-consignment! db (merge uil consignment)))

(defn find-consignment [db _whoami uil]
  (first (query-db/find-by db :consignment consignment-schema/Consignment uil)))

(defn find-platform-consignment [db _whoami uil]
  (when-let [consignment (find-consignment db _whoami uil)]
    (:body (http/get (str (:platform-url consignment) "/consignments/:data-id")))))