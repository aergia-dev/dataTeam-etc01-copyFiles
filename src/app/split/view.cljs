(ns app.split.view
 (:require ;;[reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
            ;; ["@tauri-apps/api/dialog" :as dialog]
            ;; ["@tauri-apps/api/fs" :as fs]
            [app.common-element :refer [input-box]]
            ;; [app.toaster :as toaster]
   [taoensso.timbre :refer [debug]]))

(defn analyze []
  (let [file (-> @(subscribe [:files]) second)
        color (-> @(subscribe [:color]))]
    ;; (debug "file " file)
    ;; (debug (seq file))
    (when (seq file)
      [:div {:class "flex justify-center grow"}
       [:button {:class (str (:all color) "text-white rounded-full w-96 ")
                 :on-click #(dispatch [:split-analyze])}
              ;;  :on-click #(analyze-on-click (first file))}
        "analyze for split"]])))

(defn analyze-result []
  (let [{:keys [frame-data total-box-cnt frame-cnt-has-box first-idx last-idx] :as data} (-> @(subscribe [:data])
                                                                                             vals
                                                                                             first)
        color1 "bg-green-400/30"
        color2 "bg-green-200"]
    (when (seq data)
      [:table {:class "w-96
                       table-auto text-white border-collapse"}
       [:thead
        [:tr {:class color1}
         [:th {:class color1} "first idx"]
         [:th {:class color1} "last idx"]
         [:th {:class color1} "total box cnt"]
         [:th {:class color1} "box cnt avg"]]
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


(defn add-user-on-click [] 
  (dispatch [:add-split-user]))

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
                     idx (count frames)]]
            [:tr {:key (gensym)}
             [:td [input-box idx (:user-name user) {:on-change #()}]]
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
               (for [frame frames]
                 [:tr {:key (gensym)}
                  [:td [input-box (:idx frame) (:start frame) {:after-on-change #(dispatch [:change-frame-num :start idx (:idx frame) %])}]]
                  [:td [input-box (:idx frame) (:end frame) {:after-on-change #(dispatch [:change-frame-num :end idx  (:idx frame) %])}]]])]]]])]])))

(defn split-input-view []
  [:div
  [:div [split-user-view]]
   [:button {:class "text-pink-500 background-transparent font-bold uppercase px-3 py-1 text-8xl outline-none focus:outline-none mr-1 mb-1 ease-linear transition-all duration-150"
             :type "button"
             :on-click add-user-on-click}
    [:i {:class "fas fa-plus-square"}]]])

(defn file-lst []
  (let [files @(subscribe [:files])]
    (when (seq files)
      [:div  {:class "flex justify-center"}
       [:div
        (for [file (second files)]
          [:div {:class "form-check"}
           [:input {:class "form-check-input appearance-none h-4 w-4 border border-gray-300 rounded-sm bg-white checked:bg-blue-600 checked:border-blue-600 focus:outline-none transition duration-200 mt-1 align-top bg-no-repeat bg-center bg-contain float-left mr-2 cursor-pointer"
                    :type "checkbox"
                    :value ""
                    :id (str file)}
            [:label {:class "form-check-label inline-block text-gray-800"
                     :for (str file)}
             (str file)]]])]])))

;; (defn file-lst []
;;   [:div {:class "flex y-10"}]
;;   (let [files @(subscribe [:files])]
;;     (when (seq files)
;;       [:div {:class "flex flex-col"}
;;        [:span {:class "h-1 w-full bg-blue-200"}]
;;        [:div {:class "flex-col"}
;;         [:ul {:class "list-inside"}]
;;         (for [file (second files)]
;;           [:li {:key (str file)}
;;            (str file)])]
;;        [:span {:class "h-1 w-full bg-blue-200"}]])))

(defn view-split []
  (dispatch [:mode :split])
  [:div {:class "mt-6 mb-6 justify-center"}
   [:div
    [:i {:class "fa-solid fa-fan text-[10rem] text-green-500 animate-spin"}]]
   [analyze]
   [analyze-result]
   [split-input-view]
   [analyze-graph]
   [validation]
   [split-config]
   [split-btn-view]
   [show-result]])
