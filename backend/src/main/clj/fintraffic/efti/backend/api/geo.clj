(ns fintraffic.efti.backend.api.geo
  (:require
    [fintraffic.efti.backend.api.route :as api-route]
    [fintraffic.efti.schema.geo :as geo-schema]
    [fintraffic.efti.backend.service.geo :as geo-service]))

(def routes
  [(api-route/find-all "countries" geo-schema/Country geo-service/find-all-countries)
   (api-route/find-all "languages" geo-schema/Language geo-service/find-all-languages)])
