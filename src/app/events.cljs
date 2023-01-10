(ns app.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [app.db :refer [default-db]]
            [app.common.utils :as u]
            [app.split.calc :refer [analyze-split]]
            [app.merge.calc :refer [analyze-merge]]
            [app.merge.action-merge :refer [action-merge]]
            ;; [app.split.calc :refer [action-split]]
            [taoensso.timbre :refer [debug]]))

(reg-event-db
 :init-db
 (fn [_ _]
   (prn (merge {} default-db))
   (merge {} default-db)))

(reg-event-db
 :files
 (fn [db [k v]]
   (let [base-file (-> (filter #(-> % u/parsing-filename nil?) v)
                       first)]
     (prn "####" base-file)
     (prn "@@@@ " (map nil? (map u/parsing-filename v)))
     (-> db
         (assoc k v)
         (assoc :base-file base-file)))))

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
   (analyze-merge (-> db :files) (-> db :base-file))
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

(reg-event-db
 :add-split-user
 (fn [db [k v]]
   (let [idx (if (nil? (:split-user-cnt db))
               0
               (:split-user-cnt db))]
     (-> db
         (update :split-user-cnt (fnil inc 0))
         (update :split-user assoc idx {:user-name nil :frame [{:start "" :end "" :idx 0}]})))))

(reg-event-db
 :add-split-user-frame
 (fn [db [k v]]
   (let [frame (get-in db [:split-user v :frame])]
     (assoc-in db [:split-user v :frame] (conj frame {:start "" :end "" :idx (-> frame count)})))))

(reg-event-db
 :minus-split-user-frame
 (fn [db [_ v]]
   (let [frame (get-in db [:split-user v :frame])
         re-new (into [] (filter #(< (:idx %) (-> frame count dec)) frame))]
     (prn "re -new " re-new)
     (if (= (count frame) 1)
       db
       (assoc-in db [:split-user v :frame] re-new)))))

(reg-event-db
 :base-file
 (fn [db [k v]]
   (assoc db k v)))

(reg-event-fx
 :split-analyze
 (fn [{db :db} _]
   {:dispatch ^:flush-dom [:action-split-analyze]
    :db (assoc db :busy true)}))


(reg-event-db
 :action-split-analyze
 (fn [db  _]
   (analyze-split (-> db :files first))
   db))

(reg-event-db
 :change-split-user-name
 (fn [db [_ k v]]
   (assoc-in db [:split-user k :user-name] v)))

(reg-event-db
 :change-frame-num
 (fn [db [_ k idx frame-k v]]
   (assoc-in db [:split-user k :frame idx frame-k] v)))

(reg-event-db
 :action-split
 (fn [db _]
  ;;  (action-split (-> db :data))
   db))

(reg-event-db
 :req-split
 (fn [{db :db} _]
   {:dispatch ^:flush-dom [:action-split]
    :db (assoc db :busy true)}))
