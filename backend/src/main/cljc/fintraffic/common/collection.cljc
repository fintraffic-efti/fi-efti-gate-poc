(ns fintraffic.common.collection)

(defn associate-by
  "Like group-by, but the value of each key will be a single value instead of a vector."
  ([key-fn coll]
   (zipmap (map key-fn coll) coll))
  ([key-fn value-fn coll]
   (zipmap (map key-fn coll) (map value-fn coll))))

(defn find-by [value keyfn coll]
  (first (filter #(= value (keyfn %)) coll)))

(defn find-by-id [id coll]
  (find-by id :id coll))

(defn contains-in? [m ks]
  (not= ::absent (get-in m ks ::absent)))

(defn update-in-if-contains
  "Like update-in, but if any levels do not exist, returns the passed in map unmodified.
  FROM: https://stackoverflow.com/a/26059795"
  [m ks f & args]
  (if (contains-in? m ks)
    (apply (partial update-in m ks f) args)
    m))

(defn find-indices [predicate sequence]
  (->> sequence
       (keep-indexed #(when (predicate %2) %1))))

(defn find-index [predicate sequence]
  (first (find-indices predicate sequence)))

(defn conjv [collection item]
  (conj (vec collection) item))

(defn one-value-not-nil?
  "Given a map returns true if at least one value is something other than a nil or a map"
  [m]
  (some (fn [[_ val]]
          (if (map? val)
            (one-value-not-nil? val)
            (some? val)))
        m))

(defn single
  "When size of collection is one, returns the only value."
  [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn map-single
  "If collection maps into a single unique value, returns that value."
  [f coll]
  (let [mapped (map f coll)
        result (first mapped)]
    (when (every? #(= % result) mapped)
      result)))

(defn pad
  [n col val]
  (take n (concat col (repeat val))))

(defn reducer-empty->nil [f] (fn ([] nil) ([acc val] (f acc val))))

(defn next-id
  ([rows]
   (next-id :ext-id rows))
  ([key-fn rows]
   (->> rows
        (map key-fn)
        (reduce max 0)
        inc)))
