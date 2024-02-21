(ns fintraffic.common.cert
  (:require [clojure.string :as str])
  (:import (java.io StringReader)
           (org.bouncycastle.openssl PEMParser)
           (org.bouncycastle.openssl.jcajce JcaPEMKeyConverter)
           (org.bouncycastle.asn1.pkcs PrivateKeyInfo)
           (org.bouncycastle.cert.jcajce JcaX509CertificateConverter)
           (org.bouncycastle.jce.provider BouncyCastleProvider)))

(defn trim-base64-string
  "Trim header/footer lines and whitespace from base64 encoded string."
  [s]
  (-> s
      (str/replace #"-+[A-Za-z ]+-+" "")
      (str/replace #"\s" "")))

(defn string->private-key
  "Load private key in base64 encoded PEM format."
  [s]
  (with-open [reader (StringReader.
                       (str "-----BEGIN PRIVATE KEY-----\n"
                            (trim-base64-string s)
                            "\n-----END PRIVATE KEY----"))]
    (let [pem-parser (PEMParser. reader)
          converter (doto (JcaPEMKeyConverter.)
                      (.setProvider (BouncyCastleProvider.)))
          private-key-info (PrivateKeyInfo/getInstance (.readObject pem-parser))]
      (.getPrivateKey converter private-key-info))))

(defn string->certificate
  "Load certificate in in base64 encoded PEM format."
  [s]
  (with-open [reader (StringReader.
                       (str "-----BEGIN CERTIFICATE-----\n"
                            (trim-base64-string s)
                            "\n-----END CERTIFICATE----"))]
    (let [pem-parser (PEMParser. reader)
          converter (doto (JcaX509CertificateConverter.)
                      (.setProvider (BouncyCastleProvider.)))]
      (.getCertificate converter (.readObject pem-parser)))))

(defn load-private-key [^String path]
  (-> path slurp string->private-key))

(defn load-certificate [^String path]
  (-> path slurp string->certificate))
