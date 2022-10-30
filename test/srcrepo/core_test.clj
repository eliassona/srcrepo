(ns srcrepo.core-test
  (:require [clojure.test :refer :all]
            [srcrepo.core :refer :all])
  (:import [srcrepo Codec]))

(deftest a-test
  (let [data []]
    (is (= data (Codec/decodeBA (Codec/encodeBA data)))))
  (let [data [["AName" (byte-array [2 4])]["BName" (byte-array [3 5])]]
        d-data (Codec/decodeBA (Codec/encodeBA data))]
    (is (= 2 (count d-data)))
    (is (= "AName" (-> d-data first first)))
    (is (= 2 (-> d-data first second first)))
    (is (= 4 (-> d-data first second second)))

    (is (= "BName" (-> d-data second first)))
    (is (= 3 (-> d-data second second first)))
    (is (= 5 (-> d-data second second second)))
    
    )
  )

(deftest test-encode-size
  (let [ba (byte-array 4)
        v 65535]
    (is (= v (Codec/decodeBaSize (Codec/encodeBaSize v 0 ba) 0)))
    ))
