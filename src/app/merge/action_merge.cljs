(ns app.merge.action-merge
  (:require  [re-frame.core :refer [dispatch]]
             [app.common.utils :refer [sort-result split-basefile-others]]
             [app.toaster :as toaster]
             ["@tauri-apps/api/fs" :as fs]
             ["@tauri-apps/api/dialog" :as dialog]
             [taoensso.timbre :refer [debug]]))

(defn make-empty-frame-str [start end]
  (debug "start " start)
  (debug "end " end)
  (let [s (apply str (reduce (fn [acc idx]
                               (conj acc (str "f" idx "\r\n"))) [] (range start  end)))]
    s))

(defn prep-data [item]
  (let [start (:start item)
        end (:end item)]
    (->> (:frame-data item)
         (filter (fn [frame] (and (<= start (:frame frame))
                                  (>= end (:frame frame)))))
         (map :data))))

;; make a single vector. 
(defn gathering-data [others]
  (reduce (fn [acc ele]
            (let [[_ {:keys [:start :end :frame-data]}] ele
                  d (filter #(and (<= start (:frame %))
                                  (>= end (:end %))) frame-data)]
              (apply conj acc d))) [] others))

(defn save-file [path content]
  (-> (.writeFile fs (clj->js {:contents content
                               :path path}))
      (.then (fn [e]
               (dispatch [:show-result (str "output file - " path)])
               (toaster/toast (str "saved - " path))))
      (.catch #(debug (ex-cause %)))))

;;remove frames that is in others from basefile
(defn re-shape-base [base-data others]
  (let [others-range (map (fn [[k v]]
                            {:start (:start v)
                             :end (:end v)}) others)]
    (filter (fn [frame-data]
              (let [f-number (:frame frame-data)]
                (every? false? (map (fn [{:keys [:start :end]}]
                                      (and (<= start f-number)
                                           (>= end f-number))) others-range)))) base-data)))

(defn merge-with-base [base others]
  (let [merged (->> (apply conj base others)
                    (sort-by :frame)
                    (map :data))]
    merged))

(defn action-merge [data]
  (let [{:keys [:basefile :others]} (split-basefile-others data)
        refined-base (re-shape-base (-> basefile first second :frame-data) others)
        refined-others (reduce (fn [acc [_ v]]
                                 (apply conj acc (-> v  :frame-data))) [] others)]
    (let [f (.save dialog)
          d (merge-with-base (if (seq refined-base)
                               refined-base
                               []) refined-others)]
      (-> f
          (.then (fn [path]
                   (save-file path (apply str d))
                   (dispatch [:busy false])))
          (.catch #(debug (ex-cause %)))))))

