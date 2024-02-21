(ns fintraffic.efti.backend.service.user
  (:require [fintraffic.common.maybe :as maybe]
            [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.query :as db-query]
            [fintraffic.efti.backend.exception :as exception]
            [fintraffic.efti.backend.schema.user :as user-schema]
            [next.jdbc.sql :as sql]))

;; *** Require sql functions ***
(db/require-queries 'user)

(def find-roles #(db-query/find-all % :role user-schema/Role))

(defn- only-first! [query users]
  (when-not (empty? (rest users))
    (exception/throw-ex-info! {:type :whoami-duplicate
                               :message "Resolving whoami failed. More than one user matched the whoami query."
                               :users users
                               :query query}))
  (first users))

(defn find-whoami [db query]
  (some->> (merge {:email nil
                   :ssn nil}
                  query)
           (user-db/select-whoami db)
           (only-first! query)))

(defn find-whoami-by-id [db id]
  (first (user-db/select-whoami-by-id db {:id id})))

(defn find-users [db] (user-db/select-users db))

(defn update-user! [db id user]
  (sql/update! db :end-user user {:id id}
               db/default-opts)
  id)

(defn unique-field! [user]
  (maybe/require-some!
    "An external unique property is missing. Define either ssn or email."
    (condp #(-> %2 %1 some?) user
      :ssn "ssn"
      :email "email"
      nil)))

(defn add-user! [db user]
  (->
    (user-db/upsert-end-user!
      db (assoc user :unique-field (unique-field! user)))
    first :id))
