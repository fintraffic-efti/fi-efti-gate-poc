(ns fintraffic.common.format
  (:import (java.text DecimalFormat DecimalFormatSymbols)
           (java.util Locale)))

(def locale (Locale/of "fi" "FI"))
(def decimal-format-symbol
  (doto (DecimalFormatSymbols. locale) (.setMinusSign \-)))
(def ^DecimalFormat decimal-format
  (doto (DecimalFormat. "#.###") (.setDecimalFormatSymbols decimal-format-symbol)))
(defn number [^Object number] (.format decimal-format number))

