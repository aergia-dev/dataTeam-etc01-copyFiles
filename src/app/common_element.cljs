(ns app.common-element
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch]]))

(defn split-input-box [id v]
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

(defn input-box [id v prop]
  (let [val (r/atom v)]
     (fn []
       (letfn [(change-val [new-v] (reset! val new-v) new-v)]
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
                         :on-change #(let [new-v (-> % .-target .-value)]
                                          (-> new-v change-val ((:after-on-change prop))))
                         :value (or @val "")}]]]))))


;; (defn default-input-ele [id v prop]
;;   (let [val (r/atom v)
;;         default-prop {:type "text"
;;                       :class "form-control block w-full px-3 py-1.5 text-base font-normal text-gray-700 bg-white bg-clip-padding border border-solid border-gray-300 rounded transition ease-in-out m-0 focus:text-gray-700 focus:bg-white focus:border-blue-600 focus:outline-none"
;;                       :id id
;;                       :on-change #(let [new-v (-> % .-target .-value)]
;;                                         ;; [k target] (clojure.string/split id #"-")]
;;                                     (reset! val new-v))
;;                                     ;; (dispatch [:modify-frame-num (js/parseInt k) (keyword target) (js/parseInt new-v)]))
;;                       :value (or @val "")}]
;;     (fn []
;;       [:div {:class "flex justify-center mt-3"
;;              :key v}
;;        [:div {:class "mb-3 w-32 ml-2 mr-2 "}
;;         [:input (merge default-prop prop)]]])))


(defn spinner [color show]
  (when (true? show)
    (let [c (if (seq color)
              color
              "#000000")]
      [:div {:style {:color c} ;;"#79bbb5"}
             :class "la-ball-spin-fade-rotating la-2x z-999"}
       [:div] [:div] [:div] [:div] [:div] [:div] [:div] [:div]])))
