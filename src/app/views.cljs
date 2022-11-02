(ns app.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [app.subs]
            [app.toaster :as toaster]
            [app.tauri-cmd :as cmd]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/tauri" :refer [invoke]]
            [taoensso.timbre :refer [debug info error fatal]]))


(defn files []

  [:div
   [:div {:class "flex w-20"}
    [:button {:class " bg-blue-500 hover:bg-blue-800 text-white font-bold rounded-full px-4 py-2"
              :on-click (fn [e]
                          (let [f (.open dialog (clj->js {:multiple true}))]
                            ;; (let [invoke js/window.__TAURI__.invoke]
                            ;;   (-> (invoke "custom_command" (clj->js {"value" "cussss"}))
                            ;;       (.then #(prn "return : " %))
                            ;;       (.catch #(prn "exec cmd err: " %))))
                            (cmd/call-fn2 "custom_command" {"value" "scuees"})
                            (-> f
                                (.then (fn [f] (dispatch-sync [:files (js->clj f)])))
                                (.catch #(js/alert "file open error: " %)))))} "open"]]
   [:div
    (let [files @(subscribe [:files])]
     [:ul {:class "list-inside"}]
       (for [file (second files)]
         [:li {:key (str file)}
          (str file)]))]])

(defn copy-target []
  [:div "copy target"])

(defn result-estimation []
  [:div "resut estimation"])

(defn default-view []
  [:div
   [files]
   (copy-target)
   (result-estimation)
   ]
  )
