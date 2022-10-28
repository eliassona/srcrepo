(ns srcrepo.core-test
  (:require [clojure.test :refer :all]
            [srcrepo.core :refer :all])
  (:import [srcrepo Codec]))

(deftest a-test
  (let [data []]
    (is (= data (Codec/decodeBA (Codec/encodeBA data)))))
  (let [data [(byte-array [1])]]
    (is (= data (Codec/decodeBA (Codec/encodeBA data)))))
  )
