(ns app.calc-split
  (:require [re-frame.core :refer [subscribe dispatch]]
            ["@tauri-apps/api/fs" :as fs]
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

(defn analyze-split [file]
  (debug "@@@ " file)
  (-> (fs/readTextFile file)
      (.then (fn [content]
               (let [frame-data (extract-box-info content)
                     first-idx (extract-first-frame-num content)
                     last-idx (extract-last-frame-num content)
                     total-box-cnt (reduce (fn [acc ele]
                                             (+ acc (js/parseInt (:frame-box-cnt ele)))) 0 frame-data)
                     box-cnt-per-frame (/ total-box-cnt (- last-idx first-idx))]
                 (dispatch [:busy false])
                 (dispatch [:add-data [:data {:frame-data frame-data
                                              :total-box-cnt total-box-cnt
                                              :frame-cnt-has-box box-cnt-per-frame
                                              :first-idx first-idx
                                              :last-idx last-idx}]]))))
                ;;  {:frame-data frame-data
                ;;   :total-box-cnt total-box-cnt
                ;;   :frame-cnt-has-box box-cnt-per-frame
                ;;   :first-idx first-idx
                ;;   :last-idx last-idx})))

      (.catch #(do
                 (dispatch [:busy false])
                 (debug "analyze for split exceptoin " (ex-cause %))))))

