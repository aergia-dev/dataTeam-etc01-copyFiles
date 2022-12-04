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
                            ;; (let [invoke js/window.__TAURI__.invoke]
                            ;;   (-> (invoke "custom_command" (clj->js {"value" "cussss"}))
                            ;;       (.then #(prn "return : " %))
                            ;;       (.catch #(prn "exec cmd err: " %))))
                            ;; (cmd/call-fn2 "custom_command" {"value" "scuees"})
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

;; (defn frame-from-filename [fname]
;;   (debug fname)
;;   (let [[filename alias start-frame end-frame] (->> (clojure.string/replace fname #"\s+" "")
;;                                                     (re-find #(dispatch [:add-data {k (merge info {:box (extract-box-info info content)})}])))
;;         (.catch #(debug (ex-cause %)))]))

(defn frame-from-filename [fname]
  (debug fname)
  (let [[filename alias start-frame end-frame] (->> (clojure.string/replace fname #"\s+" "")
                                                    (re-find #"\d+_\d+_\d+_\d+_\d+_\d+_(.+)_?\((\d+)_(\d+)\)"))]
    ;; (re-find #".+\((\d+)_(\d+)\).*\.box"))]
    {:filename fname
     :alias alias
     :start (js/parseInt start-frame)
     :end (js/parseInt end-frame)}))

(defn extract-box-info [info content]
  (let [data (re-seq #"f(\d+)\r?\n(b.+\r?\n){0,}" content)
        last-idx (->> content
                      (take-last 10)
                      (apply str)
                      (re-seq #"f(\d+)")
                      last
                      second
                      js/parseInt)]

    (->> (map (fn [[whole frame box]]
                {:frame (js/parseInt frame)
                 :box box
                 :last-idx last-idx
                 :data whole}) data)
         (filter (fn [m] (and (< (:start info) (:frame m))
                              (> (:end info) (:frame m))
                              (seq (:box m))))))))

(defn analyze-file [k fname]
  (debug fname)
  (let [info (frame-from-filename fname)]
    (->  (fs/readTextFile fname)
         (.then (fn [content]
                  (dispatch [:add-data [k (merge info {:box (extract-box-info info content)})]])))
         (.catch #(debug (ex-cause %))))))

(defn analyze-on-click [file-list]
  (doall (map analyze-file (range (count file-list)) file-list)))

(defn analyze [file-list]
  ;; (let [file-list (second @(subscribe [:files]))]
    [:div {:class "flex"}
     [:button {:class "bg-blue-500 rounded-full x-10"
               :on-click #(analyze-on-click file-list)}
      "analyze"]])

(defn default-input-box [v]
  [:div {:class "flex justify-center"
         :key v}
   [:div {:class "mb-3 xl:w-96"}
    [:input {:type "text"
             :class "form-control block w-full
        px-3 py-1.5 text-base font-normal text-gray-700
        bg-white bg-clip-padding border border-solid border-gray-300
        rounded transition ease-in-out m-0
        focus:text-gray-700 focus:bg-white focus:border-blue-600 focus:outline-none"
             :value v}]]])

(defn analyze-ele-view [k {:keys [filename alias start end box]}]
  [:div {:class "flex flex-row"
         :key filename}
   [:div alias]
   (default-input-box start)
   (default-input-box end)
   [:button {:class "bg-blue-500 x-5"
             :on-click #()} "apply"]])

(defn analyze-result []
  (let [result @(subscribe [:data])]
    [:div {:class "flex "}
     (for [[k v] result]
       (analyze-ele-view k v))]))

(defn analyze-graph []
  (let [result @(subscribe [:data])]
    
  ))

(defn validation-check [result]
  true)

(defn validation []
  (let [result @(subscribe [:data])]
    (if (validation-check result)
      [:div "validate"]
      [:div "not validate"])))

  ;; (let [file-list (second @(subscribe [:files]))]
    ;; (if (= (count file-list) 1)
      ;; (view-split file-list)
      ;; (view-merge file-list))))



;;(let [s "2022_06_16_17_06_06_Pedestrian_GT_장정인(1702_2557).box"
;;      [filename start-frame end-frame] (re-find #".+\((\d+)_(\d+)\).box" s)]

;; {:file-1 {:fname filename
;;           :start-frame start-frame
;;           :end-frame end-frame
;;           :data {....}}}

(defn default-view []
  [:div
   [view-open-file]
   (let [files @(subscribe [:files])]
     (when (seq files)
       [:div {:id "anaylize-view"}
        [analyze (second files)]
        [analyze-result]
        [analyze-graph]
        [validation]]))

   [c/test-a]
   (copy-target)
   (result-estimation)])


;; (dispatch [:files '("/home/hss/2022_06_16_17_06_06_Pedestrian_GT_test(1702_2557).box")])
