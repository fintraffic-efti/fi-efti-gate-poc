(ns fintraffic.efti.schema.user
  (:require [fintraffic.efti.schema :as schema]
            [tick.core :as tick]))

(def User
  "Any user"
  {:id         schema/Id
   :role-id    (schema/ForeignKey :role)
   :last-name  (schema/maybe string?)
   :first-name (schema/maybe string?)})

(def SystemUser
  "Any system user"
  {:id      schema/Id
   :role-id (schema/ForeignKey :role)
   :name    (schema/maybe string?)})

(def Platform (assoc SystemUser
                     :id string?
                     :role-id string?
                     :platform-url (schema/maybe string?)))

(def CAUser (assoc SystemUser
                   :id string?
                   :role-id string?))

(def Role schema/Classification)

(def centuries
  {\A 2000 \B 2000 \C 2000
   \D 2000 \E 2000 \F 2000
   \Y 1900 \X 1900 \W 1900
   \V 1900 \U 1900 \- 1900
   \+ 1800})

(defn ssn-fi-checksum [txt]
  (some->
    txt parse-long (mod 31)
    (some->>
      (nth [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9
            \A \B \C \D \E \F \H \J \K \L
            \M \N \P \R \S \T \U \V \W \X \Y]))))

(defn valid-ssn-fi? [s]
  (and
    (= 11 (count s))
    (let [date-part (subs s 0 6)
          century-sign (nth s 6)
          individual-number (subs s 7 10)
          checksum (last s)]
      (and (contains? centuries century-sign)
           (= checksum (ssn-fi-checksum (str date-part individual-number)))))))

(defn ssn->birth-date
  [ssn]
  (when (valid-ssn-fi? ssn)
    (let [day   (parse-long (subs ssn 0 2))
          month (parse-long (subs ssn 2 4))
          year  (+ (parse-long (subs ssn 4 6))
                   (centuries (nth ssn 6)))]
      (tick/new-date year month day))))

(def SSN-FI
  [:and
   string?
   [:fn
    {:title "FI social security number"
     :fintraffic.efti.schema/type :ssn-fi}
    valid-ssn-fi?]])

(def system
  {:database 0
   :authentication -1
   :edelivery -2})
