(ns fintraffic.efti.backend.api.user
  (:require [ring.util.response :as r]
            [clojure.set :as set]
            [fintraffic.efti.schema.user :as user-schema]
            [fintraffic.efti.backend.service.user :as user-service]
            [malli.experimental.lite :as lmalli]))

(defn whoami [schema]
  ["/whoami"
   {:get {:summary   "Find current signed in user"
          :responses {200 {:body schema}}
          :handler   (fn [{:keys [whoami]}]
                       (r/response (-> whoami
                                       (select-keys [:id :role-id])
                                       (update :id str)
                                       (update :role-id str)
                                       (set/rename-keys {:id (if (= schema
                                                                    user-schema/WhoamiPlatformResponse)
                                                               :platform-id
                                                               :app-id)
                                                         :role-id :role}))))}}])

(def routes
  [whoami
   ["/roles"
    {:get {:summary   "Find all user roles"
           :responses {200 {:body (lmalli/vector user-schema/Role)}}
           :handler   (fn [{:keys [db]}]
                        (r/response (user-service/find-roles db)))}}]
   ["/users"
    {:get {:summary    "Find all users"
           :responses  {200 {:body (lmalli/vector user-schema/User)}}
           :handler    (fn [{:keys [db]}]
                         (r/response (user-service/find-users db)))}}]])
