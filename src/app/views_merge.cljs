(ns app.views-merge
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/fs" :as fs]
            [app.common-element :refer [split-input-box]]
            [app.toaster :as toaster]
            [taoensso.timbre :refer [debug info error fatal]]))

(defn sort-result [result]
  (sort-by #(-> % second :start) < result))

(defn frame-from-filename [fname]
  (letfn [(make-frame-info [{:keys [start end]}] {:start start :end end})
          (extract-frame [v] (->> (clojure.string/split v #"_")
                                  (map #(clojure.string/split % #"-"))))]
    (let [[_ alias frame-str] (->> (clojure.string/replace fname #"\s+" "")
                                   (re-find #"\d+_\d+_\d+_\d+_\d+_\d+_(.+)_?\((.+)\)"))
          frames (reduce (fn [acc [s e]]
                           (conj acc {:start (js/parseInt s) :end (js/parseInt e)})) [] (extract-frame frame-str))]
      (map #(merge (make-frame-info %) {:filename fname :alias alias}) frames))))


(defn count-box [data]
  (count (re-seq #"b" data)))

(defn extract-box-info [info content]
  (let [data (re-seq #"(b.+\r?\n){0,}f(\d+)\r?\n" content)]
    (map (fn [[whole _ frame]]
           {:frame (js/parseInt frame)
            ;; :box box
            :frame-box-cnt (count-box whole)
            :data whole}) data)))

         ;; (filter (fn [m] (and (< (:start info) (:frame m))
                              ;; (> (:end info) (:frame m))))))))

(defn filter-box-range [start end frame-data]
  (filter (fn [f]
            (and (<= start (:frame f))
                 (>= end  (:frame f))) frame-data)))

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
    (count (filter #(comp not nil? (:frame %)) filtered-frame))))

(defn analyze-file [k item]
  (debug item)
  (->  (fs/readTextFile (:filename item))
       (.then (fn [content]
                (let [frame-data (extract-box-info item content)
                      last-idx (extract-last-frame-num content)
                      ;;need to filter start/end frame num
                      total-box-cnt (reduce (fn [acc ele]
                                              (+ acc (js/parseInt (:box-cnt ele)))) 0 frame-data)
                      ;;need to filter start/end frame num
                      frame-cnt-has-box (count-frame-has-box (:start item) (:end item) frame-data)
                      box-cnt-per-frame (/ total-box-cnt frame-cnt-has-box)]
                  (debug "total " total-box-cnt)
                  (debug "frame " frame-cnt-has-box)
                  (dispatch [:add-data [k (merge item {:frame-data frame-data
                                                       :total-box-cnt total-box-cnt
                                                       :box-cnt-per-frame box-cnt-per-frame
                                                       :last-idx last-idx})]]))))
       (.catch #(debug "analyze file exception " (ex-cause %)))))

(defn analyze-on-click [file-list]
  (debug "file-list " file-list)
  (let [items (flatten (map frame-from-filename file-list))]
    (debug "### " items)
    (doall (map analyze-file (range (count items)) items))))

(defn analyze [file-list]
  [:div {:class "flex justify-center grow"}
   [:button {:class "bg-blue-500 text-white rounded-full w-96 hover:bg-blue-700"
             :on-click #(analyze-on-click file-list)}
    "analyze for merge"]])


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

(defn validation-overlap [item1 item2]
  (debug (:start item1)  "," (:end item1)  "," (:start item2) "," (:end item2))
  (< (:start item1) (:end item1) (:start item2) (:end item2)))

(defn validation-min-max [item]
  (let [min (-> item second :start)
        max (-> item second :end)]
    (< min max)))

(defn validation-check [result]
  (let [partitioned (partition 2 1 result)
        min-max-result (map validation-min-max result)
        overlap-result (map (fn [[e1 e2]]
                              (validation-overlap (second e1) (second e2))) partitioned)
        result-msg (cond
                     (not (every? true? min-max-result)) (str "Not valid - start, end frame num is wrong -"  (map (fn [r m] (when (false? r)
                                                                                                                              (str (-> m second :alias)))) min-max-result result))
                     (not (every? true? overlap-result)) (str "Not valid - overlaped files- " (remove nil? (map (fn [r [e1 e2]] (when (false? r)
                                                                                                                                  (str (-> e1 second :alias) ", " (-> e2 second :alias)))) overlap-result partitioned)))
                     (= 0 (count result)) ""
                     :else "valid")]
    (dispatch [:validation result-msg])))

(defn prep-write-data [item]
  (let [data (second item)
        start (data :start)
        end (data :end)]
    (map (fn [frame-info]
           (when (and (<= start (:frame frame-info))
                      (>= end (:frame frame-info)))
             (get-in frame-info [:data]))) (data :box))))

(defn make-empty-frame-str [start end]
  (let [s (apply str (reduce (fn [acc idx]
                               (concat acc (str "f" idx "\r\n"))) "" (range start (inc end))))]
    s))

(defn make-write-data [ori-data]
  (let [all-data (map prep-write-data ori-data)
        last-idx (-> ori-data first second :last-idx)]
    (loop [cur-frame 2
           result []
           data all-data]
      (if (seq data)
        (let [[_ min-frame-s] (re-find #"f(\d+)" (ffirst data))
              min-frame (js/parseInt min-frame-s)]
          (recur (inc min-frame) (conj result (make-empty-frame-str cur-frame min-frame) (first data)) (rest data)))
        (do
          (apply concat (conj result (make-empty-frame-str (-> ori-data last second :end) last-idx))))))))

(defn save-file [path content]
  (-> (.writeFile fs (clj->js {:contents content
                               :path path}))
      (.then (fn [e]
               (dispatch [:show-result (str "output file - " path)])
               (toaster/toast (str "saved - " path))))
      (.catch #(debug (ex-cause %)))))


(defn processing-spin []
  (let [busy? @(subscribe [:busy])]
    ;; (debug "busy? " busy?)
    (when (true? busy?)
      [:div {:class "flex justify-center items-center"}
       [:div {:class "animate-spin inline-block w-16 h-16 border-4 rounded-full"
              :role "status"}
        [:span {:class "visually-hidden"} "processing"]]])))

(defn test-spinning []
  (r/create-class
   {:component-did-mount processing-spin
    :display-name "processing"
    :reagent-render (fn []
                      [:div {:id "processing"}])}))

(defn validation []
  (let [result @(subscribe [:data])]
    (validation-check (sort-result result))))

(defn merge-on-click []
  (test-spinning)
  (let [f (.save dialog)
        data (-> @(subscribe [:data]) sort-result)
        d (apply str (make-write-data data))]
    (-> f
        (.then (fn [path]
                 (save-file path (apply str d))))
        (.catch #(debug (ex-cause %))))))


(defn merge-btn-view []
  (let [validation-msg @(subscribe [:validation])]
    (if (= "valid" validation-msg)
      [:div {:class "bg-white/100 mt-6 items-center grow mt-6"}
       [:div {:class "bg-blue-500/50 text-white text-center grow"} validation-msg]
       [:button {:class "bg-blue-500 hover:bg-blue-700 text-white rounded-full w-96 mt-6 font-bold"
                 :on-click #(merge-on-click)} "merge"]]
      [:div {:class "bg-red-500/50 text-white mt-6 text-center"} validation-msg])))

(defn show-result []
  (let [result @(subscribe [:show-result])]
    (when (not (nil? result))
      [:div {:class "bg-white/100 mt-6 items-center grow mt-6"}
       [:div {:class "bg-blue-500/50 text-white text-center grow"} result]])))


(defn view-merge [files]
  [:div {:class "mt-6 mb-6"}
   [analyze (second files)]
   [analyze-result]
   [analyze-graph]
   [validation]
   [merge-btn-view]
   [processing-spin]
   [show-result]])
