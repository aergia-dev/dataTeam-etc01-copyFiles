(ns app.merge.view
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/fs" :as fs]
            [app.merge.calc :as calc]
            [app.common-element :refer [split-input-box spinner]]
            [app.toaster :as toaster]
            [app.calc-merge :refer [analyze-merge]]
            [taoensso.timbre :refer [debug]]))


(defn sort-result [result]
  (sort-by #(-> % second :start) < result))


(defn analyze []
  (let [files (-> @(subscribe [:files]) second)]
    (when (seq files)
      [:div {:class "flex justify-center grow"}
       [:button {:class "bg-blue-500 text-white rounded-full w-96 hover:bg-blue-700"
                 :on-click #(dispatch [:merge-analyze])}
        "analyze for merge"]])))


(defn analyze-ele-view [k {:keys [filename alias start end total-box-cnt]}]
  [:div {:class "flex flex-row"
         :key filename}
   [:div  {:class "ml-2 mr-2 mt-3 items-center"}
    alias]
   [split-input-box (str k "-start") start]
   [split-input-box (str k "-end") end]
   [:div {:class "mt-3 items-center"} total-box-cnt]])



(defn analyze-result []
  (let [result @(subscribe [:data])]
    (when (seq result)
      [:div {:class "flex flex-col mt-6"}
       [:div {:class "flex flex-row grow items-center"}
        [:div {:class "w-36 justify-center"} "alias"]
        [:div {:class "w-36 items-center"} "start frame"]
        [:div {:class "w-36"} "end frame"]
        [:div {:class "w-36"} "total box"]]
       (for [[k v] (sort-result result)]
         [:div {:key k}
          (analyze-ele-view k v)
          [:span {:class "h-1 w-full bg-gray-300"}]])])))

(defn analyze-graph []
  (let [result @(subscribe [:data])]))


(defn validation []
  (let [data @(subscribe [:data])
        validation-result (calc/validation-check (sort-result data))]
    (dispatch [:validation validation-result])))


(defn merge-btn-view []
  (let [validation-msg @(subscribe [:validation])
        item-cnt @(subscribe [:item-cnt])
        data-cnt @(subscribe [:data-cnt])]
    (when (= item-cnt data-cnt)
      (if (= "valid" validation-msg)
        [:div {:class " mt-6 items-center grow mt-6"}
         [:div {:class
                "bg-blue-500/50 text-white text-center grow"} validation-msg]
         [:button {:class "bg-blue-500 hover:bg-blue-700 text-white text-center rounded-full w-96 mt-6 font-bold"
                   :on-click ;;#(;;let [data @(subscribe [:data])]
                   #(dispatch [:req-merge])} "merge"]]
        [:div {:class "bg-red-500/50 text-white mt-6 text-center"} validation-msg]))))


(defn show-result []
  (let [result @(subscribe [:show-result])]
    (when (not (nil? result))
      [:div {:class "bg-white/100 mt-6 items-center grow mt-6"}
       [:div {:class "bg-blue-500/50 text-white text-center grow"} result]])))

(defn view-merge []
  (dispatch [:mode :merge])
  [:div {:class "mt-6 mb-6 justify-center"}
   [analyze]
   [analyze-result]
   [analyze-graph]
   [validation]
   [merge-btn-view]
   [show-result]])
