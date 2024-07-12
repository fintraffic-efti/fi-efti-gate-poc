(ns fintraffic.efti.backend.service.edelivery-test
  (:require [clojure.test :refer [deftest is testing]]
            [fintraffic.efti.backend.service.edelivery :as edelivery]
            [fintraffic.efti.backend.service.edelivery.ws :refer [validate-edelivery-payload!]]))

(deftest uil-query-xml
  (testing "Message is valid xml"
    (let [payload (edelivery/uil-query->xml {:gate-id "fi1"
                                             :platform-id "test-1"
                                             :dataset-id "1"
                                             :subset-id "identifiers"})]
      (is (string? payload))
      (validate-edelivery-payload! payload))))

(deftest query-xml
  (testing "Message is valid xml"
    (let [payload (edelivery/query->xml {:identifier "abc-12"
                                         :limit 0
                                         :offset 0})]
      (is (string? payload))
      (validate-edelivery-payload! payload))))

(deftest uil-response
  (testing "Message is valid xml"
    (testing "Consignment under way"
      (let [payload (edelivery/uil-response {:uil
                                             {:gateId "TODO EFTI-361",
                                              :platformId "TODO EFTI-361",
                                              :datasetId "da253ebf-4577-476a-99df-4e5f6bc6b750"},
                                             :deliveryInformation nil,
                                             :carrierAcceptanceDateTime "2024-07-10T09:46:15Z",
                                             :deliveryTransportEvent {:actualOccurrenceDateTime nil},
                                             :mainCarriageTransportMovements
                                             [{:dangerousGoodsIndicator false,
                                               :transportModeCode 2,
                                               :usedTransportMeans
                                               {:identifier "RXCU", :registrationCountry {:id "DE"}}}],
                                             :utilizedTransportEquipments
                                             [{:categoryCode "T1",
                                               :identifier "IAYY",
                                               :sequenceNumeric 1,
                                               :registrationCountry {:id "FI"},
                                               :carriedTransportEquipments
                                               [{:identifier "61ZU", :sequenceNumeric 1}]}]})]
        (is (string? payload))
        (validate-edelivery-payload! payload)))

    (testing "Delivered Consignment"
      (let [payload (edelivery/uil-response {:uil
                                             {:gateId "TODO EFTI-361",
                                              :platformId "TODO EFTI-361",
                                              :datasetId "da253ebf-4577-476a-99df-4e5f6bc6b750"},
                                             :deliveryInformation "2024-07-11T10:46:15Z",
                                             :carrierAcceptanceDateTime "2024-07-10T09:46:15Z",
                                             :deliveryTransportEvent {:actualOccurrenceDateTime "2024-07-11T10:46:15Z"},
                                             :mainCarriageTransportMovements
                                             [{:dangerousGoodsIndicator false,
                                               :transportModeCode 2,
                                               :usedTransportMeans
                                               {:identifier "RXCU", :registrationCountry {:id "DE"}}}],
                                             :utilizedTransportEquipments
                                             [{:categoryCode "T1",
                                               :identifier "IAYY",
                                               :sequenceNumeric 1,
                                               :registrationCountry {:id "FI"},
                                               :carriedTransportEquipments
                                               [{:identifier "61ZU", :sequenceNumeric 1}]}]})]
        (is (string? payload))
        (validate-edelivery-payload! payload)))))

(deftest identifier-response
  (testing "Message is valid xml"
    (let [payload (edelivery/identifier-response
                   [{:main-carriage-transport-movements
                     [{:used-transport-means
                       {:registration-country {:id "DE"}, :identifier "RXCU"},
                       :dangerous-goods-indicator false,
                       :transport-mode-code 2}],
                     :utilized-transport-equipments
                     [{:carried-transport-equipments
                       [{:identifier "61ZU", :sequence-numeric 1}],
                       :category-code "T1",
                       :identifier "IAYY",
                       :registration-country {:id "FI"},
                       :sequence-numeric 1}],
                     :id 1,
                     :uil
                     {:gate-id "fi1",
                      :platform-id "-10",
                      :dataset-id "da253ebf-4577-476a-99df-4e5f6bc6b750"},
                     :carrier-acceptance-date-time #time/instant "2024-07-10T09:46:15Z",
                     :delivery-event {:actual-occurrence-date-time nil}}])]
      (is (string? payload))
      (validate-edelivery-payload! payload))))
