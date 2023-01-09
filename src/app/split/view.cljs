(ns app.split.view
 (:require ;;[reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
            ;; ["@tauri-apps/api/dialog" :as dialog]
            ;; ["@tauri-apps/api/fs" :as fs]
            [app.common-element :refer [input-box]]
            ;; [app.toaster :as toaster]
   [taoensso.timbre :refer [debug]]))

(defn analyze []
  (let [file @(subscribe [:files])
        color (-> @(subscribe [:color]))]
   (when (seq file)
      [:div {:class "flex justify-center grow"}
       [:button {:class (str (:all color) "text-white rounded-full w-96 ")
                 :on-click #(dispatch [:split-analyze])}
              ;;  :on-click #(analyze-on-click (first file))}
        "analyze for split"]])))

(defn analyze-result []
  (let [data @(subscribe [:data])
        {:keys [frame-data total-box-cnt frame-cnt-has-box first-idx last-idx]}  (-> data first second)
        color1 "bg-green-600/30"
        color2 "bg-green-400"]
      ;; (prn "analyze-result"  (first data))
      ;; [:div "sdf"])))
    (prn "3333 "(-> data first second :total-box-cnt))
    (when (seq data)
      [:table {:class "w-96
                       table-auto text-white border-collapse"}
       [:thead
        [:tr {:class color1}
         [:th {:class color1} "first idx"]
         [:th {:class color1} "last idx"]
         [:th {:class color1} "total box cnt"]
         [:th {:class color1} "box cnt avg"]]]
       [:tbody
        [:tr
         [:th {:class color2} first-idx]
         [:th {:class color2} last-idx]
         [:th {:class color2} total-box-cnt]
         [:th {:class color2} frame-cnt-has-box]]]])))

(defn analyze-graph []
    (let [{:keys [frame-data total-box-cnt frame-cnt-has-box first-idx last-idx] :as data} (-> @(subscribe [:data])
                                                                                               )]
      (prn "####")))

(defn validation [])

(defn split-config []
;;   (let [data (-> @(subscribe [:data]))]
    ;; (when (seq data)
  [:div
   [:button {:class "text-pink-500 background-transparent font-bold uppercase px-3 py-1 text-xs outline-none focus:outline-none mr-1 mb-1 ease-linear transition-all duration-150"
             :type= "button"}
    [:i {:class "fas fa-heart"}]]])

(defn split-btn-view []
  (let [data (-> @(subscribe [:data]))]
    (prn "## " data)))

(defn show-result [])


(defn add-user-on-click [k] 
  (dispatch [:add-split-user k]))

(defn add-frame-on-click [k]
  (dispatch [:add-split-user-frame k]))

(defn minus-frame-on-click [k]
  (dispatch [:minus-split-user-frame k]))


(defn split-user-view []
  (let [users @(subscribe [:split-user])]
    (when (seq users)
      (prn "users " users)
      (prn "frame " (-> users first second :frame))
      [:table
       [:thead
        [:tr
         [:th "user name"]
         [:th ""]
         [:th "frame"]]]
       [:tbody
        (for [[k user] users
               :let [frames (-> user :frame)
                     idx (-> frames count dec)]]
            [:tr {:key (str "tr-user-name-" idx)}
             [:td [input-box (str "user-name-" idx) (:user-name user) {:after-on-change #(dispatch [:change-split-user-name k %])}]]
             [:td [:div {:class "flex-col"}
                   [:button {:class "text-green-500 background-transparent font-bold uppercase px-3 py-1 text-4xl outline-none focus:outline-none mr-1 mb-1 ease-linear transition-all duration-150"
                             :type "button"
                             :on-click #(add-frame-on-click k)}
                    [:i {:class "fas fa-plus-square"}]]
                   [:button {:class "text-green-500 background-transparent font-bold uppercase px-3 py-1 text-4xl outline-none focus:outline-none mr-1 mb-1 ease-linear transition-all duration-150"
                             :type "button"
                             :on-click #(minus-frame-on-click k)}
                    [:i {:class "fas fa-minus-square"}]]]]
             [:td
              [:table
                 [:tbody
                  (for [i (range (count frames))]
                    [:tr {:key (str "frame-tr" (:idx (get frames i)))}
                     [:td [input-box (str "frame-start-" (:idx (get frames i))) (:start (get frames i)) {:after-on-change #(dispatch [:change-frame-num k i :start %])}]]
                     [:td [input-box (str "frame-end-" (:idx (get frames i))) (:end (get frames i)) {:after-on-change #(dispatch [:change-frame-num k i :end %])}]]])]]]])]])))

(defn split-input-view []
  (let [data @(subscribe [:data])]
    (when (seq data)
      [:div
       [:div [split-user-view]]
       [:button {:class "text-pink-500 background-transparent font-bold uppercase px-3 py-1 text-8xl outline-none focus:outline-none mr-1 mb-1 ease-linear transition-all duration-150"
                 :type "button"
                 :on-click add-user-on-click}
        [:i {:class "fas fa-plus-square"}]]])))

(defn file-lst []
  (let [files @(subscribe [:files])
        base-file @(subscribe [:base-file])]
    (when (seq files)
      [:div  {:class "flex justify-center"}
       [:div
        (for [file files]
          [:div {:class "form-check"
                 :key (str file)}
            (str file)])]])))

(defn view-split []
  (dispatch [:mode :split])
  [:div {:class "mt-6 mb-6 justify-center"}
   [file-lst]
   [analyze]
   [analyze-result]
   [split-input-view]
  ;;  [split-input-view]
  ;;  [analyze-graph]
  ;;  [validation]
  ;;  [split-config]
  ;;  [split-btn-view]
  ;;  [show-result]
   ])
