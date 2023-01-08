(ns app.views
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [app.subs]
            [app.common-element :refer [spinner]]
            [app.merge.view :as merge-view]
            [app.split.view :as split-view]
            ["react-toastify" :refer [ToastContainer]]
            ["@tauri-apps/api/dialog" :as dialog]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
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


(defn analyze-select [file-cnt]
  (if (= 1 file-cnt)
    [view-split]
    [view-merge]))

(def imgs ["m_1.jpg"
           "m_2.jpg"
           "m_3.jpg"
           "m_4.jpg"
           "m_5.jpg"
           "m_6.jpg"
           "m_7.jpg"])

(defn get-img-path []
  (let [idx (-> (.random js/Math)
                (* 10)
                (mod (count imgs))
                (js/Math.floor))]
    (get imgs idx)))


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
                                 :pauseOnHover true})]]
   [:div {:class "flex h-screen w-screen items-center justify-center fixed z-999"}
    (let [enable? @(subscribe [:busy])]
      (spinner "" enable?))]

   [:div {:class "flex flex-col  w-screen items-center justify-center fixed"}
    [view-open-file]
    (let [files @(subscribe [:files])]
      (when (seq files)
        (debug "##" (count files))
        (if (= (count files) 1)
          [split-view/view-split]
          [merge-view/view-merge])))]])
