(ns app.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [app.subs]
            [app.views-merge :refer [view-merge]]
            [app.toaster :as toaster]
            ["react-toastify" :refer [ToastContainer]]
            [app.tauri-cmd :as cmd]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/tauri" :refer [invoke]]
            ["@tauri-apps/api/fs" :as fs]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [app.recharttest :as c]
            [taoensso.timbre :refer [debug info error fatal]]))


(defn view-open-file []
  [:div {:class "flex v-screen flex-col space-y-4"}
   [:div {:class "flex grow justify-center"}
    [:button {:class " bg-blue-500 hover:bg-blue-700 text-white font-bold rounded-full w-96"
              :on-click (fn [e]
                          (let [f (.open dialog (clj->js {:multiple true}))]
                            (dispatch [:clear-data nil])
                            (-> f
                                (.then (fn [f] (dispatch-sync [:files (js->clj f)])))
                                (.catch #(js/alert "file open error: " %)))))} "open"]]

   [:div {:class "flex y-10"}]
   (let [files @(subscribe [:files])]
     (when (seq files)
       [:div {:class "flex flex-col"}
        [:span {:class "h-1 w-full bg-blue-200"}]
        [:div {:class "flex-col"}
         [:ul {:class "list-inside"}]
         (for [file (second files)]
           [:li {:key (str file)}
            (str file)])]
        [:span {:class "h-1 w-full bg-blue-200"}]]))])



(defn analyze-split []
  [:div {:id "analyze-split"}
   "split"])
   ;; [default-input-box "" ""]])

(defn analyze-select [files]
  (debug (-> files second count))
  (debug files)
  (if (= 1 (-> files second count))
    (analyze-split)
    (view-merge files)))
    ;; (analyze-merge files)))

(defn default-view []
  [:div {:class "flex flex-col items-center"}
   [:> ToastContainer (clj->js {:position "bottom-right"
                                :autoClose 2000
                                :hideProgressBar false
                                :newestOnTop false
                                :closeOnClick true
                                :rtl false
                                :pauseOnFocusLoss false
                                :draggable true
                                :pauseOnHover true})]
   [view-open-file]
   (let [files @(subscribe [:files])]
     (when (seq files)
       [:div {:class "flex grow"
              :id "analyze-split"}
        (analyze-select files)]))
  ;;  [c/test-a]
   ])
