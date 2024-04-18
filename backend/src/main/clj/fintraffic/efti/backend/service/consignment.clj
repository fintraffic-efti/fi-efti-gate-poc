(ns fintraffic.efti.backend.service.consignment
  (:require [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [fintraffic.common.xml :as fxml]
            [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.dml :as dml]
            [fintraffic.efti.backend.db.query :as db-query]
            [fintraffic.efti.backend.db.query]
            [fintraffic.efti.backend.exception :as exception]
            [fintraffic.efti.backend.service.edelivery :as edelivery]
            [fintraffic.efti.backend.service.edelivery :as edelivery-service]
            [fintraffic.efti.backend.service.edelivery.ws :as edelivery-ws-service]
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
(defn find-consignment-db [db uil]
  (->> (consignment-db/select-consignment db uil)
       (map db->consignment)
       first))

(defn find-consignment-gate [db config query]
  (let [conversation-id (edelivery-service/new-conversation-id db)
        request (edelivery-ws-service/send-find-consignment-message! db config conversation-id query)
        response (edelivery-service/find-messages-until db conversation-id (complement empty?) 60000)]
    (if (empty? response)
      (exception/throw-ex-info! :timeout (str "Foreign gate " (:gate-id query)
                                              " did not respond within 60s. Request message id: "
                                              (:message-id request)))
      (-> response first :payload xml/parse-str fxml/element->sexp
          edelivery/xml->consignment))))

(defn find-platform-consignment [db uil]
  (when-let [consignment (find-consignment-db db uil)]
    (:body (http/get (str (->> consignment :platform-id Long/parseLong
                               (user-service/find-whoami-by-id db user-schema/Platform)
                               :platform-url)
                          "/consignments/" (:data-id uil))
                     {:as :json}))))

(defn find-consignment [db config query]
  (if (= (:gate-id config) (:gate-id query))
    (if (nil? (:dataset-id query))
      (find-consignment-db db query)
      (find-platform-consignment db query))
    (find-consignment-gate db config query)))

(def default-query-params
  {:limit      10
   :offset     0
   :vehicle-id nil})

(defn find-consignments-db [db query]
  (->> query
       (merge default-query-params)
       (consignment-db/select-consignments db)
       (map db->consignment)))

(defn find-consignments-gate [db config query]
  (let [conversation-id (edelivery-service/new-conversation-id db)
        gate-ids (:gate-ids config)]
    (doseq [to-id gate-ids]
      (edelivery-ws-service/send-find-consignments-message! db config conversation-id to-id query))
    (->>
      (edelivery-service/find-messages-until db conversation-id #(= (count %) (count gate-ids)) 60000)
      (mapcat #(->> % :payload xml/parse-str fxml/element->sexp
                    (drop 2) (map edelivery/xml->consignment))))))

(defn find-consignments [db config query]
  (concat
    (find-consignments-db db query)
    (find-consignments-gate db config query)))