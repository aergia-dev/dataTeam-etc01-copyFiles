(ns app.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [app.db :refer [default-db]]
            [taoensso.timbre :refer [debug info error fatal]]))

(reg-event-db
 :init-db
 (fn [_ _]
   (prn (merge {} default-db))
   (merge {} default-db)))

(reg-event-db
 :files
 (fn [db files]
   (assoc db :files files)))

(reg-event-db
 :add-data
 (fn [db [_ [k v]]]
   (assoc-in db [:data k] v)))

(reg-event-db
 :clear-data
 (fn [db _]
   {}))


(reg-event-db
 :validation
 (fn [db [_ v]]
   (assoc db :validation v)))

(reg-event-db
 :modify-frame-num
 (fn [db [_ k target v]]
   (let [ori (get-in db [:data k])
         start (if (= target :start)
                 v
                 (get-in db [:data k :start]))
         end (if (= target :end)
               v
               (get-in db [:data k :end]))
         filtered-data (filter (fn [e]
                                 (let [frame-num (:frame e)]
                                   (and (<= start frame-num)
                                        (>= end frame-num)))) (:box ori))
         box-cnt (reduce (fn [acc ele] (+ acc (count (re-seq #"b" (:data ele))))) 0 filtered-data)]
     (assoc-in db [:data k] (merge ori {target v} {:box-cnt box-cnt})))))

(reg-event-db
 :busy
 (fn [db v]
   (debug v)
   db))

(reg-event-db
 :show-result
 (fn [db v]
   (assoc db :show-result v)))

;; (reg-event-db
;;  :modify-frame-num
;;  (fn [db [_ k start-v end-v]]
;;    (debug "k " k)
;;    (debug start-v end-v)
;;    (let [ori (get-in db [:data k])]
;;      (assoc-in db [:data k] (merge ori {:start start-v :end end-v})))))