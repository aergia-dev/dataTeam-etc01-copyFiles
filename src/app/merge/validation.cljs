(ns app.merge.validation
  (:require  [re-frame.core :refer [subscribe dispatch]]
             [app.common.utils :refer [sort-result split-basefile-others]]
             [taoensso.timbre :refer [debug]]))

(defn validation-overlap [item1 item2]
  (debug (:start item1)  "," (:end item1)  "," (:start item2) "," (:end item2))
  (< (:start item1) (:end item1) (:start item2) (:end item2)))

(defn validation-min-max [item]
  (let [min (-> item second :start)
        max (-> item second :end)]
    (< min max)))

(defn validation-check [result]
  (let [partitioned (partition 2 1 result)
        min-max-result (map validation-min-max result)
        overlap-result (map (fn [[e1 e2]]
                              (validation-overlap (second e1) (second e2))) partitioned)
        result-msg (cond
                     (not (every? true? min-max-result)) (str "Not valid - start, end frame num is wrong -"  (map (fn [r m] (when (false? r)
                                                                                                                              (str (-> m second :alias)))) min-max-result result))
                     (not (every? true? overlap-result)) (str "Not valid - overlaped files- " (remove nil? (map (fn [r [e1 e2]] (when (false? r)
                                                                                                                                  (str (-> e1 second :alias) ", " (-> e2 second :alias)))) overlap-result partitioned)))
                     (= 0 (count result)) ""
                     :else "valid")]
    (dispatch [:validation result-msg])))


(defn validation []
  (let [result @(subscribe [:data])
        others  (:others (split-basefile-others result))]
    (validation-check (sort-result others))))

