(ns app.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [app.subs]
            [app.toaster :as toaster]
            ["react-toastify" :refer [ToastContainer]]
            [app.tauri-cmd :as cmd]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/tauri" :refer [invoke]]
            ["@tauri-apps/api/fs" :as fs]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [app.recharttest :as c]
            [taoensso.timbre :refer [debug info error fatal]]))


(defn view-open-file []
  [:div {:class "flex v-screen flex-col space-y-4"}
   [:div {:class "flex grow justify-center"}
    [:button {:class " bg-blue-500 hover:bg-blue-700 text-white font-bold rounded-full w-96"
              :on-click (fn [e]
                          (let [f (.open dialog (clj->js {:multiple true}))]
                            (dispatch [:clear-data nil])
                            (-> f
                                (.then (fn [f] (dispatch-sync [:files (js->clj f)])))
                                (.catch #(js/alert "file open error: " %)))))} "open"]]

   [:div {:class "flex y-10"}]
   (let [files @(subscribe [:files])]
     (when (seq files)
       [:div {:class "flex flex-col"}
        [:span {:class "h-1 w-full bg-blue-200"}]
        [:div {:class "flex-col"}
         [:ul {:class "list-inside"}]
         (for [file (second files)]
           [:li {:key (str file)}
            (str file)])]
        [:span {:class "h-1 w-full bg-blue-200"}]]))])


(defn frame-from-filename [fname]
  (debug fname)
  (let [[filename alias start-frame end-frame] (->> (clojure.string/replace fname #"\s+" "")
                                                    (re-find #"\d+_\d+_\d+_\d+_\d+_\d+_(.+)_?\((\d+)(\d+)\)"))]
    {:filename fname
     :alias alias
     :start (js/parseInt start-frame)
     :end (js/parseInt end-frame)}))

(defn count-box [data]
  (count (re-seq #"b" data)))

(defn extract-box-info [info content]
  (let [data (re-seq #"(b.+\r?\n){0,}f(\d+)\r?\n" content)]
    (->> (map (fn [[whole box frame]]
                {:frame (js/parseInt frame)
                 :box box
                 :box-cnt (count-box whole)
                 :data whole}) data)
         (filter (fn [m] (and (< (:start info) (:frame m))
                              (> (:end info) (:frame m))
                              ;; (seq (:box m))
                              ))))))

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
                  (let [box-info (extract-box-info info content)
                        last-idx (extract-last-frame-num content)
                        total-box-cnt (reduce (fn [acc ele]
                                                ;; (debug (:box-cnt ele))
                                                (+ acc (js/parseInt (:box-cnt ele)))) 0 box-info)]
                    (dispatch [:add-data [k (merge info {:box box-info
                                                         :total-box-cnt total-box-cnt
                                                         :last-idx last-idx})]]))))
         (.catch #(debug (ex-cause %))))))

(defn analyze-on-click [file-list]
  (doall (map analyze-file (range (count file-list)) file-list)))

(defn analyze [file-list]
  [:div {:class "flex justify-center grow"}
   [:button {:class "bg-blue-500 text-white rounded-full w-96 hover:bg-blue-700"
             :on-click #(analyze-on-click file-list)}
    "analyze for merge"]])

(defn default-input-box [id v]
  (let [val (r/atom v)]
    (fn []
      [:div {:class "flex justify-center mt-3"
             :key v}
       [:div {:class "mb-3 w-32 ml-2 mr-2 "}
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


(defn analyze-ele-view [k {:keys [filename alias start end total-box-cnt]}]
  [:div {:class "flex flex-row"
         :key filename}
   [:div  {:class "ml-2 mr-2 mt-3 items-center"}
    alias]
   [default-input-box (str k "-start") start]
   [default-input-box (str k "-end") end]
   [:div {:class "mt-3 items-center"} total-box-cnt]])

(defn sort-result [result]
  (sort-by #(-> % second :start) < result))

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

(defn merge-on-click []

  (test-spinning)
  (let [f (.save dialog)
        data (-> @(subscribe [:data]) sort-result)
        d (apply str (make-write-data data))]
    (-> f
        (.then (fn [path]
                 (save-file path (apply str d))))
        (.catch #(debug (ex-cause %))))))


(defn validation []
  (let [result @(subscribe [:data])]
    (validation-check (sort-result result))))

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

(defn analyze-merge [files]
  [:div {:class "mt-6 mb-6"}
   [analyze (second files)]
   [analyze-result]
   [analyze-graph]
   [validation]
   [merge-btn-view]
   [processing-spin]
   [show-result]])



(defn analyze-split []
  [:div {:id "analyze-split"}
   [default-input-box "" ""]])

(defn analyze-select [files]
  (debug (-> files second count))
  (debug files)
  (if (= 1 (-> files second count))
    (analyze-split)
    (analyze-merge files)))

(defn default-view []
  [:div {:class "flex flex-col items-center"}
   [:> ToastContainer (clj->js {:position "bottom-right"
                                :autoClose 2000
                                :hideProgressBar false
                                :newestOnTop false
                                :closeOnClick true
                                :rtl false
                                :pauseOnFocusLoss false
                                :draggable true
                                :pauseOnHover true})]
   [view-open-file]
   (let [files @(subscribe [:files])]
     (when (seq files)
       [:div {:class "flex grow"
              :id "analyze-split"}
        (analyze-select files)]))
  ;;  [c/test-a]
   ])

