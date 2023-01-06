(ns app.views
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [app.subs]
            [app.common-element :refer [spinner]]
            [app.merge.view :as merge-view]
            [app.split.view :as split-view]
            ["react-toastify" :refer [ToastContainer]]
            ["@tauri-apps/api/dialog" :as dialog]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [taoensso.timbre :refer [debug info error fatal]]))


(defn view-open-file []
  [:div {:class "flex v-screen flex-col space-y-4 justify-center"}
   [:button {:class "bg-gray-500 hover:bg-gray-700 text-white font-bold rounded-full w-20"
             :on-click (fn [_]
                         (let [f (.open dialog (clj->js {:multiple true}))]
                           (dispatch [:clear-data nil])
                           (-> f
                               (.then (fn [f] (dispatch-sync [:files (js->clj f)])))
                               (.catch #(js/alert "file open error: " %)))))}
    "open"]])



(defn default-view []
  [:div
   [:div {:class "flex w-screen flex-col items-center justify-center z-20 fixed"}
    [:> ToastContainer (clj->js {:position "bottom-right"
                                 :autoClose 2000
                                 :hideProgressBar false
                                 :newestOnTop false
                                 :closeOnClick true
                                 :rtl false
                                 :pauseOnFocusLoss false
                                 :draggable true
                                 :pauseOnHover true})]]
   [:div {:class "flex h-screen w-screen items-center justify-center fixed z-999"}
    (let [enable? @(subscribe [:busy])]
      (spinner "" enable?))]

   [:div {:class "flex flex-col  w-screen items-center justify-center fixed"}
    [view-open-file]
    (let [files @(subscribe [:files])]
      (when (seq files)
        (debug "##" (count files))
        (if (= (count files) 1)
          [split-view/view-split]
          [merge-view/view-merge])))]])
