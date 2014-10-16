(ns thrift.gen
  (:require [clojure.test.check.generators :as gen])
  (:import (org.apache.thrift.meta_data FieldMetaData FieldValueMetaData
                                        StructMetaData EnumMetaData ListMetaData MapMetaData SetMetaData)
           (org.apache.thrift.protocol TType)
           (org.apache.thrift TFieldRequirementType TFieldIdEnum TUnion)
           (java.nio ByteBuffer))
  (:use flatland.useful.debug))

(set! *warn-on-reflection* true)

(defmacro bind-do [actions body]
  (if (empty? actions)
    body
    (let [[binding generator] (take 2 actions)]
      `(gen/bind ~generator
                 (fn [~binding]
                   (bind-do [~@(drop 2 actions)]
                     ~body))))))

(defn read-static-field [^Class class field-name]
  (.get (.getField class field-name) nil))

(declare struct-gen)

(defmulti primitive-generator (fn [^FieldValueMetaData desc]
                                (.type desc)))

(defmethod primitive-generator TType/BOOL [_] gen/boolean)
(defmethod primitive-generator TType/BYTE [_] gen/byte)
(defmethod primitive-generator TType/I16 [_] (gen/fmap short (gen/choose
                                                          Short/MIN_VALUE Short/MAX_VALUE)))
(defmethod primitive-generator TType/I32 [_] (gen/fmap int (gen/choose
                                                            Integer/MIN_VALUE Integer/MAX_VALUE)))
(defmethod primitive-generator TType/I64 [_] (gen/choose Long/MIN_VALUE Long/MAX_VALUE))
(defmethod primitive-generator TType/STRING [^FieldValueMetaData desc]
  (gen/fmap (if (.isBinary desc)
              (fn [^String s]
                (ByteBuffer/wrap (.getBytes s "UTF-8")))
              identity)
            gen/string))

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
  (primitive-generator desc))

(defn field-generator [^Class class union? make-copy]
  (fn [field-meta]
    (let [field-name (key field-meta)
          args #(object-array [field-name %])
          ^FieldMetaData desc (val field-meta)
          required (= TFieldRequirementType/REQUIRED (.requirementType desc))
          field-gen (generator (.valueMetaData desc))
          build-args (into-array Class [(type field-name) Object])]
      (if union?
        (let [constructor (.getDeclaredConstructor class build-args)]
          {:gen (gen/fmap (fn [field]
                            (.newInstance constructor (args field)))
                          field-gen)})
        (let [setter (.getMethod class "setFieldValue" build-args)
              set-field (fn [m v]
                          (.invoke setter m (args v)))]
          {:required required, :bind (fn [m]
                                       (gen/fmap (fn [field]
                                                   (doto (make-copy m)
                                                     (set-field field)))
                                                 field-gen))})))))

(defn struct-gen [^Class class]
  (let [union? (.isAssignableFrom TUnion class)
        meta (read-static-field class "metaDataMap")
        copy-method (.getMethod class "deepCopy" (into-array Class []))
        make-copy (fn [m] (.invoke copy-method m (object-array 0)))
        field-generators (map (field-generator class union? make-copy) meta)]
    (if union?
      (-> (gen/elements field-generators)
          (gen/bind :gen))
      (->> field-generators
           (reduce (fn [gen {:keys [required bind]}]
                     (let [include-gen (if required
                                         (gen/return true)
                                         gen/boolean)]
                       (bind-do [m gen,
                                 include include-gen]
                                (if-not include
                                  (gen/return m)
                                  (bind m)))))
                   (gen/return (.newInstance class)))))))
