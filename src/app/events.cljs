(ns app.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [app.db :refer [default-db]]
            [app.calc-split :refer [analyze-split]]
            [app.calc-merge :refer [analyze-merge action-merge]]
            [taoensso.timbre :refer [debug]]))

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
 (fn [db [k v]]
   (debug "!!!!!!!" k ", " v)
   (assoc-in db [k] v)))

(reg-event-db
 :show-result
 (fn [db v]
   (assoc db :show-result v)))

(reg-event-db
 :item-cnt
 (fn [db [k v]]
   (assoc-in db [:status k] v)))

;; (reg-event-fx
;;  :action-split
;;  (fn [{db :db}  _]
;;    (debug "##################")
;;    (let [data (analyze-split (:files db))]
;;      {:dispatch [:add-data [:data data]]
;;       :db (assoc db :busy false)})))

(reg-event-db
 :action-split
 (fn [db  _]
   (debug "##################")
   (analyze-split (-> db :files second first))
   db))

(reg-event-fx
 :split-analyze
 (fn [{db :db} _]
   {:dispatch ^:flush-dom [:action-split]
    :db (assoc db :busy true)}))

(reg-event-fx
 :merge-analyze
 (fn [{db :db} _]
   {:dispatch ^:flush-dom [:action-merge-analyze]
    :db (assoc db :busy true)}))

(reg-event-fx
 :req-merge
 (fn [{db :db} _]
   {:dispatch ^:flush-dom [:action-merge]
    :db (assoc db :busy true)}))


(reg-event-db
 :action-merge-analyze
 (fn [db  _]
   (analyze-merge (-> db :files second))
   db))

(reg-event-db
 :action-merge
 (fn [db  _]
   (action-merge (-> db :data))
   db))


(reg-event-db
 :mode
 (fn [db [k v]]
   (let [color (condp = v
                 :split {:normal " bg-green-200 "
                         :hover " hover:bg-green-400 "
                         :all " bg-green-200 hover:bg-green-400 "}
                 :merge {:normal " bg-blue-200 "
                         :hover " hover:bg-blue-400 "
                         :all " bg-blue-200 hover:bg-blue-400 "})]
     (assoc db  k v :color color))))