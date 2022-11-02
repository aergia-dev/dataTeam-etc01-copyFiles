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

