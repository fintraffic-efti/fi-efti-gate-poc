(ns fintraffic.efti.backend.schema.role)

(def admin-id 2)

(defn public? [whoami] (nil? whoami))

(defn admin? [{:keys [role-id]}]
  (= role-id admin-id))

(def system-users
  {:database 0
   :conversion -1
   :cognito -2})

