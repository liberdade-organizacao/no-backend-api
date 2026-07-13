(ns br.bsb.liberdade.baas.validation
  (:require [clojure.string :as string]))

(defn validate-presence [value]
  (if (nil? value)
    "is required"
    nil))

(defn validate-string [value]
  (if (string? value)
    nil
    "must be a string"))

(defn validate-email [value]
  (let [email-regex #"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"]
    (if (and (string? value) (re-matches email-regex value))
      nil
      "must be a valid email address")))

(defn validate [schema params]
   (let [errors (reduce
                  (fn [acc [key validator-fn]]
                    (let [value (get params key)
                         error (validator-fn value)]
                      (if error
                        (assoc acc key error)
                       acc)))
                  {}
                 schema)]
      (if (empty? errors)
       params
        {:error "Validation Failed"
         :details errors})))

(defn validate-headers [req schema]
    (let [headers (reduce (fn [acc h] (assoc acc h (-> req :headers (get h)))) {} (keys schema))]
      (validate schema headers)))

(defn validate-query [req schema parse-fn]
    (let [params ((or parse-fn identity) (:query-string req))]
      (validate schema params)))
