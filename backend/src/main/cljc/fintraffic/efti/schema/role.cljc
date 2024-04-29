(ns fintraffic.efti.schema.role)

(def admin 2)
(def ca 1)
(def platform 0)

(defn public? [whoami] (nil? whoami))

(defn admin? [{:keys [role-id]}]
  (= role-id admin))

(defn ca? [{:keys [role-id]}]
  (= role-id ca))

(defn platform? [{:keys [role-id]}]
  (= role-id platform))

(def system-users
  {:database 0
   :authentication -1})

