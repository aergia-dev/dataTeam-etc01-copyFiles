(ns app.views
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [app.subs]
            [app.common-element :refer [spinner]]
            [app.merge.view :refer [view-merge]]
            [app.split.view :refer [view-split]]
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
   [:button {:class "bg-gray-500 hover:bg-gray-700 text-white font-bold rounded-full w-20"
             :on-click (fn [_]
                          (let [f (.open dialog (clj->js {:multiple true}))]
                            (dispatch [:clear-data nil])
                            (-> f
                                (.then (fn [f] (dispatch-sync [:files (js->clj f)])))
                               (.catch #(js/alert "file open error: " %)))))}
    "open"]])



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
          [view-split]
          [view-merge])))]])
