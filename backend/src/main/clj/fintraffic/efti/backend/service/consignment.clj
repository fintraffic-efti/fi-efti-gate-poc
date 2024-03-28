(ns fintraffic.efti.backend.service.consignment
  (:require [clj-http.client :as http]
            [fintraffic.common.map :as map]
            [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.dml :as dml]
            [fintraffic.efti.backend.db.query :as db-query]
            [fintraffic.efti.backend.db.query]
            [fintraffic.efti.backend.service.user :as user-service]
            [fintraffic.efti.schema.consignment :as consignment-schema]
            [fintraffic.efti.schema.user :as user-schema]))

(db/require-queries 'consignment)

(defn save-consignment! [db _whoami uil consignment]
  (db/with-transaction
    [tx db]
    (let [[{:keys [id]}]
          (dml/upsert tx :consignment
                      [(merge uil (dissoc consignment :transport-vehicles))]
                      (keys consignment-schema/UIL)
                      db/default-opts)]
      (dml/upsert tx :transport-vehicle
                  (map-indexed #(assoc %2 :ordinal %1 :consignment-id id)
                               (:transport-vehicles consignment))
                  [:consignment-id :ordinal] db/default-opts))))

(def db->consignment (db-query/decoder consignment-schema/Consignment))
(defn find-consignment [db _whoami uil]
  (->> (consignment-db/select-consignment db uil)
       (map db->consignment)
       first))

(defn find-platform-consignment [db whoami uil]
  (when-let [consignment (find-consignment db whoami uil)]
    (:body (http/get (str (->> consignment :platform-id Long/parseLong
                               (user-service/find-whoami-by-id db user-schema/Platform)
                               :platform-url)
                          "/consignments/" (:data-id uil))))))

(def default-query-params
  (map/map-values (constantly nil) consignment-schema/ConsignmentQuery))

(defn find-consignments [db query]
  (->> query
       (merge default-query-params)
       (consignment-db/select-consignments db)
       (map db->consignment)))