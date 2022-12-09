(ns app.views-split
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/fs" :as fs]
            [app.common-element :refer [split-input-box]]
            [app.toaster :as toaster]
            [taoensso.timbre :refer [debug]]))

(defn count-box [data]
  (count (re-seq #"b" data)))

(defn extract-box-info [content]
  (let [data (re-seq #"(b.+\r?\n){0,}f(\d+)\r?\n" content)]
    (map (fn [[whole _ frame]]
           {:frame (js/parseInt frame)
            :frame-box-cnt (count-box whole)
            :data whole}) data)))

(defn extract-last-frame-num [content]
  (->> content
       (take-last 10)
       (apply str)
       (re-seq #"f(\d+)")
       last
       second
       js/parseInt))

(defn extract-first-frame-num [content]
  (->> content
       (re-find #"f(\d+)")
       first
       second
       js/parseInt))


(defn analyze-on-click [file]
  (-> (fs/readTextFile file)
      (.then (fn [content]
               (let [frame-data (extract-box-info content)
                     first-idx (extract-first-frame-num content)
                     last-idx (extract-last-frame-num content)
                     total-box-cnt (reduce (fn [acc ele]
                                             (+ acc (js/parseInt (:frame-box-cnt ele)))) 0 frame-data)
                     box-cnt-per-frame (/ total-box-cnt (- last-idx first-idx))]
                 (dispatch [:add-data [:split {:frame-data frame-data
                                               :total-box-cnt total-box-cnt
                                               :frame-cnt-has-box box-cnt-per-frame
                                               :first-idx first-idx
                                               :last-idx last-idx}]]))))
      (.catch #(debug "analyze for split exceptoin " (ex-cause %)))))

(defn analyze [file]
  (debug "file " file)
  (debug (seq file))
  (when (seq file)
    [:div {:class "flex justify-center grow"}
     [:button {:class "bg-green-400 text-white rounded-full w-96 hover:bg-green-600"
               :on-click #(analyze-on-click (first file))}
     "analyze for split"]]))

(defn analyze-result []
  )

(defn analyze-graph []
  )

(defn validation []
  )

(defn split-btn-view []
  )

(defn show-result []
  )

(defn view-split [files]
  (debug (second files))
  [:div {:class "mt-6 mb-6 justify-center"}
   [:div

    [:i {:class "fa-solid fa-fan text-[10rem] text-green-500 animate-spin"}]]

   [analyze (second files)]
   [analyze-result]
   [analyze-graph]
   [validation]
   [split-btn-view]
   [show-result]])
