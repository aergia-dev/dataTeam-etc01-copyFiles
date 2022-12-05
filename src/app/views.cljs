(ns app.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [app.subs]
            [app.toaster :as toaster]
            [app.tauri-cmd :as cmd]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/tauri" :refer [invoke]]
            ["@tauri-apps/api/fs" :as fs]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [app.recharttest :as c]
            [taoensso.timbre :refer [debug info error fatal]]))


(defn view-open-file []
  [:div
   [:div {:class "flex w-20"}
    [:button {:class " bg-blue-500 hover:bg-blue-800 text-white font-bold rounded-full px-4 py-2"
              :on-click (fn [e]
                          (let [f (.open dialog (clj->js {:multiple true}))]
                            (dispatch [:clear-data nil])
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

(defn frame-from-filename [fname]
  (debug fname)
  (let [[filename alias start-frame end-frame] (->> (clojure.string/replace fname #"\s+" "")
                                                    (re-find #"\d+_\d+_\d+_\d+_\d+_\d+_(.+)_?\((\d+)_(\d+)\)"))]
    {:filename fname
     :alias alias
     :start (js/parseInt start-frame)
     :end (js/parseInt end-frame)}))

(defn extract-box-info [info content]
  (let [data (re-seq #"(b.+\r?\n){0,}f(\d+)\r?\n" content)]

    (->> (map (fn [[whole box frame]]
                {:frame (js/parseInt frame)
                 :box box
                ;;  :last-idx last-idx
                 :data whole}) data)
         (filter (fn [m] (and (< (:start info) (:frame m))
                              (> (:end info) (:frame m))
                              (seq (:box m))))))))

(defn extract-last-frame-num [content]
  (->> content
       (take-last 10)
       (apply str)
       (re-seq #"f(\d+)")
       last
       second
       js/parseInt))

(defn analyze-file [k fname]
  (debug fname)
  (let [info (frame-from-filename fname)]
    (->  (fs/readTextFile fname)
         (.then (fn [content]
                  (dispatch [:add-data [k (merge info {:box (extract-box-info info content)
                                                       :last-idx (extract-last-frame-num content)})]])))
         (.catch #(debug (ex-cause %))))))

(defn analyze-on-click [file-list]
  (doall (map analyze-file (range (count file-list)) file-list)))

(defn analyze [file-list]
  [:div {:class "flex"}
   [:button {:class "bg-blue-500 rounded-full x-15 y-10"
             :on-click #(analyze-on-click file-list)}
    "analyze for split"]])

(defn default-input-box [id v]
  (let [val (r/atom v)]
    (fn []
      [:div {:class "flex justify-center"
             :key v}
       [:div {:class "mb-3 xl:w-96"}
        [:input {:type "text"
                 :class "form-control block w-full
        px-3 py-1.5 text-base font-normal text-gray-700
        bg-white bg-clip-padding border border-solid border-gray-300
        rounded transition ease-in-out m-0
        focus:text-gray-700 focus:bg-white focus:border-blue-600 focus:outline-none"
                 :id id
                 :on-change #(let [new-v (-> % .-target .-value)
                                   [k target] (clojure.string/split id #"-")]
                               (reset! val new-v)
                               (dispatch [:modify-frame-num (js/parseInt k) (keyword target) (js/parseInt new-v)]))
                 :value (or @val "")}]]])))


(defn analyze-ele-view [k {:keys [filename alias start end box]}]
  [:div {:class "flex flex-row"
         :key filename}
   [:div alias]
   [default-input-box (str k "-start") start]
   [default-input-box (str k "-end") end]])

(defn sort-result [result]
  (sort-by (juxt second :start) > result))

(defn analyze-result []
  (let [result @(subscribe [:data])]
    [:div {:class "flex flex-col"}
     (for [[k v] (sort-result result)]
       [:div {:key k}
        (analyze-ele-view k v)])]))

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
                     (not (every? true? min-max-result)) (str "start, end frame num is wrong -"  (map (fn [r m] (when (false? r)
                                                                                                                  (str (-> m second :alias)))) min-max-result result))
                     (not (every? true? overlap-result)) (str "Not validate overlaped files- " (remove nil? (map (fn [r [e1 e2]] (when (false? r)
                                                                                                                                   (str (-> e1 second :alias) ", " (-> e2 second :alias)))) overlap-result partitioned)))
                     (= 0 (count result)) ""
                     :else "validate")]
    (dispatch [:validation result-msg])))

(defn prep-write-data [item]
  ;; (debug "prep write data " (second item))
  (let [data (second item)
        start (data :start)
        end (data :end)]
    ;; (debug "Start " start)
    ;; (debug "End " end)

    (map (fn [frame-info]
           (debug "cur frame" (:frame frame-info))
           (when (and (<= start (:frame frame-info))
                      (>= end (:frame frame-info)))
             (get-in frame-info [:data]))) (data :box))))

(defn make-empty-frame-str [start end]
  (apply str (reduce (fn [acc idx]
                       (concat acc (str "f" idx "\r\n"))) "" (range start end))))

(defn make-write-data [data]
  (debug "count data " (count data))
  (debug data)
  (let [all-data (map prep-write-data data)]
    ;; (debug (flatten all-data))
    ;; (debug (count (flatten all-data)))
    (debug (count (first (flatten all-data))))
    (reduce (fn [acc frame]
              (let [[_ frame-num] (re-find #"(f\d+)" (apply str frame))
                    idx (:cur acc)
                    filled-str (concat (make-empty-frame-str idx frame-num))
                    prev-data (:data acc)]
                ;; (if (not (= (:cur acc) frame-num))
                (assoc acc :data (concat prev-data filled-str frame) :cur (inc idx))))
                  ;; (assoc acc :data frame)))) 
            {:cur 0 :data nil} (flatten all-data))))

(defn save-file [path content]
  ;; (let []
        ;; yaml-str (->> content
                    ;;  (map #(_make-lidar-str (val %)))
                    ;;  (apply str)
                    ;;  (str default-format))]
  (debug content)
  (debug path)
  (-> (.writeFile fs (clj->js {:contents content
                               :path path}))
      (.then (debug "then"));;#(toaster/toast (str "saved - " path)))
      (.catch #(debug (ex-cause %)))))

;;      (.catch (debug "catch"))));;#(toaster/toast (str "failed saving - " path)))))



(defn merge-on-click []
  (let [data (-> @(subscribe [:data]) sort-result)]
    (let [d (:data (make-write-data data))]
      (debug "### " d)
      (save-file "D:\\wip\\kk\\abc.txt" (apply str d)))))

(defn validation []
  (let [result @(subscribe [:data])]
    (validation-check (sort-result result))
    (let [validation-msg @(subscribe [:validation])]
      [:div
       [:div validation-msg]
       (when (= "validate" validation-msg)
         [:button {:class "bg-blue-500"
                   :on-click #(merge-on-click)} "merge"])])))


(defn analyze-split [files]
  [:div {:id "analyze-split"}
   [analyze (second files)]
   [analyze-result]
   [analyze-graph]
   [validation]])

(defn analyze-merge [files]
  [:div {:id "analyze-merge"}]
  "for merge")

(defn analyze-select [files]
  (debug (-> files second count))
  (debug files)
  (if (= 1 (-> files second count))
    (analyze-merge [files])
    (analyze-split files)))

(defn default-view []
  [:div
   [view-open-file]
   (let [files @(subscribe [:files])]
     (when (seq files)
       (analyze-select files)))
  ;;  [c/test-a]
   (copy-target)
   (result-estimation)])


;; (dispatch [:files '("/home/hss/2022_06_16_17_06_06_Pedestrian_GT_test(1702_2557).box")])
