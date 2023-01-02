(ns app.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [app.subs]
            [app.merge.view :refer [view-merge]]
            [app.split.view :refer [view-split]]
            [app.common-element :refer [spinner]]
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


(defn base-file-checkbox-cont [cur-filename] 
;; (debug (.stringify js/JSON checkbox nil 4))
;; (debug (.keys js/Object checkbox))
;; (debug (.keys js/Object (.-target checkbox)))
  (let [boxs (.getElementsByName js/document "basefile-check")
        filenames (.getElementsByName js/document "filenames")
        cnt (.-length boxs)]

    ;; (debug (.keys js/Object (aget filenames 0)))
    (debug  (.-lenth filenames))
    (debug "## " cnt)
    (doseq [i (range cnt)]
      (let [filename (aget filenames i)]
        (set! (.-checked (aget boxs i)) false)))))

(defn view-open-file []
  [:div {:class "flex v-screen flex-col space-y-4 justify-center"}
   [:div {:class "flex grow justify-center"}
    [:button {:class " bg-gray-500 hover:bg-gray-700 text-white font-bold rounded-full w-96"
              :on-click (fn [e]
                          (let [f (.open dialog (clj->js {:multiple true}))]
                            (dispatch [:clear-data nil])
                            (-> f
                                (.then (fn [f] (dispatch-sync [:files (js->clj f)])))
                                (.catch #(js/alert "file open error: " %)))))} "open"]]

   (let [files @(subscribe [:files])]
     (when (seq files)
   [:div {:class "flex justify-center"}
    [:div
     (for [file (second files)]
       [:div {:class "form-check"
              :key (str file)}
        [:input {:class "form-check-input appearance-none h-4 w-4 border border-gray-300 rounded-sm bg-white checked:bg-blue-600 checked:border-blue-600 focus:outline-none transition duration-200 mt-1 align-top bg-no-repeat bg-center bg-contain float-left mr-2 cursor-pointer"
                 :type "checkbox"
                 :value ""
                 :id (str file)
                 :name "basefile-check"
                 :on-change #(do (base-file-checkbox-cont file)
                                 (dispatch [:base-file file]))}]
        [:label {:class "form-check-label inline-block text-gray-800"
                 :name "filenames"
                 :for (str file)}
         (str file)]])]]))])

(defn analyze-select [file-cnt]
  (if (= 1 file-cnt)
    [view-split]
    [view-merge]))

(defn add-user-on-click [] 
  (dispatch [:add-split-user]))

(defn add-frame-on-click [k]
  (dispatch [:add-split-user-frame k]))

(defn split-user-view []
  (let [users @(subscribe [:split-user])]
    (when (seq users)
      (prn "users " users)
      [:table
       [:thead
        [:tr
         [:th "user name"]
         [:th ""]
         [:th "start"]
         [:th "end"]]]
       [:tbody
        (for [[k user] users
               :let [frames (-> user first :frame)
                 ;;multi-frame-cnt (count frames)
                     first-frame (first frames)
                     the-others (rest frames)]]
          (do
            (prn "user" k)
            (prn "user" user)
            [:tr {:key (gensym)}
             [:td (:user-name user)]
             [:td [:button {:class "text-green-500 background-transparent font-bold uppercase px-3 py-1 text-4xl outline-none focus:outline-none mr-1 mb-1 ease-linear transition-all duration-150"
                            :type "button"
                            :on-click #(add-frame-on-click k)}
                   [:i {:class "fas fa-plus-square"}]]]
             [:td 1234];;(:start first-frame)]
             [:td (:end first-frame)]]))]])))
          ;;  (for [frame the-others]
          ;;    [:tr {:key (gensym)}
          ;;     [:td "xxxxx"]
          ;;     [:td (:start frame)]
          ;;     [:td (:end frame)]])))]])))

(defn testing []
  [:div
  [:div [split-user-view]]
   [:button {:class "text-pink-500 background-transparent font-bold uppercase px-3 py-1 text-8xl outline-none focus:outline-none mr-1 mb-1 ease-linear transition-all duration-150"
             :type "button"
             :on-click add-user-on-click}
    [:i {:class "fas fa-plus-square"}]]])


(defn default-view []
  [:div
   [:div {:class "flex w-screen flex-col items-center justify-center z-20 fixed"}
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
    [testing]
    (let [files @(subscribe [:files])]
      (when (seq files)
        [:div {:class "flex grow justify-center"
               :id    "analyze-split"}
         (analyze-select (-> files second count))]))]
   [:div {:class "flex h-screen w-screen items-center justify-center fixed z-999"}
    (let [enable? @(subscribe [:busy])]
      (spinner "" enable?))]])
