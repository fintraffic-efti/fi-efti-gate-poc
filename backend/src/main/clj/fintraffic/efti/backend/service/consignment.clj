(ns fintraffic.efti.backend.service.consignment
  (:require [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [fintraffic.common.debug :as debug]
            [fintraffic.common.map :as map]
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
            [fintraffic.efti.schema.user :as user-schema]
            [flathead.flatten :as flat]))

(db/require-queries 'consignment)

(defn consignment->db [uil consignment]
  (as-> consignment %
        (dissoc % :main-carriage-transport-movements :utilized-transport-equipments)
        (assoc % :uil uil)
        (flat/tree->flat "$" %)))

(defn transport-equipment->db [consignment-id ordinal equipment]
  (as-> equipment %
        (dissoc % :carried-transport-equipments)
        (assoc % :ordinal ordinal :consignment-id consignment-id)
        (flat/tree->flat "$" %)))

(defn carried-transport-equipment->db [consignment-id
                                       equipment-ordinal
                                       ordinal
                                       equipment]
  (as-> equipment %
        (assoc % :transport-equipment-ordinal equipment-ordinal
                 :ordinal ordinal
                 :consignment-id consignment-id)
        (flat/tree->flat "$" %)))

(defn save-consignment! [db _whoami uil consignment]
  (db/with-transaction
    [tx db]
    (let [[{:keys [id]}]
          (dml/upsert tx :consignment
                      [(consignment->db uil consignment)]
                      [:uil$gate-id :uil$platform-id :uil$data-id]
                      db/default-opts)]
      (dml/upsert tx :transport-movement
                  (map-indexed #(assoc (flat/tree->flat "$" %2)
                                  :ordinal %1 :consignment-id id)
                               (:main-carriage-transport-movements consignment))
                  [:consignment-id :ordinal] db/default-opts)
      (dml/upsert tx :transport-equipment
                  (map-indexed #(transport-equipment->db id %1 %2)
                               (:utilized-transport-equipments consignment))
                  [:consignment-id :ordinal] db/default-opts)
      (dml/upsert tx :carried-transport-equipment
                  (flatten
                    (map-indexed
                      (fn [equipment-ordinal equipment]
                        (map-indexed
                          (partial carried-transport-equipment->db id equipment-ordinal)
                          (:carried-transport-equipments equipment)))
                      (:utilized-transport-equipments consignment)))
                  [:consignment-id :transport-equipment-ordinal :ordinal]
                  db/default-opts))))

(def db->consignment
  (comp (db-query/decoder consignment-schema/Consignment)
        db-query/flat->tree))

(defn find-consignment-db [db uil]
  (->> (consignment-db/select-consignment db uil)
       (map db->consignment)
       first))

(defn xml->consignments [consignments]
  (->> consignments (drop 2) (map edelivery/xml->consignment)))

(defn find-consignment-gate [db config query]
  (let [conversation-id (edelivery-service/new-conversation-id db)
        request (edelivery-ws-service/send-find-consignment-message! db config conversation-id query)
        response (edelivery-service/find-messages-until db conversation-id (complement empty?) 60000)]
    (if (empty? response)
      (exception/throw-ex-info! :timeout (str "Foreign gate " (:gate-id query)
                                              " did not respond within 60s. Request message id: "
                                              (:message-id request)))
      (-> response first :payload xml/parse-str fxml/element->sexp xml->consignments first))))

(defn find-platform-consignment [db uil]
  (when-let [consignment (find-consignment-db db uil)]
    (:body (http/get (str (->> consignment :uil :platform-id Long/parseLong
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
   :identifier nil})

(defn find-consignments-db [db query]
  (->> query
       (merge default-query-params)
       (consignment-db/select-consignments db)
       (map db->consignment)))

(defn find-consignments-gate [db config query]
  (let [query (merge {:limit 10 :offset 0} query)
        conversation-id (edelivery-service/new-conversation-id db)
        gate-ids (if (empty? (:gate-ids query)) (:gate-ids config)
                   (-> query :gate-ids set (disj (:gate-id config))))
        query (dissoc query :gate-ids)]
    (doseq [to-id gate-ids]
      (edelivery-ws-service/send-find-consignments-message! db config conversation-id to-id query))
    (->>
      (edelivery-service/find-messages-until db conversation-id #(= (count %) (count gate-ids)) 60000)
      (mapcat #(->> % :payload xml/parse-str fxml/element->sexp xml->consignments)))))

(defn find-consignments [db config query]
  (concat
    (if (or (empty? (:gate-ids query))
            (-> query :gate-ids set (contains? (:gate-id config))))
      (find-consignments-db db query) [])
    (find-consignments-gate db config query)))