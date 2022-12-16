(ns app.calc-merge
  (:require [re-frame.core :refer [subscribe dispatch]]
            ["@tauri-apps/api/fs" :as fs]
            ["@tauri-apps/api/dialog" :as dialog]
            [app.toaster :as toaster]
            [taoensso.timbre :refer [debug]]))

(defn count-box [data]
  (count (re-seq #"b" data)))

(defn extract-box-info [info content]
  (let [data (re-seq #"(b.+\r?\n){0,}f(\d+)\r?\n" content)]
    (map (fn [[whole _ frame]]
           {:frame (js/parseInt frame)
            :frame-box-cnt (count-box whole)
            :data whole}) data)))

(defn filter-box-range [start end frame-data]
  (filter (fn [f]
            (and (<= start (:frame f))
                 (>= end  (:frame f)))) frame-data))

(defn count-total-box [item]
  (let [filtered-box (filter-box-range (:start item) (:end item) (:box item))]
    (count filtered-box)))

(defn extract-last-frame-num [content]
  (->> content
       (take-last 10)
       (apply str)
       (re-seq #"f(\d+)")
       last
       second
       js/parseInt))

(defn count-frame-has-box [start end frame-data]
  (let [filtered-frame (filter-box-range start end frame-data)]
    (count (filter #(not (zero? (:frame-box-cnt %))) filtered-frame))))


(defn frame-from-filename [fname]
  (letfn [(make-frame-info [{:keys [start end]}] {:start start :end end})
          (extract-frame [v] (->> (clojure.string/split v #"_")
                                  (map #(clojure.string/split % #"-"))))]
    (let [[_ alias frame-str] (->> (clojure.string/replace fname #"\s+" "")
                                  ;;  (re-find #"\d+_\d+_\d+_\d+_\d+_\d+_(.+)_?\((.+)\)"))
                                   (re-find #"GT_(.+)_?\((.+)\)"))
          frames (reduce (fn [acc [s e]]
                           (conj acc {:start (js/parseInt s) :end (js/parseInt e)})) [] (extract-frame frame-str))]
      (map #(merge (make-frame-info %) {:filename fname :alias alias}) frames))))


(defn analyze-file [k item]
  (debug item)
  (->  (fs/readTextFile (:filename item))
       (.then (fn [content]
                (let [frame-data (->> (extract-box-info item content)
                                      (filter-box-range (:start item) (:end item)))
                      last-idx (extract-last-frame-num content)
                      ;;need to filter start/end frame num
                      total-box-cnt (reduce (fn [acc ele]
                                              (+ acc (js/parseInt (:frame-box-cnt ele)))) 0 frame-data)
                      ;;need to filter start/end frame num
                      frame-cnt-has-box (count-frame-has-box (:start item) (:end item) frame-data)
                      box-cnt-per-frame (/ total-box-cnt frame-cnt-has-box)]
                  (debug "total " total-box-cnt)
                  (debug "frame " frame-cnt-has-box)
                  (dispatch [:busy false])
                  (dispatch [:add-data [k (merge item {:frame-data frame-data
                                                       :total-box-cnt total-box-cnt
                                                       :frame-cnt-has-box frame-cnt-has-box
                                                       :box-cnt-per-frame box-cnt-per-frame
                                                       :last-idx last-idx})]]))))
       (.catch #(debug "analyze file exception " (ex-cause %)))))


(defn analyze-merge [file-list]
  (let [items (flatten (map frame-from-filename file-list))]
    (dispatch [:item-cnt (count items)])
    (doall (map analyze-file (range (count items)) items))))

(defn sort-result [result]
  (sort-by #(-> % second :start) < result))


(defn prep-write-data [item]
  (let [data (second item)
        start (data :start)
        end (data :end)]
    (->> (:frame-data data)
         (filter (fn [frame] (and (<= start (:frame frame))
                                  (>= end (:frame frame)))))
         (map :data))))

(defn prep-data [item]
  (let [start (:start item)
        end (:end item)]
    (->> (:frame-data item)
         (filter (fn [frame] (and (<= start (:frame frame))
                                  (>= end (:frame frame)))))
         (map :data))))


(defn make-empty-frame-str [start end]
  (debug "start " start)
  (debug "end " end)
  (let [s (apply str (reduce (fn [acc idx]
                               (conj acc (str "f" idx "\r\n"))) [] (range start  end)))]
    s))


(defn make-write-data [ori-data]
  ;; (debug ori-data)
  ;; (debug (first ori-data))
  ;; (debug (count ori-data))
  (loop [cur-frame 2
         result []
         data ori-data]
    (if (seq data)
      (let [m (-> data first second)
            min-frame (->> m
                           :frame-data
                           first
                           :frame)
            max-frame (->> m
                           :frame-data
                           last
                           :frame)]

        (debug (-> data first first))
        (debug "cur " cur-frame)
        (debug "min " min-frame)
        (debug "max " max-frame)
        (recur (inc max-frame) (conj result (make-empty-frame-str cur-frame min-frame) (prep-data m)) (rest data)))
      (do
        (apply concat (conj result (make-empty-frame-str (-> ori-data last second :end inc) (-> ori-data last second :last-idx))))))))

(defn save-file [path content]
  (-> (.writeFile fs (clj->js {:contents content
                               :path path}))
      (.then (fn [e]
               (dispatch [:show-result (str "output file - " path)])
               (toaster/toast (str "saved - " path))))
      (.catch #(debug (ex-cause %)))))


(defn action-merge [data]
  (let [f (.save dialog)
        d (apply str (make-write-data (sort-result data)))]
    (-> f
        (.then (fn [path]
                 (save-file path (apply str d))
                 (dispatch [:busy false])))
        (.catch #(debug (ex-cause %))))))

