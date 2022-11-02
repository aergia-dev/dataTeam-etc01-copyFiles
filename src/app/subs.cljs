(ns app.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [taoensso.timbre :refer [debug info error fatal]]))

(reg-sub
 :files
 (fn [db _]
   (get db :files)))
