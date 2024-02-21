(ns fintraffic.common.map
  (:require [clojure.string :as str]))

(defn map-keys [f m] (into {} (map (fn [[k, v]] [(f k) v]) m)))

(defn filter-keys [predicate m] (into {} (filter (fn [[k, _]] (predicate k)) m)))

(defn map-values [f m] (into {} (map (fn [[k, v]] [k (f v)]) m)))

(defn nil-values
  "Replace map values with nils."
  [m]
  (map-values (constantly nil) m))

(defn remove-nil-values [m]
  (into {}
        (remove #(nil? (val %)))
        m))

(defn submap? [m1 m2] (= m1 (select-keys m2 (keys m1))))

(defmacro bindings->map [& bindings]
  (into {} (map (fn [s] [(keyword (name s)) s]) bindings)))

(defn paths
  ([coll]
   (paths coll []))
  ([coll path]
   (reduce
    (fn [acc [k v]]
      (let [v (if (sequential? v)
                (into {} (map-indexed vector v))
                v)
            current-path (conj path k)]
        (if (map? v)
          (concat acc (paths v current-path))
          (conj acc current-path))))
    []
    coll)))

(defn dissoc-in [m ks]
  (if (> (count ks) 1)
    (if (map? (get-in m (butlast ks)))
      (update-in m (butlast ks) dissoc (last ks))
      m)
    (dissoc m (first ks))))

(defn contains-in? [m path]
  (not= (get-in m path ::undefined) ::undefined))

(defn join-key [& parts]
  (->> parts
       (map name)
       (str/join "-")
       keyword))