(ns thrift.gen
  (:require [clojure.test.check.generators :as gen])
  (:import (org.apache.thrift.meta_data FieldMetaData FieldValueMetaData
                                        StructMetaData EnumMetaData ListMetaData MapMetaData SetMetaData)
           (org.apache.thrift.protocol TType)
           (org.apache.thrift TFieldRequirementType)
           clojure.lang.Reflector
           (java.lang.reflect Method Field))
  (:use flatland.useful.debug))

(set! *warn-on-reflection* true)

(defn static-field [^Class class field-name]
  (.get (.getField class field-name) nil))

(declare struct-gen)

(defmulti primitive-generator identity)

(defmethod primitive-generator TType/BOOL [_] gen/boolean)
(defmethod primitive-generator TType/BYTE [_] gen/byte)
(defmethod primitive-generator TType/I16 [_] (gen/choose Short/MIN_VALUE Short/MAX_VALUE))
(defmethod primitive-generator TType/I32 [_] (gen/choose Integer/MIN_VALUE Integer/MAX_VALUE))
(defmethod primitive-generator TType/I64 [_] (gen/choose Long/MIN_VALUE Long/MAX_VALUE))
(defmethod primitive-generator TType/STRING [_] gen/string)

(defmulti generator class)

(defmethod generator StructMetaData [^StructMetaData desc]
  (struct-gen (.structClass desc)))

(defmethod generator ListMetaData [^ListMetaData desc]
  (gen/list (generator (.elemMetaData desc))))

(defmethod generator SetMetaData [^SetMetaData desc]
  (gen/fmap set (gen/list (generator (.elemMetaData desc)))))

(defmethod generator MapMetaData [^MapMetaData desc]
  (gen/map (generator (.keyMetaData desc))
           (generator (.valueMetaData desc))))

(defmethod generator EnumMetaData [^EnumMetaData desc]
  (let [c (.enumClass desc)]
    (gen/elements (.getEnumConstants c))))

;; primitives all share a single class
(defmethod generator :default [^FieldValueMetaData desc]
  (primitive-generator (.type desc)))

(defn struct-gen [^Class class]
  (let [meta (static-field class "metaDataMap")]
    (reduce (fn [gen field-meta]
              (let [field-name (key field-meta)
                    ^FieldMetaData desc (val field-meta)
                    required (= TFieldRequirementType/REQUIRED (.requirementType desc))
                    include-gen (if required
                                  (gen/return true)
                                  gen/boolean)
                    field-gen (generator (.valueMetaData desc))
                    copy-method (.getMethod class "deepCopy" (into-array Class []))
                    [setter] (Reflector/getMethods class 2 "setFieldValue" false)
                    set-field (fn [m k v]
                                (?! [setter m k v])
                                (.invoke ^Method setter m (object-array [k v])))]
                (gen/bind gen
                          (fn [m]
                            (gen/bind include-gen
                                      (fn [include?]
                                        (if-not include?
                                          (gen/return m)
                                          (gen/bind field-gen
                                                    (fn [field]
                                                      (let [copy (.invoke copy-method m
                                                                          (object-array 0))]
                                                        (set-field copy field-name field)
                                                        (gen/return copy)))))))))))
            (gen/return (.newInstance class))
            meta)))
