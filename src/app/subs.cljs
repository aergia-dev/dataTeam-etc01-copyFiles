(ns app.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [taoensso.timbre :refer [debug info error fatal]]))

(reg-sub
 :files
 (fn [db _]
   (get db :files)))


(reg-sub
 :data
 (fn [db _]
   (:data db)))

(reg-sub
 :validation
 (fn [db _]
   (:validation db)))

(reg-sub
 :busy
 (fn [db _]
   (:busy db)))

(reg-sub
 :show-result
 (fn [db _]
   (:show-result db)))