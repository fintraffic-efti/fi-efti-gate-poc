(ns fintraffic.efti.backend.db.dml
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [fintraffic.efti.backend.exception :as exception]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :as sql]
    [next.jdbc.sql.builder :as sql-builder]))

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

(defn upsert [connectable table objects conflict-keys
              {:keys [column-fn] :as options
               :or {column-fn identity}}]
  (let [conflict-clause (str " on conflict ("
                             (sql-builder/as-cols conflict-keys options)
                             ") do update set ")
        update-keys (-> objects first keys set (set/difference conflict-keys))
        update #(str % " = excluded." %)
        update-clause (str/join ", " (map (comp update name column-fn) update-keys))]
    (sql/insert-multi! connectable table objects
                       (assoc options :suffix (str conflict-clause update-clause)))))

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
