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
 :test
 (fn [db _]
   (debug ":test " _)))

(reg-event-db
 :add-data
 (fn [db [_ [k v]]]
   (assoc-in db [:data k] v)))

(reg-event-db
 :clear-data
 (fn [db _]
   (assoc-in db [:data] nil)))


(reg-event-db
 :validation
 (fn [db [k v]]
   (assoc db :validation v)))

(reg-event-db
 :modify-frame-num
 (fn [db [_ k target v]]
   (let [ori (get-in db [:data k])]
     (assoc-in db [:data k] (merge ori {target v})))))

;; (reg-event-db
;;  :modify-frame-num
;;  (fn [db [_ k start-v end-v]]
;;    (debug "k " k)
;;    (debug start-v end-v)
;;    (let [ori (get-in db [:data k])]
;;      (assoc-in db [:data k] (merge ori {:start start-v :end end-v})))))