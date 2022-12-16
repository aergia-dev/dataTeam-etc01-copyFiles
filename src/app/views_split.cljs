(ns app.views-split
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/fs" :as fs]
            [app.common-element :refer [split-input-box spinner]]
            [app.toaster :as toaster]
            [taoensso.timbre :refer [debug]]))

(defn analyze []
  (let [file (-> @(subscribe [:files]) second)
        color (-> @(subscribe [:color]))]

    (debug "###" color)
    ;; (debug "file " file)
    ;; (debug (seq file))
    (when (seq file)
      [:div {:class "flex justify-center grow"}
       [:button {:class (str (:all color) "text-white rounded-full w-96 ")
                 :on-click #(dispatch [:split-analyze])}
              ;;  :on-click #(analyze-on-click (first file))}
        "analyze for split"]])))

(defn analyze-result []
  (let [{:keys [frame-data total-box-cnt frame-cnt-has-box first-idx last-idx] :as data} (-> @(subscribe [:data])
                                                                                             vals
                                                                                             first)
        color1 "bg-green-400/30"
        color2 "bg-green-200"]
    (when (seq data)
      [:table {:class "w-96
                       table-auto text-white border-collapse"}
       [:thead
        [:tr {:class color1}
         [:th {:class color1} "first idx"]
         [:th {:class color1} "last idx"]
         [:th {:class color1} "total box cnt"]
         [:th {:class color1} "box cnt avg"]]
        [:tr
         [:th {:class color2} first-idx]
         [:th {:class color2} last-idx]
         [:th {:class color2} total-box-cnt]
         [:th {:class color2} frame-cnt-has-box]]]])))

(defn analyze-graph [])

(defn validation [])

(defn split-btn-view [])

(defn show-result [])

(defn view-split []
  (dispatch [:mode :split])
  [:div {:class "mt-6 mb-6 justify-center"}
   [:div
    [:i {:class "fa-solid fa-fan text-[10rem] text-green-500 animate-spin"}]]
   [analyze]
   [analyze-result]
   [analyze-graph]
   [validation]
   [split-btn-view]
   [show-result]])
