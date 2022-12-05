(ns app.recharttest
  (:require  [reagent.core :as r]
             ["chart.js/auto" :as c :refer [Chart]])) ;;:refer [Chart] :as cj]
             ;; ["recharts" :refer [BarChart] :as c]))


;; (def chart-data
;;   [{:name "A" :before 200 :current 300 :after 500}])

;; (defn test-aaaa []
;;   [:div
;;    [BarChart (clj->js {:width 499
;;                        :height 300
;;                        :data chart-data})]])


;; (defn graph-render [graph-attrs graph-data]
;;   ;; return hiccup for the graph here
;;   (let [width (:width graph-attrs)
;;         height (:height graph-attrs)]
;;     [:svg {:width width :height height}
;;      [:g.graph]]))

;; (defn graph-component [graph-attrs graph-data]
;;   (r/create-class
;;    {:display-name "graph"
;;     :reagent-render graph-render
;;     :component-did-update (fn [this]
;;                             (let [[_ graph-attrs graph-data] (r/argv this)]
;;                               (update! graph-attrs graph-data)))
;;     :component-did-mount (fn [this]
;;                            (let [[_ graph-attrs graph-data] (r/argv this)]
;;                              (init! graph-attrs graph-data)))}))

;; (defn container []
;;   [:div {:id "graph-container"}
;;    [graph-component
;;     @graph-attrs-ratom
;;     @graph-data-ratom]])

(defn show-chart []
  (let [context (.getContext (.getElementById js/document "chart1") "2d")
        chart-data {:type "bar"
                    :data {:labels ["2012" "2013" "2014" "2015" "2016"]
                           :datasets [{:data [5 10 15 20 25]
                                       :label "Rev in MM"
                                       :backgroundColor "#90EE90"}
                                      {:data [3 6 9 12 15]
                                       :label "Cost in MM"
                                       :backgroundColor "#F08080"}]}
                    :options {:scales {:y {:beginAtZero true}}}}]
    [:div
     (Chart. context (clj->js chart-data))]))

(defn test-chart []
  (let [context (.getContext (.getElementById js/document "chart1") "2d")
        chart-data {:type "bar"
                    :data {:labels ["merged"]
                           :datasets [{:data [50]
                                       :label "emtpy"
                                       :backgroundColor "#90EE90"}
                                      {:data [30]
                                       :label "exist"
                                       :backgroundColor "#F08080"}
                                      {:data [20]
                                       :label "empty"
                                       :backgroundColor "#90EE90"}]}
                    :options {:indexAxis "y"
                              :scales {:x {:stacked true}
                                       :y {:stacked true}}}}]
    [:div
     (Chart. context (clj->js chart-data))]))

(defn test-a []
  (r/create-class
   {:component-did-mount test-chart
    ;; :compoent-did-update show-chart
    :display-name "chart-omponent"
    :reagent-render (fn []
                      [:div {:key "char"}
                       [:canvas {:id "chart1" :width 200 :height 50}]])}))

