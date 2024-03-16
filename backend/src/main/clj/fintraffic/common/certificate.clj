(ns fintraffic.common.certificate
  (:refer-clojure :exclude [read])
  (:require [clojure.java.io :as io])
  (:import (java.security.cert CertificateFactory X509Certificate)))

(defn ^X509Certificate read [resource]
  (let [factory ^CertificateFactory (CertificateFactory/getInstance "X509")]
    (->> resource io/input-stream (.generateCertificate factory))))
