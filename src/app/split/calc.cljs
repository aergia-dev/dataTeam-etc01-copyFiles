(ns app.split.calc
  (:require [re-frame.core :refer [dispatch]]
            ["@tauri-apps/api/fs" :as fs]
            ["@tauri-apps/api/dialog" :as dialog]
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


(defn analyze-split [file]
  (-> (fs/readTextFile file)
      (.then (fn [content]
               (let [frame-data (extract-box-info content)
                     first-idx (extract-first-frame-num content)
                     last-idx (extract-last-frame-num content)
                     total-box-cnt (reduce (fn [acc ele]
                                             (+ acc (js/parseInt (:frame-box-cnt ele)))) 0 frame-data)
                     box-cnt-per-frame (/ total-box-cnt (- last-idx first-idx))]
                 (dispatch [:busy false])
                 (dispatch [:add-data [0 {:frame-data frame-data
                                          :total-box-cnt total-box-cnt
                                          :frame-cnt-has-box box-cnt-per-frame
                                          :first-idx first-idx
                                          :last-idx last-idx}]]))))
      (.catch #(do
                 (dispatch [:busy false])
                 (debug "analyze for split exceptoin " (ex-cause %))))))


(defn make-data [[k {:keys [:user-name :frames]}] [_ {:keys [:frame-data]}]]
  (reduce (fn [acc {:keys [:start :end]}]
            (let [range-data (filter #(and (<= start (:frame %))
                                           (>= end (:frame %))) frame-data)]
              (conj acc range-data))) [] frames))

(defn save-file [path content]
  (-> (.writeFile fs (clj->js {:contents content
                               :path path}))
      (.then (fn [e]
               (dispatch [:show-result (str "output file - " path)])
               (toaster/toast (str "saved - " path))))
      (.catch #(debug (ex-cause %)))))


(defn write-split [dir filename contents]
(prn "write dir " dir)
  (-> filename
      (.then (fn [path]
               (save-file path (apply str contents))
               (dispatch [:busy false])))))


;;{0 {:user-name "abc", :frame [{:start "1", :end "100", :idx 0}]}
;; 1 {:user-name "", :frame [{:start "1", :end "100", :idx 0}]}}
(defn action-split [data users]
  (let [dir (.save dialog (clj->js {:directory true}))]
(prn "dir " dir)))
    ;; (map (fn [user]
          ;;  (->> (make-data user data)
                ;; (write-split dir (:user-name user)))) users)))