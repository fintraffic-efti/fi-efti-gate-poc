eDelivery xsd schemas v0.5
===

This contains edelivery xsd schema proposals for edelivery message payloads in 
Gate to Gate communication (v0.5).

- edelivery.xsd - edelivery message payloads in gate to gate communication
  - identifier-query / identifier-response
  - uil-query / uil-response
- consignment-dummy.xsd - dummy efti common dataset consignment
- consignment-common.xsd - efti common dataset consignment
- consignment-identifier.xsd - identifier subset of efti common dataset consignment. 
  - Data model is based on https://vocabulary.uncefact.org/about
  - Element names are derived using:
```clojure
(defn property-name [domain-name property]
  (-> property                                  ;; json-ld property data
      rdfs-label                                ;; use rdfs label from property data
      (replace-case-insensitive domain-name "") ;; remove redundant domain name from property name if it exists in name
      lower-first))                             ;; change first character to lowercase
```
- examples - xml example documents
  - consignment.xml - example consignment xml document (identifier subset of efti common dataset)
  - identifier-query - example payload for identifiers search request to other gate
  - identifier-response - example payload for identifiers search response to other gate
  - uil-query - example payload for uil query request to other gate
  - uil-response - example payload for uil query response to other gate