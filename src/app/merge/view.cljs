(ns app.merge.view
 (:require [re-frame.core :refer [subscribe dispatch]]
           [app.common-element :refer [split-input-box]]
           [app.merge.calc :as calc]
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


;; (defn analyze-ele-view [k {:keys [filename alias start end total-box-cnt]}]
;;   [:div {:class "flex flex-row"
;;          :key filename}
;;    [:div  {:class "ml-2 mr-2 mt-3 items-center"}
;;     alias]
;;    [split-input-box (str k "-start") start]
;;    [split-input-box (str k "-end") end]
;;    [:div {:class "mt-3 items-center"} total-box-cnt]])

(defn analyze-ele-view [k {:keys [filename alias start end total-box-cnt box-cnt-per-frame base-file]}]
  [:tr {:class "text-center"
        :key (str filename "-" start)}
   [:td alias]
   [:td [split-input-box (str k "-start") start]]
   [:td [split-input-box (str k "-end") end]]
   [:td {:class "mt-3 items-center"} total-box-cnt]
   [:td (.floor js/Math box-cnt-per-frame)]])


(defn analyze-base-file-view [[k {:keys [filename alias start end total-box-cnt box-cnt-per-frame base-file]}]]
  (when (not (nil? k))
    [:tr {:class "bg-blue-100 text-center"
          :key "base-file"}
     [:td "base file"]
     [:td [split-input-box (str k "-start") start]]
     [:td [split-input-box (str k "-end") end]]
     [:td {:class "mt-3 items-center"} total-box-cnt]
     [:td (.floor js/Math total-box-cnt)]]))




(defn analyze-result []
  (let [result @(subscribe [:data])]
        ;; 
        ;; others (dissoc result (-> keys base-file first))]
    ;; (prn (-> keys base-file))
    (when (seq result)
      (let [base-file (-> (filter (fn [[_ v]] (true? (:base-file? v))) result) first)
            others (dissoc result (-> base-file first))]
      [:div {:class "flex flex-col mt-6"}
         [:table {:class "table-auto"}
          [:thead
           [:tr
            [:th {:class "w-36 justify-center"} "alias"]
            [:th {:class "w-36 items-center"} "start frame"]
            [:th {:class "w-36"} "end frame"]
            [:th {:class "w-36"} "total box"]
            [:th {:class "w-36"} "box avg (box cnt/frame)"]]]
          [:tbody
           (analyze-base-file-view base-file)
           (for [[k v] (sort-result others)]
             (analyze-ele-view k v))]]]))))

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


(defn validation []
  (let [result @(subscribe [:data])
        without-base (filter (fn [[_ {:keys [:base-file?]}]]
                               (not (true? base-file?))) result)]
   (validation-check (sort-result without-base))))


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

(defn basefile-check [e]
  (let [check-boxs (.getElementsByName js/document "basefile-checkbox")
        file-lst (.getElementsByName js/document "filelist")
        cur-checked (-> e .-target .-checked)
        cur-item (->> e
                      .-target
                      (.values js/Object)
                      second
                      js->clj
                      (#(get % "id")))]
    (if (true? cur-checked)
      (dispatch [:base-file cur-item])
      (dispatch [:base-file nil]))
    (doseq [i (range (.-length file-lst))
            :let [item (->> (aget file-lst i)
                            (.values js/Object)
                            js->clj
                            second
                            (#(get % "children")))]]
      (when (not= cur-item item)
        (set! (.-checked (aget check-boxs i)) false)))))


(defn file-lst []
  (let [files @(subscribe [:files])
        base-file @(subscribe [:base-file])]
    (when (seq files)
      [:div  {:class "flex justify-center"}
       [:div
        (for [file files]
          [:div {:class "form-check"
                 :key (str file)}
           [:input {:class "form-check-input appearance-none h-4 w-4 border border-gray-300 rounded-sm bg-white checked:bg-blue-600 checked:border-blue-600 focus:outline-none transition duration-200 mt-1 align-top bg-no-repeat bg-center bg-contain float-left mr-2 cursor-pointer"
                    :type "checkbox"
                    :value ""
                    :name "basefile-checkbox"
                    :checked (= file base-file)
                    :id (str file)
                    :on-change #(basefile-check %)}]
           [:label {:class "form-check-label inline-block text-gray-800"
                    :for (str file)
                    :id (str file)
                    :name "filelist"}
            (str file)]])]])))

(defn view-merge []
  (dispatch [:mode :merge])
  [:div {:class "mt-6 mb-6 justify-center"}
   [file-lst]
   [analyze]
   [analyze-result]
   [analyze-graph]
   [validation]
   [merge-btn-view]
   [show-result]])