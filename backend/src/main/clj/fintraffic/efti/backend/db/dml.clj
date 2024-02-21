(ns fintraffic.efti.backend.db.dml
  (:require
    [clojure.walk :as walk]
    [flathead.logic :as logic]
    [next.jdbc.sql.builder :as sql-builder]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :as sql]
    [fintraffic.efti.backend.db :as db]
    [fintraffic.efti.backend.exception :as exception]))

(defn for-update [table object where-keys options]
  (sql-builder/for-update
    table (apply dissoc object where-keys)
    (select-keys object where-keys)
    options))

(defn assert-update-count-1! [table where update]
  (assert (<= 0 update 1)
          (str "Invalid update to a table: " table ", where: " where
               "Invalid update count: " update))
  update)

(defn update-1! [connectable table key-map where-params opts]
  (->> (sql/update! connectable table key-map where-params opts)
       ::jdbc/update-count (assert-update-count-1! table where-params)))

(defn update-multi!
  "Execute a batch update for a sequence of objects."
  [connectable table objects where-keys options]
  (let [sqls (map #(for-update table (into (sorted-map) %) where-keys options) objects)]
    (assert (apply = (map first sqls))
            (str "Invalid batch update to a table: " table ", where: " where-keys
                 ". All objects must have the same keys so that the update sql for each object is the same."))
    (jdbc/execute-batch! connectable (ffirst sqls)
                         (map rest sqls) options)))

(defn update-multi-1!
  "Batch update that asserts that every object updates only a one row."
  [connectable table objects where-keys options]
  (->> (update-multi! connectable table objects where-keys options)
       (map (partial assert-update-count-1! table where-keys))))

(defn- concat-children-for-save [parents children-key parent-key other-parent-keys]
  (mapcat
    (fn [parent]
      (map (fn [child]
             (-> child
                 (assoc parent-key (:id parent))
                 (merge (select-keys parent other-parent-keys))))
           (children-key parent)))
    parents))

(defn assert-update! [children-key where-keys children index updated]
  (when (= updated 0)
    (exception/throw-ex-info!
      {:type :item-not-found
       :list children-key
       :identity (-> children (nth index) (select-keys where-keys))})))

(defn save-children! [db parents {:keys [child-table children-key parent-key
                                         other-parent-keys child->db]
                                  :or {child->db identity other-parent-keys []}}]
  (let [children (concat-children-for-save parents children-key parent-key other-parent-keys)
        new-children (filter #(-> % :id nil?) children)
        existing-children (filter #(-> % :id some?) children)
        where-keys (concat [:id parent-key] other-parent-keys)]
    (when-not (empty? existing-children)
      (->>
        (update-multi-1! db child-table (map child->db existing-children)
                         where-keys db/default-opts)
        (map-indexed (partial assert-update! children-key where-keys existing-children))
        doall))
    (if (empty? new-children)
      children
      (concat
        (map
          #(assoc %1 :id (:id %2))
          new-children
          (sql/insert-multi!
            db child-table
            (map (comp child->db #(dissoc % :id)) new-children)
            db/default-opts))
        existing-children))))

(defn all-id->nil [m]
  (walk/postwalk
    (logic/when*
      (every-pred map-entry? (comp #(= :id %) key))
      (constantly [:id nil]))
    m))

(defn update-by-id+error [update-by-id! find-by-id error]
  (fn [db id update]
    (if-not (= (->> (assoc update :id id)
                    (update-by-id! db)
                    first ::jdbc/update-count) 1)
      (when-let [object (find-by-id db id)]
        (exception/throw-ex-info! (error object update)))
      1)))

(defn update-by-id+conflict-error [update-by-id! find-by-id error]
  (update-by-id+error
    update-by-id! find-by-id
    (fn [object _update] (-> object error (assoc :type :update-conflict)))))

(defn update-by-id+invalid-state [update-by-id! find-by-id error]
  (update-by-id+error
    update-by-id! find-by-id
    (fn [object _update] (-> object error (assoc :type :invalid-state)))))
